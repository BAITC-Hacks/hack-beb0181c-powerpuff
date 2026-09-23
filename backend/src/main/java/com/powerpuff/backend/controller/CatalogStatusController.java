package com.powerpuff.backend.controller;

import com.powerpuff.backend.service.sync.CatalogSyncStore;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
public class CatalogStatusController {
    private final CatalogSyncStore store;
    public CatalogStatusController(CatalogSyncStore store) { this.store=store; }
    @GetMapping("/status") public Map<String,Object> status() { return store.status(); }
}
