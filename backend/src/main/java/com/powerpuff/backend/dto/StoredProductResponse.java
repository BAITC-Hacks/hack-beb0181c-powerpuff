package com.powerpuff.backend.dto;

import java.time.Instant;
import java.util.List;

public record StoredProductResponse(CatalogProductResponse product, Instant listUpdatedAt,
        Instant detailUpdatedAt, String detailError, List<String> warnings) {}
