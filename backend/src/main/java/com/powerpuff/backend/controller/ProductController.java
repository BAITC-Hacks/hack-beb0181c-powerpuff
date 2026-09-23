package com.powerpuff.backend.controller;

import com.powerpuff.backend.dto.*;
import com.powerpuff.backend.service.CatalogService;
import com.powerpuff.backend.service.CatalogSearchService;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final CatalogService service;
    private final CatalogSearchService search;
    public ProductController(CatalogService service, CatalogSearchService search) {
        this.service = service;
        this.search = search;
    }
    @GetMapping("/search")
    public ProductSearchResponse search(@RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return search.search(q,page,size);
    }
    @GetMapping
    public CatalogPageResponse list(@RequestParam(defaultValue = "1") @Min(1) int page) {
        return service.list(page);
    }
    @GetMapping("/{id}")
    public CatalogProductResponse get(@PathVariable @Min(1) long id) { return service.get(id); }
}
