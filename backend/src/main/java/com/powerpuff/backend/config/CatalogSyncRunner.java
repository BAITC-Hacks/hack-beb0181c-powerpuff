package com.powerpuff.backend.config;

import com.powerpuff.backend.service.sync.CatalogSynchronizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("catalog-sync")
public class CatalogSyncRunner implements CommandLineRunner {
    private final CatalogSynchronizer sync;
    private final ConfigurableApplicationContext context;
    @Value("${catalog.sync.resume:true}") boolean resume;
    @Value("${catalog.sync.max-pages:1}") int pages;
    @Value("${catalog.sync.max-details:20}") int details;
    @Value("${catalog.sync.delay-ms:250}") long delay;
    public CatalogSyncRunner(CatalogSynchronizer sync,ConfigurableApplicationContext context) { this.sync=sync;this.context=context; }
    @Override public void run(String... args) throws Exception {
        try { System.out.println("Catalog sync run id: "+sync.sync(resume,pages,details,delay)); }
        finally { context.close(); }
    }
}
