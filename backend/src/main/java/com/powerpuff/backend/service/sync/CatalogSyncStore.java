package com.powerpuff.backend.service.sync;

import com.powerpuff.backend.client.EktDtos;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Короткие транзакции записи; вызовов EKT внутри них нет. */
@Repository
public class CatalogSyncStore {
    private final JdbcTemplate db;
    public CatalogSyncStore(JdbcTemplate db) { this.db = db; }
    public record Run(long id, int nextPage, boolean listComplete) {}
    public Run load(long id) {
        return db.queryForObject("SELECT id,next_page,list_complete FROM catalog_sync_runs WHERE id=?",
                (r,n) -> new Run(r.getLong(1),r.getInt(2),r.getBoolean(3)), id);
    }
    @Transactional
    public Run start(boolean resume) {
        var previous = db.queryForList("SELECT id FROM catalog_sync_runs WHERE id=(SELECT MAX(id) FROM catalog_sync_runs) AND status <> 'COMPLETE'", Long.class);
        long id;
        if (resume && !previous.isEmpty()) id = previous.getFirst();
        else id = db.queryForObject("INSERT INTO catalog_sync_runs(status) VALUES ('RUNNING') RETURNING id", Long.class);
        state(id,"RUNNING",null);
        return load(id);
    }
    public boolean seen(long id, String hash) {
        return db.queryForObject("SELECT COUNT(*) FROM catalog_sync_pages WHERE run_id=? AND fingerprint=?",Long.class,id,hash) > 0;
    }
    @Transactional
    public void page(long run, int page, String hash, List<EktDtos.Product> items) {
        for (var p: items) {
            db.update("""
                INSERT INTO products(id,name,article,price,image,url,list_updated_at)
                VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)
                ON CONFLICT(id) DO UPDATE SET name=EXCLUDED.name,article=EXCLUDED.article,
                price=EXCLUDED.price,image=EXCLUDED.image,url=EXCLUDED.url,list_updated_at=CURRENT_TIMESTAMP
                """,p.id(),p.name(),p.article(),p.price(),p.image(),p.url());
            db.update("INSERT INTO catalog_sync_products(run_id,product_id) VALUES (?,?) ON CONFLICT DO NOTHING",run,p.id());
        }
        db.update("INSERT INTO catalog_sync_pages(run_id,page,fingerprint) VALUES (?,?,?)",run,page,hash);
        db.update("UPDATE catalog_sync_runs SET next_page=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",page+1,run);
    }
    public void listComplete(long id) {
        db.update("UPDATE catalog_sync_runs SET list_complete=TRUE,updated_at=CURRENT_TIMESTAMP WHERE id=?",id);
    }
    public List<Long> pending(long id, long after, int limit) {
        return db.queryForList("""
            SELECT product_id FROM catalog_sync_products
            WHERE run_id=? AND detail_status <> 'COMPLETE' AND product_id>? ORDER BY product_id LIMIT ?
            """,Long.class,id,after,limit);
    }
    @Transactional
    public void detail(long run, EktDtos.Product p) {
        db.update("""
            UPDATE products SET name=?,article=?,description=?,price=?,quantity=?,image=?,url=?,
            barcode=?,supplier_article=?,brand=?,properties=CAST(? AS JSONB),offers=CAST(? AS JSONB),
            detail_updated_at=CURRENT_TIMESTAMP,detail_error=NULL WHERE id=?
            """,p.name(),p.article(),p.description(),p.price(),p.quantity(),p.image(),p.url(),
            property(p.properties(),"CML2_BAR_CODE"),property(p.properties(),"ARTIKULPOSTAVSHCHIKA"),
            property(p.properties(),"TORGOVAYA_MARKA"),json(p.properties()),json(p.offers()),p.id());
        // NULL означает неизвестный набор складов, старые строки нельзя показывать как свежие.
        db.update("DELETE FROM product_stocks WHERE product_id=?",p.id());
        if (p.stores() != null) for (var s:p.stores()) {
            if (s == null || s.id() == null) throw new IllegalArgumentException("INVALID_STORE");
            db.update("INSERT INTO stores(id,name) VALUES (?,?) ON CONFLICT(id) DO UPDATE SET name=EXCLUDED.name",s.id(),s.name());
            db.update("""
                INSERT INTO product_stocks(product_id,store_id,quantity) VALUES (?,?,?)
                ON CONFLICT(product_id,store_id) DO UPDATE SET quantity=EXCLUDED.quantity
                """,p.id(),s.id(),s.quantity());
        }
        db.update("UPDATE catalog_sync_products SET detail_status='COMPLETE',error=NULL WHERE run_id=? AND product_id=?",run,p.id());
        touch(run);
    }
    private String property(JsonNode node,String key) {
        if (node == null || !node.path(key).isValueNode() || node.path(key).isNull()) return null;
        return node.path(key).asText();
    }
    private String json(JsonNode n) { return n == null ? null : n.toString(); }
    @Transactional
    public void failed(long run,long product,String code) {
        db.update("UPDATE catalog_sync_products SET detail_status='FAILED',error=? WHERE run_id=? AND product_id=?",code,run,product);
        db.update("UPDATE products SET detail_error=? WHERE id=?",code,product);
        touch(run);
    }
    private void touch(long id) { db.update("UPDATE catalog_sync_runs SET updated_at=CURRENT_TIMESTAMP WHERE id=?",id); }
    public void state(long id,String status,String reason) {
        db.update("UPDATE catalog_sync_runs SET status=?,reason=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",status,reason,id);
    }
    public long remaining(long id) {
        return db.queryForObject("SELECT COUNT(*) FROM catalog_sync_products WHERE run_id=? AND detail_status <> 'COMPLETE'",Long.class,id);
    }
    public Map<String,Object> status() {
        var result = new LinkedHashMap<String,Object>();
        result.put("totalProducts",db.queryForObject("SELECT COUNT(*) FROM products",Long.class));
        result.put("productsWithDetails",db.queryForObject("SELECT COUNT(*) FROM products WHERE detail_updated_at IS NOT NULL",Long.class));
        var runs = db.queryForList("SELECT * FROM catalog_sync_runs ORDER BY id DESC LIMIT 1");
        if (runs.isEmpty()) { result.put("run",null); return result; }
        var run = new LinkedHashMap<>(runs.getFirst());
        long id = ((Number)run.get("id")).longValue();
        run.put("products",db.queryForObject("SELECT COUNT(*) FROM catalog_sync_products WHERE run_id=?",Long.class,id));
        run.put("detailsComplete",db.queryForObject("SELECT COUNT(*) FROM catalog_sync_products WHERE run_id=? AND detail_status='COMPLETE'",Long.class,id));
        run.put("errors",db.queryForObject("SELECT COUNT(*) FROM catalog_sync_products WHERE run_id=? AND detail_status='FAILED'",Long.class,id));
        result.put("run",run);
        return result;
    }
}
