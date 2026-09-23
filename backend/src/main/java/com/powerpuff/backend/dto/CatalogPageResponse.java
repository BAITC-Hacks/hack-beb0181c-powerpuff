package com.powerpuff.backend.dto;

import java.util.List;

/** count — размер страницы, не общее количество каталога. */
public record CatalogPageResponse(int page, int perPage, int count, List<CatalogProductResponse> items) {}
