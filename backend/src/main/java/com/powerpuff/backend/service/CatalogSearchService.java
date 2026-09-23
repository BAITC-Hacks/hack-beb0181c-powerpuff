package com.powerpuff.backend.service;

import com.powerpuff.backend.dto.ProductSearchResponse;
import com.powerpuff.backend.repository.CatalogSearchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogSearchService {
    private final CatalogSearchRepository repository;
    public CatalogSearchService(CatalogSearchRepository repository) { this.repository=repository; }
    public ProductSearchResponse search(String query,int page,int size) {
        String normalized=query==null?"":query.strip().replaceAll("\\s+"," ");
        if(normalized.isBlank() || query.length()>200 || normalized.split(" ").length>10
                || page<0 || page>1_000_000 || size<1 || size>100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid search parameters");
        return repository.search(normalized,page,size);
    }
}
