package com.powerpuff.backend.service.sync;

import com.powerpuff.backend.client.EktClient;
import com.powerpuff.backend.exception.EktException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.HexFormat;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class CatalogSynchronizer {
    private final DataSource dataSource;
    private final EktClient client;
    private final CatalogSyncStore store;
    public CatalogSynchronizer(DataSource dataSource,EktClient client,CatalogSyncStore store) {
        this.dataSource=dataSource; this.client=client; this.store=store;
    }
    /** Лимиты на один запуск; resume продолжает курсор и не повторяет успешные детали. */
    public long sync(boolean resume,int maxPages,int maxDetails,long delayMs) throws Exception {
        if(maxPages<0 || maxDetails<0 || (maxPages==0 && maxDetails==0) || delayMs<0) throw new IllegalArgumentException("Invalid sync limits");
        // Отдельное соединение удерживает session advisory lock без открытой транзакции.
        try(Connection lock=dataSource.getConnection()) {
            lock.setAutoCommit(true);
            try(var stmt=lock.createStatement();var rs=stmt.executeQuery("SELECT pg_try_advisory_lock(5152912026)")) {
                rs.next(); if(!rs.getBoolean(1)) throw new IllegalStateException("CATALOG_SYNC_ALREADY_RUNNING");
            }
            try { return run(resume,maxPages,maxDetails,delayMs); }
            finally { try(var stmt=lock.createStatement()) { stmt.execute("SELECT pg_advisory_unlock(5152912026)"); } }
        }
    }
    private long run(boolean resume,int maxPages,int maxDetails,long delay) throws Exception {
        var run=store.start(resume);
        long id=run.id();
        try {
            if(!run.listComplete()) {
                for(int n=0;n<maxPages;n++) {
                    int page=store.load(id).nextPage();
                    var response=client.getProducts(page);
                    if(response.items().isEmpty()) { store.listComplete(id); break; }
                    String ids=response.items().stream().map(p->p.id().toString()).sorted().reduce("",(a,b)->a+","+b);
                    String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ids.getBytes(StandardCharsets.UTF_8)));
                    if(store.seen(id,hash)) { store.state(id,"STOPPED","REPEATED_PAGE"); return id; }
                    store.page(id,page,hash,response.items());
                    Thread.sleep(delay);
                }
            }
            int attempted=0;
            long after=0;
            while(attempted<maxDetails) {
                var batch=store.pending(id,after,Math.min(100,maxDetails-attempted));
                if(batch.isEmpty()) break;
                for(long product:batch) {
                    try { store.detail(id,client.getProduct(product)); }
                    catch(EktException ex) {
                        store.failed(id,product,ex.getCode());
                        // Глобальные сбои не размножаем на весь каталог.
                        if(!ex.getCode().equals("PRODUCT_NOT_FOUND") && !ex.getCode().equals("EKT_INVALID_RESPONSE")) throw ex;
                    } catch(IllegalArgumentException ex) { store.failed(id,product,"INVALID_DETAIL_DATA"); }
                    after=product; attempted++;
                    Thread.sleep(delay);
                }
            }
            boolean complete=store.load(id).listComplete() && store.remaining(id)==0;
            store.state(id,complete?"COMPLETE":"STOPPED",complete?null:
                    (!store.load(id).listComplete()?"PAGE_LIMIT":"DETAILS_PENDING"));
            return id;
        } catch(InterruptedException ex) {
            store.state(id,"STOPPED","INTERRUPTED"); Thread.currentThread().interrupt(); throw ex;
        } catch(Exception ex) {
            store.state(id,"FAILED",ex instanceof EktException e?e.getCode():"SYNC_FAILED");
            // Внешние сообщения/SQL с параметрами в консоль не передаём.
            throw new IllegalStateException("Catalog sync failed; inspect /api/catalog/status");
        }
    }
}
