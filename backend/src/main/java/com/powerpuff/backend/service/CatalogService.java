package com.powerpuff.backend.service;

import com.powerpuff.backend.client.*;
import com.powerpuff.backend.dto.*;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    private final EktClient client;
    public CatalogService(EktClient client) { this.client = client; }
    public CatalogPageResponse list(int page) {
        var p = client.getProducts(page);
        return new CatalogPageResponse(p.page(), p.perPage(), p.count(), p.items().stream().map(this::map).toList());
    }
    public CatalogProductResponse get(long id) { return map(client.getProduct(id)); }
    private CatalogProductResponse map(EktDtos.Product p) {
        var stocks = p.stores() == null ? null : p.stores().stream()
                .map(s -> new CatalogProductResponse.Stock(s.id(), s.name(), s.quantity())).toList();
        return new CatalogProductResponse(p.id(), p.name(), p.article(), p.description(), p.price(),
                p.quantity(), stocks, p.image(), p.url(), p.offers(), p.properties());
    }
}
