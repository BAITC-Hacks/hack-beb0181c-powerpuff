package com.powerpuff.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;

public record CatalogProductResponse(Long id, String name, String article, String description,
        BigDecimal price, BigDecimal quantity, List<Stock> stores,
        String image, String url, JsonNode offers, JsonNode properties) {
    @com.fasterxml.jackson.annotation.JsonProperty("characteristicConflicts")
    public List<com.powerpuff.backend.service.CharacteristicDiagnostics.Conflict> characteristicConflicts() {
        return com.powerpuff.backend.service.CharacteristicDiagnostics.conflicts(name, description, properties);
    }
    @com.fasterxml.jackson.annotation.JsonProperty("warnings")
    public List<String> warnings() { return characteristicConflicts().stream().map(c -> c.message()).toList(); }
    @com.fasterxml.jackson.annotation.JsonProperty("stockStatus")
    public String stockStatus() { return quantity == null ? "UNKNOWN" : quantity.signum() == 0 ? "OUT_OF_STOCK" : "REPORTED_STOCK_NOT_CONFIRMED_SELLABLE"; }
    @com.fasterxml.jackson.annotation.JsonProperty("certificateSources")
    public java.util.Map<String, JsonNode> certificateSources() {
        var found = new java.util.LinkedHashMap<String, JsonNode>();
        if (properties != null && properties.isObject()) properties.fields().forEachRemaining(e -> {
            String key=e.getKey().toLowerCase(java.util.Locale.ROOT);
            if (key.contains("certif") || key.contains("sertif") || key.contains("сертиф")) found.put("properties."+e.getKey(), e.getValue());
        });
        return found;
    }
    public record Stock(Long id, String name, BigDecimal quantity) {}
}
