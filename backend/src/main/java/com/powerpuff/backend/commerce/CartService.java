package com.powerpuff.backend.commerce;

import com.powerpuff.backend.client.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CartService {
    private final JdbcTemplate db;
    private final EktClient ekt;
    private final SessionService sessions;
    private final TransactionTemplate tx;
    private final String cartUrl;

    public CartService(JdbcTemplate db, EktClient ekt, SessionService sessions, TransactionTemplate tx,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontend) {
        this.db=db; this.ekt=ekt; this.sessions=sessions; this.tx=tx;
        cartUrl=frontend.replaceAll("/+$", "")+"/cart";
    }
    public record Proposal(UUID id, long productId, String name, BigDecimal quantity, BigDecimal price,
            BigDecimal stock, int revision, String status, OffsetDateTime expiresAt, String operation, long cartVersion) {}
    public record Confirmation(boolean added, boolean requiresConfirmation, Proposal proposal, Map<String,Object> cart) {}
    public record UpdateResult(boolean updated, boolean requiresConfirmation, Proposal proposal, Map<String,Object> cart) {}
    private record Creation(Proposal proposal, long version) {}
    private record Line(BigDecimal quantity, BigDecimal price, String multiple) {}
    private record Mutation(String operation, long product, BigDecimal quantity, long version, UUID proposal) {}

    private Proposal row(java.sql.ResultSet r) throws java.sql.SQLException {
        return new Proposal(r.getObject("id",UUID.class),r.getLong("product_id"),r.getString("name"),
            r.getBigDecimal("quantity"),r.getBigDecimal("price"),r.getBigDecimal("stock"),r.getInt("revision"),
            r.getString("status"),r.getObject("expires_at",OffsetDateTime.class),r.getString("operation"),r.getLong("cart_version"));
    }
    public Map<String,Object> cart(UUID session) {
        return tx.execute(status -> { sessions.lock(session); return snapshot(session); });
    }
    // All snapshots and mutations share the session lock, so version/items/total describe one state.
    private Map<String,Object> snapshot(UUID session) {
        var items=db.queryForList("""
            SELECT c.product_id AS "productId",c.name,c.quantity,c.price,c.quantity*c.price AS subtotal,
                COALESCE(c.article,p.article) AS article,COALESCE(c.image,p.image) AS image,
                COALESCE(c.order_multiple,p.properties->>'KRATNOST_MIN') AS multiple
            FROM cart_items c LEFT JOIN products p ON p.id=c.product_id WHERE c.session_id=? ORDER BY c.product_id
            """,session);
        BigDecimal total=BigDecimal.ZERO;
        for(var item:items) {
            item.put("orderMultiple", parseMultiple((String)item.remove("multiple")));
            total=total.add((BigDecimal)item.get("subtotal"));
        }
        return Map.of("items",items,"total",total,"version",version(session),"cartUrl",cartUrl,
                "mode","LOCAL_DEMO","stockReserved",false,"availabilityConfirmed",false);
    }
    private long version(UUID session) {
        return db.queryForObject("SELECT cart_version FROM customer_sessions WHERE id=?",Long.class,session);
    }
    private void versionMatches(UUID session,long expected) {
        if(version(session)!=expected) throw conflict("CART_CHANGED","Cart changed; reload it before confirming");
    }
    private void advance(UUID session) {
        db.update("UPDATE customer_sessions SET cart_version=cart_version+1 WHERE id=?",session);
        supersede(session);
    }
    private void supersede(UUID session) {
        db.update("UPDATE cart_proposals SET status='SUPERSEDED' WHERE session_id=? AND status='PENDING'",session);
    }
    private Line line(UUID session,long product) {
        var rows=db.query("""
            SELECT c.quantity,c.price,COALESCE(c.order_multiple,p.properties->>'KRATNOST_MIN') AS multiple
            FROM cart_items c LEFT JOIN products p ON p.id=c.product_id WHERE c.session_id=? AND c.product_id=?
            """,(r,n)->new Line(r.getBigDecimal("quantity"),r.getBigDecimal("price"),r.getString("multiple")),session,product);
        if(rows.isEmpty()) throw new BusinessException(HttpStatus.NOT_FOUND,"CART_ITEM_NOT_FOUND","Item is not in this session's cart");
        return rows.getFirst();
    }
    public Proposal create(UUID session,UUID requestId,long product,BigDecimal quantity) {
        validate(quantity);
        var previous=tx.execute(status -> {
            sessions.lock(session);
            var p=byRequest(session,requestId);
            return new Creation(p==null?null:sameRequest(p,product,quantity),version(session));
        });
        if(previous.proposal()!=null) return previous.proposal();
        var current=ekt.getProduct(product); // Never hold a transaction across network I/O.
        return tx.execute(status -> {
            sessions.lock(session);
            var duplicate=byRequest(session,requestId);
            if(duplicate!=null) return sameRequest(duplicate,product,quantity);
            versionMatches(session,previous.version());
            available(current,quantity.add(existing(session,product)),quantity);
            return proposal(session,requestId,current,quantity,"ADD");
        });
    }
    private Proposal byRequest(UUID session,UUID requestId) {
        var rows=db.query("SELECT * FROM cart_proposals WHERE session_id=? AND request_id=?",(r,n)->row(r),session,requestId);
        return rows.isEmpty()?null:rows.getFirst();
    }
    private Proposal sameRequest(Proposal p,long product,BigDecimal quantity) {
        if(!p.operation().equals("ADD") || p.productId()!=product || p.quantity().compareTo(quantity)!=0)
            throw conflict("REQUEST_ID_REUSED","Request ID already used with different parameters");
        return p;
    }
    private Proposal proposal(UUID session,UUID requestId,EktDtos.Product p,BigDecimal q,String operation) {
        supersede(session);
        UUID id=UUID.randomUUID();
        db.update("""
            INSERT INTO cart_proposals(id,session_id,request_id,product_id,name,quantity,price,stock,expires_at,operation,cart_version)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """,id,session,requestId,p.id(),p.name(),q,p.price(),p.quantity(),
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10),operation,version(session));
        return get(session,id);
    }
    public Proposal pending(UUID session) {
        var rows=db.query("SELECT * FROM cart_proposals WHERE session_id=? AND status='PENDING' AND operation='ADD' AND expires_at>CURRENT_TIMESTAMP",(r,n)->row(r),session);
        if(rows.size()!=1) throw conflict("NO_PENDING_PROPOSAL","No unambiguous pending proposal");
        return rows.getFirst();
    }
    public Proposal get(UUID session,UUID id) {
        var rows=db.query("SELECT * FROM cart_proposals WHERE session_id=? AND id=?",(r,n)->row(r),session,id);
        if(rows.isEmpty()) throw new BusinessException(HttpStatus.NOT_FOUND,"PROPOSAL_NOT_FOUND","Proposal not found");
        return rows.getFirst();
    }
    public Confirmation confirm(UUID session,UUID id,int revision) {
        var previous=tx.execute(status -> {
            sessions.lock(session);
            var p=get(session,id);
            if(p.status().equals("CONFIRMED")) return new Confirmation(true,false,p,snapshot(session));
            check(session,p,revision);
            return null;
        });
        if(previous!=null) return previous;
        var fresh=ekt.getProduct(get(session,id).productId());
        return tx.execute(status -> {
            sessions.lock(session);
            var locked=get(session,id);
            if(locked.status().equals("CONFIRMED")) return new Confirmation(true,false,locked,snapshot(session));
            check(session,locked,revision);
            boolean set=locked.operation().equals("SET");
            if(set) line(session,locked.productId()); // SET must never recreate a missing item.
            var target=set?locked.quantity():existing(session,locked.productId()).add(locked.quantity());
            available(fresh,target,locked.quantity());
            if(fresh.price().compareTo(locked.price())!=0 || fresh.quantity().compareTo(locked.stock())!=0) {
                db.update("UPDATE cart_proposals SET price=?,stock=?,revision=revision+1,expires_at=? WHERE id=?",
                    fresh.price(),fresh.quantity(),OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10),id);
                return new Confirmation(false,true,get(session,id),snapshot(session));
            }
            saveLine(session,fresh,target);
            db.update("UPDATE cart_proposals SET status='CONFIRMED' WHERE id=?",id);
            advance(session);
            return new Confirmation(true,false,get(session,id),snapshot(session));
        });
    }
    public UpdateResult setQuantity(UUID session,UUID requestId,long product,BigDecimal quantity,long expectedVersion) {
        validate(quantity);
        var immediate=tx.execute(status -> {
            sessions.lock(session);
            var duplicate=replay(session,requestId,"SET",product,quantity,expectedVersion);
            if(duplicate!=null) return duplicate;
            versionMatches(session,expectedVersion);
            var current=line(session,product);
            if(quantity.compareTo(current.quantity())>0) return null;
            validateMultiple(current.multiple(),quantity);
            db.update("UPDATE cart_items SET quantity=? WHERE session_id=? AND product_id=?",quantity,session,product);
            advance(session);
            remember(session,requestId,"SET",product,quantity,expectedVersion,null);
            return new UpdateResult(true,false,null,snapshot(session));
        });
        if(immediate!=null) return immediate;
        var fresh=ekt.getProduct(product);
        return tx.execute(status -> {
            sessions.lock(session);
            var duplicate=replay(session,requestId,"SET",product,quantity,expectedVersion);
            if(duplicate!=null) return duplicate;
            versionMatches(session,expectedVersion);
            var current=line(session,product);
            available(fresh,quantity,quantity);
            if(fresh.price().compareTo(current.price())!=0) {
                var p=proposal(session,UUID.randomUUID(),fresh,quantity,"SET");
                remember(session,requestId,"SET",product,quantity,expectedVersion,p.id());
                return new UpdateResult(false,true,p,snapshot(session));
            }
            saveLine(session,fresh,quantity);
            advance(session);
            remember(session,requestId,"SET",product,quantity,expectedVersion,null);
            return new UpdateResult(true,false,null,snapshot(session));
        });
    }
    public Map<String,Object> delete(UUID session,UUID requestId,long product,long expectedVersion) {
        return tx.execute(status -> {
            sessions.lock(session);
            var duplicate=replay(session,requestId,"DELETE",product,null,expectedVersion);
            if(duplicate!=null) return duplicate.cart();
            versionMatches(session,expectedVersion);
            line(session,product);
            db.update("DELETE FROM cart_items WHERE session_id=? AND product_id=?",session,product);
            advance(session);
            remember(session,requestId,"DELETE",product,null,expectedVersion,null);
            return snapshot(session);
        });
    }
    private UpdateResult replay(UUID session,UUID request,String operation,long product,BigDecimal quantity,long expected) {
        var rows=db.query("SELECT * FROM cart_mutations WHERE session_id=? AND request_id=?",
                (r,n)->new Mutation(r.getString("operation"),r.getLong("product_id"),r.getBigDecimal("quantity"),r.getLong("cart_version"),r.getObject("proposal_id",UUID.class)),session,request);
        if(rows.isEmpty()) return null;
        var m=rows.getFirst();
        if(!m.operation().equals(operation) || m.product()!=product || m.version()!=expected ||
                (m.quantity()==null?quantity!=null:quantity==null || m.quantity().compareTo(quantity)!=0))
            throw conflict("REQUEST_ID_REUSED","Request ID already used with different parameters");
        if(m.proposal()==null) return new UpdateResult(true,false,null,snapshot(session));
        var p=get(session,m.proposal());
        if(p.status().equals("CONFIRMED")) return new UpdateResult(true,false,p,snapshot(session));
        check(session,p,p.revision());
        return new UpdateResult(false,true,p,snapshot(session));
    }
    private void remember(UUID session,UUID request,String operation,long product,BigDecimal quantity,long expected,UUID proposal) {
        db.update("INSERT INTO cart_mutations(session_id,request_id,operation,product_id,quantity,cart_version,proposal_id) VALUES (?,?,?,?,?,?,?)",
                session,request,operation,product,quantity,expected,proposal);
    }
    private void saveLine(UUID session,EktDtos.Product p,BigDecimal target) {
        db.update("""
            INSERT INTO cart_items(session_id,product_id,name,quantity,price,article,image,order_multiple) VALUES (?,?,?,?,?,?,?,?)
            ON CONFLICT(session_id,product_id) DO UPDATE SET quantity=EXCLUDED.quantity,price=EXCLUDED.price,
                name=EXCLUDED.name,article=EXCLUDED.article,image=EXCLUDED.image,order_multiple=EXCLUDED.order_multiple
            """,session,p.id(),p.name(),target,p.price(),p.article(),p.image(),multiple(p));
    }
    private BigDecimal existing(UUID session,long product) {
        return db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM cart_items WHERE session_id=? AND product_id=?",BigDecimal.class,session,product);
    }
    private void check(UUID session,Proposal p,int revision) {
        if(p.status().equals("SUPERSEDED") || version(session)!=p.cartVersion())
            throw conflict("PROPOSAL_STALE","Cart or proposal changed; create a new proposal");
        if(!p.status().equals("PENDING") || p.expiresAt().isBefore(OffsetDateTime.now()))
            throw conflict("PROPOSAL_EXPIRED","Create a new proposal");
        if(p.revision()!=revision) throw conflict("PROPOSAL_CHANGED","Confirm the current proposal revision");
    }
    private void available(EktDtos.Product p,BigDecimal target,BigDecimal quantity) {
        if(p.price()==null || p.price().signum()<0 || p.quantity()==null)
            throw conflict("PURCHASE_DATA_UNKNOWN","Price or stock is not available");
        if(target.compareTo(p.quantity())>0) throw conflict("INSUFFICIENT_STOCK","Requested quantity exceeds available stock");
        validateMultiple(multiple(p),quantity);
    }
    private String multiple(EktDtos.Product p) {
        return p.properties()!=null && p.properties().hasNonNull("KRATNOST_MIN")?p.properties().get("KRATNOST_MIN").asText():null;
    }
    private BigDecimal parseMultiple(String raw) {
        if(raw==null) return null;
        try { var value=new BigDecimal(raw.strip().replace(',','.')); return value.signum()>0?value:null; }
        catch(NumberFormatException ex) { return null; }
    }
    private void validateMultiple(String raw,BigDecimal quantity) {
        if(raw==null) return;
        var multiple=parseMultiple(raw);
        if(multiple==null) throw conflict("UNKNOWN_MULTIPLE","Order multiple cannot be verified");
        if(quantity.remainder(multiple).signum()!=0) throw conflict("INVALID_MULTIPLE","Check the product order multiple");
    }
    private void validate(BigDecimal q) {
        if(q==null || q.signum()<=0 || q.scale()>10 || q.precision()>28)
            throw new BusinessException(HttpStatus.BAD_REQUEST,"INVALID_QUANTITY","Quantity must be positive; use delete to remove an item");
    }
    private BusinessException conflict(String code,String text) { return new BusinessException(HttpStatus.CONFLICT,code,text); }
}
