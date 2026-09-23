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
    private final JdbcTemplate db; private final EktClient ekt; private final SessionService sessions;
    private final TransactionTemplate tx; private final String cartUrl;
    public CartService(JdbcTemplate db,EktClient ekt,SessionService sessions,TransactionTemplate tx,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontend) {
        this.db=db;this.ekt=ekt;this.sessions=sessions;this.tx=tx;cartUrl=frontend.replaceAll("/+$","")+"/cart";
    }
    public record Proposal(UUID id,long productId,String name,BigDecimal quantity,BigDecimal price,
            BigDecimal stock,int revision,String status,OffsetDateTime expiresAt) {}
    public record Confirmation(boolean added,boolean requiresConfirmation,Proposal proposal,Map<String,Object> cart) {}
    private Proposal row(java.sql.ResultSet r) throws java.sql.SQLException {
        return new Proposal(r.getObject("id",UUID.class),r.getLong("product_id"),r.getString("name"),
            r.getBigDecimal("quantity"),r.getBigDecimal("price"),r.getBigDecimal("stock"),r.getInt("revision"),
            r.getString("status"),r.getObject("expires_at",OffsetDateTime.class));
    }
    public Map<String,Object> cart(UUID session) {
        var items=db.queryForList("SELECT product_id AS \"productId\",name,quantity,price,quantity*price AS subtotal FROM cart_items WHERE session_id=? ORDER BY product_id",session);
        return Map.of("items",items,"cartUrl",cartUrl,"mode","LOCAL_DEMO","stockReserved",false);
    }
    public Proposal create(UUID session,UUID requestId,long product,BigDecimal quantity) {
        validate(quantity);
        var previous=db.query("SELECT * FROM cart_proposals WHERE session_id=? AND request_id=?",(r,n)->row(r),session,requestId);
        if(!previous.isEmpty()) return sameRequest(previous.getFirst(),product,quantity);
        var current=ekt.getProduct(product); // Сеть до транзакции.
        return tx.execute(status->{
            sessions.lock(session);
            var duplicate=db.query("SELECT * FROM cart_proposals WHERE session_id=? AND request_id=?",(r,n)->row(r),session,requestId);
            if(!duplicate.isEmpty()) return sameRequest(duplicate.getFirst(),product,quantity);
            available(session,current,quantity);
            db.update("UPDATE cart_proposals SET status='SUPERSEDED' WHERE session_id=? AND status='PENDING'",session);
            UUID id=UUID.randomUUID();
            db.update("INSERT INTO cart_proposals(id,session_id,request_id,product_id,name,quantity,price,stock,expires_at) VALUES (?,?,?,?,?,?,?,?,?)",
                id,session,requestId,product,current.name(),quantity,current.price(),current.quantity(),OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10));
            return get(session,id);
        });
    }
    private Proposal sameRequest(Proposal p,long product,BigDecimal quantity) {
        if(p.productId()!=product || p.quantity().compareTo(quantity)!=0) throw conflict("REQUEST_ID_REUSED","Request ID already used with another item");
        return p;
    }
    public Proposal pending(UUID session) {
        var rows=db.query("SELECT * FROM cart_proposals WHERE session_id=? AND status='PENDING' AND expires_at>CURRENT_TIMESTAMP",(r,n)->row(r),session);
        if(rows.size()!=1) throw conflict("NO_PENDING_PROPOSAL","No unambiguous pending proposal");
        return rows.getFirst();
    }
    public Proposal get(UUID session,UUID id) {
        var rows=db.query("SELECT * FROM cart_proposals WHERE session_id=? AND id=?",(r,n)->row(r),session,id);
        if(rows.isEmpty()) throw new BusinessException(HttpStatus.NOT_FOUND,"PROPOSAL_NOT_FOUND","Proposal not found");
        return rows.getFirst();
    }
    public Confirmation confirm(UUID session,UUID id,int revision) {
        var proposal=get(session,id);
        if(proposal.status().equals("CONFIRMED")) return new Confirmation(true,false,proposal,cart(session));
        check(proposal,revision);
        var fresh=ekt.getProduct(proposal.productId());
        return tx.execute(status->{
            sessions.lock(session);
            var locked=get(session,id);
            if(locked.status().equals("CONFIRMED")) return new Confirmation(true,false,locked,cart(session));
            check(locked,revision);
            available(session,fresh,locked.quantity());
            if(fresh.price().compareTo(locked.price())!=0 || fresh.quantity().compareTo(locked.stock())!=0) {
                db.update("UPDATE cart_proposals SET price=?,stock=?,revision=revision+1,expires_at=? WHERE id=?",
                    fresh.price(),fresh.quantity(),OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10),id);
                return new Confirmation(false,true,get(session,id),cart(session));
            }
            db.update("""
                INSERT INTO cart_items(session_id,product_id,name,quantity,price) VALUES (?,?,?,?,?)
                ON CONFLICT(session_id,product_id) DO UPDATE SET quantity=cart_items.quantity+EXCLUDED.quantity,
                    price=EXCLUDED.price,name=EXCLUDED.name
                """,session,locked.productId(),fresh.name(),locked.quantity(),fresh.price());
            db.update("UPDATE cart_proposals SET status='CONFIRMED' WHERE id=?",id);
            return new Confirmation(true,false,get(session,id),cart(session));
        });
    }
    private void check(Proposal p,int revision) {
        if(!p.status().equals("PENDING") || p.expiresAt().isBefore(OffsetDateTime.now())) throw conflict("PROPOSAL_EXPIRED","Create a new proposal");
        if(p.revision()!=revision) throw conflict("PROPOSAL_CHANGED","Confirm the current proposal revision");
    }
    private void available(UUID session,EktDtos.Product p,BigDecimal quantity) {
        if(p.price()==null || p.price().signum()<0 || p.quantity()==null) throw conflict("PURCHASE_DATA_UNKNOWN","Price or stock is not available");
        BigDecimal existing=db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM cart_items WHERE session_id=? AND product_id=?",BigDecimal.class,session,p.id());
        if(quantity.add(existing).compareTo(p.quantity())>0) throw conflict("INSUFFICIENT_STOCK","Requested quantity exceeds available stock including the cart");
        if(p.properties()!=null && p.properties().hasNonNull("KRATNOST_MIN")) {
            try {
                var multiple=new BigDecimal(p.properties().get("KRATNOST_MIN").asText().replace(',','.'));
                if(multiple.signum()<=0 || quantity.remainder(multiple).signum()!=0) throw conflict("INVALID_MULTIPLE","Check the product order multiple");
            }catch(NumberFormatException ex){throw conflict("UNKNOWN_MULTIPLE","Order multiple cannot be verified");}
        }
    }
    private void validate(BigDecimal q) {
        if(q==null || q.signum()<=0 || q.scale()>10 || q.precision()>28) throw new BusinessException(HttpStatus.BAD_REQUEST,"INVALID_QUANTITY","Invalid quantity");
    }
    private BusinessException conflict(String code,String text){return new BusinessException(HttpStatus.CONFLICT,code,text);}
}
