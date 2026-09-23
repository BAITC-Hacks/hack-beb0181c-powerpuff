package com.powerpuff.backend.controller;

import com.powerpuff.backend.dto.StoredProductResponse;
import com.powerpuff.backend.repository.StoredCatalogRepository;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/products")
public class StoredCatalogController {
    private final StoredCatalogRepository repository;
    private final com.powerpuff.backend.repository.CatalogSearchRepository search;
    public StoredCatalogController(StoredCatalogRepository repository,com.powerpuff.backend.repository.CatalogSearchRepository search) {this.repository=repository;this.search=search;}
    @GetMapping
    public com.powerpuff.backend.dto.ProductSearchResponse list(
        @RequestParam(defaultValue="0") @Min(0) @jakarta.validation.constraints.Max(1000000) int page,
        @RequestParam(defaultValue="20") @Min(1) @jakarta.validation.constraints.Max(100) int size) {
        return search.search("",page,size);
    }
    @GetMapping("/{id}")
    public StoredProductResponse get(@PathVariable @Min(1) long id) {return repository.get(id);}
}
