package com.powerpuff.backend.repository;

import com.powerpuff.backend.dto.ProductSearchResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogSearchRepository {
    private final NamedParameterJdbcTemplate db;
    public CatalogSearchRepository(NamedParameterJdbcTemplate db) { this.db = db; }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ProductSearchResponse search(String query, int page, int size) {
        var params = new MapSqlParameterSource().addValue("query",query)
                .addValue("phrase","%"+escape(query)+"%")
                .addValue("limit",size).addValue("offset",(long)page*size);
        var clauses = new ArrayList<String>();
        String[] tokens=query.isEmpty()?new String[0]:query.split("\\s+");
        for(int i=0;i<tokens.length;i++) {
            String key="token"+i;
            params.addValue(key,"%"+escape(tokens[i])+"%");
            clauses.add("(lower(article) LIKE lower(:"+key+") ESCAPE '!' OR "
                    +"lower(supplier_article) LIKE lower(:"+key+") ESCAPE '!' OR "
                    +"lower(name) LIKE lower(:"+key+") ESCAPE '!' OR "
                    +"lower(brand) LIKE lower(:"+key+") ESCAPE '!')");
        }
        String where=clauses.isEmpty()?"TRUE":String.join(" AND ",clauses);
        long total=db.queryForObject("SELECT count(*) FROM products WHERE "+where,params,Long.class);
        var items=db.query("SELECT * FROM products WHERE "+where+" "+"""
            ORDER BY CASE
                WHEN lower(article)=lower(:query) THEN 0
                WHEN lower(supplier_article)=lower(:query) THEN 1
                WHEN lower(name) LIKE lower(:phrase) ESCAPE '!' THEN 2
                ELSE 3 END, lower(name), id
            LIMIT :limit OFFSET :offset
            """,params,(r,n)->new ProductSearchResponse.Item(r.getLong("id"),r.getString("name"),
                r.getString("article"),r.getString("supplier_article"),r.getString("brand"),
                r.getBigDecimal("price"),r.getBigDecimal("quantity"),r.getString("image"),r.getString("url"),
                instant(r,"list_updated_at"),instant(r,"detail_updated_at"),r.getString("detail_error")));
        return new ProductSearchResponse(query,page,size,total,(total+size-1)/size,items);
    }
    private String escape(String value) { return value.replace("!","!!").replace("%","!%").replace("_","!_"); }
    private Instant instant(ResultSet r,String column) throws SQLException {
        var value=r.getTimestamp(column);
        return value==null?null:value.toInstant();
    }
}
