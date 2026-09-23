package com.powerpuff.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;

public record CatalogProductResponse(Long id, String name, String article, String description,
        BigDecimal price, BigDecimal quantity, List<Stock> stores,
        String image, String url, JsonNode offers, JsonNode properties) {
    public record Stock(Long id, String name, BigDecimal quantity) {}
}
