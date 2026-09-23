package com.powerpuff.backend.commerce;

import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private final JdbcTemplate db;
    public SessionService(JdbcTemplate db) {this.db=db;}
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=300000,initialDelay=300000)
    public void purgeExpired() {db.update("DELETE FROM customer_sessions WHERE expires_at<CURRENT_TIMESTAMP");}
    public Map<String,Object> create() {
        byte[] random=new byte[32]; new SecureRandom().nextBytes(random);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        var id=UUID.randomUUID(); var expiry=java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).plusHours(24);
        db.update("INSERT INTO customer_sessions(id,token_hash,expires_at) VALUES (?,?,?)",id,hash(token),expiry);
        return Map.of("token",token,"expiresAt",expiry,"tokenType","Bearer");
    }
    public UUID authenticate(String authorization) {
        if(authorization==null || !authorization.startsWith("Bearer ") || authorization.length()>100) throw unauthorized();
        var ids=db.queryForList("SELECT id FROM customer_sessions WHERE token_hash=? AND expires_at>CURRENT_TIMESTAMP",UUID.class,hash(authorization.substring(7)));
        if(ids.isEmpty()) throw unauthorized();
        return ids.getFirst();
    }
    public void lock(UUID session) {
        if(db.queryForList("SELECT id FROM customer_sessions WHERE id=? AND expires_at>CURRENT_TIMESTAMP FOR UPDATE",UUID.class,session).isEmpty()) throw unauthorized();
    }
    private BusinessException unauthorized() {return new BusinessException(HttpStatus.UNAUTHORIZED,"SESSION_REQUIRED","Create a new session");}
    private String hash(String token) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
}
