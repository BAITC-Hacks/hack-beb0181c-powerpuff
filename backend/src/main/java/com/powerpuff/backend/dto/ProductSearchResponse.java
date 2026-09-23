package com.powerpuff.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductSearchResponse(String query, int page, int size, long totalElements,
                                    long totalPages, List<Item> items) {
    public record Item(Long id, String name, String article, String supplierArticle, String brand,
                       BigDecimal price, BigDecimal quantity, String image, String url,
                       Instant listUpdatedAt, Instant detailUpdatedAt, String detailError) {}
}
