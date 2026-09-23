package com.powerpuff.backend.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;

/** Только формат внешнего EKT API. Не сущности БД. */
public final class EktDtos {
    private EktDtos() {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page(Integer page, @JsonProperty("per_page") Integer perPage,
                       Integer count, List<Product> items) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(Long id, String name, String article, String description,
                          BigDecimal price, BigDecimal quantity, List<Store> stores,
                          String image, String url, JsonNode offers, JsonNode properties) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Store(Long id, String name, BigDecimal quantity) {}
}
