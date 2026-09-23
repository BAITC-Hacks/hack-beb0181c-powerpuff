package com.powerpuff.backend;

import com.powerpuff.backend.entity.Product;
import com.powerpuff.backend.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BackendApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;

    @Test void health() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }
    @Test void migratedSchemaPreservesIdentifiersAndDecimals() {
        products.saveAndFlush(new Product(515291L, "Example", "00200300285_", "0012345",
                new BigDecimal("64920.12"), new BigDecimal("0.125")));
        Product found = products.findById(515291L).orElseThrow();
        assertThat(found.getArticle()).isEqualTo("00200300285_");
        assertThat(found.getBarcode()).isEqualTo("0012345");
        assertThat(found.getPrice()).isEqualByComparingTo("64920.12");
        assertThat(found.getQuantity()).isEqualByComparingTo("0.125");
        products.saveAndFlush(new Product(515292L, "Unknown", "000_", null, null, null));
        assertThat(products.findById(515292L).orElseThrow().getQuantity()).isNull();
    }
    @Test void validationIsProblemDetail() throws Exception {
        mvc.perform(post("/api/items").contentType("application/json").content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.title").exists());
    }
    @Test void malformedJsonDoesNotLeakInput() throws Exception {
        mvc.perform(post("/api/items").contentType("application/json").content("secret-not-json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Request cannot be processed"));
    }
    @Test void missingItem() throws Exception {
        mvc.perform(get("/api/items/99999999")).andExpect(status().isNotFound());
    }
    @Test void incorrectId() throws Exception {
        mvc.perform(get("/api/items/not-a-number")).andExpect(status().isBadRequest());
    }
}
