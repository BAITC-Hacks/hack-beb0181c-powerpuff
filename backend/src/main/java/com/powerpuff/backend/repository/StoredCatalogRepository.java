package com.powerpuff.backend.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerpuff.backend.dto.*;
import com.powerpuff.backend.exception.NotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Repository
public class StoredCatalogRepository {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public StoredCatalogRepository(JdbcTemplate db,ObjectMapper json) { this.db=db;this.json=json; }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public StoredProductResponse get(long id) {
        var stocks=db.query("SELECT s.id,s.name,p.quantity FROM product_stocks p JOIN stores s ON s.id=p.store_id WHERE p.product_id=? ORDER BY s.id",
                (r,n)->new CatalogProductResponse.Stock(r.getLong(1),r.getString(2),r.getBigDecimal(3)),id);
        var result=db.query("SELECT * FROM products WHERE id=?",(r,n)->{
            var properties=read(r.getString("properties"));
            var product=new CatalogProductResponse(r.getLong("id"),r.getString("name"),r.getString("article"),
                    r.getString("description"),r.getBigDecimal("price"),r.getBigDecimal("quantity"),
                    r.getTimestamp("detail_updated_at")==null?null:stocks,r.getString("image"),r.getString("url"),read(r.getString("offers")),properties);
            var warnings=new ArrayList<String>();
            if(r.getTimestamp("detail_updated_at")==null) warnings.add("DETAILS_NOT_LOADED");
            if(r.getString("detail_error")!=null) warnings.add("DETAIL_REFRESH_FAILED");
            product.characteristicConflicts().forEach(c -> warnings.add(c.code()));
            return new StoredProductResponse(product,
                    r.getTimestamp("list_updated_at")==null?null:r.getTimestamp("list_updated_at").toInstant(),
                    r.getTimestamp("detail_updated_at")==null?null:r.getTimestamp("detail_updated_at").toInstant(),r.getString("detail_error"),warnings);
        },id);
        if(result.isEmpty()) throw new NotFoundException("Product not found");
        return result.getFirst();
    }
    private JsonNode read(String value) throws SQLException {
        if(value==null) return null;
        try{return json.readTree(value);}catch(Exception e){throw new SQLException("Invalid stored JSON");}
    }
}
