package com.powerpuff.backend.client;

import com.powerpuff.backend.config.EktProperties;
import com.powerpuff.backend.exception.EktException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EktClientTest {
    MockRestServiceServer server;
    EktClient client;
    EktProperties config;
    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("https://ekt.kz")
                .defaultHeaders(h -> h.setBasicAuth("fixture", "fixture"));
        server = MockRestServiceServer.bindTo(builder).build();
        config = new EktProperties(); config.setEnabled(true);
        client = new EktClient(builder.build(), config);
    }
    @AfterEach void verify() { server.verify(); }
    @Test void detailPreservesValues() {
        server.expect(requestTo("https://ekt.kz/api/products/detail?id=515291"))
            .andExpect(header("Authorization", "Basic Zml4dHVyZTpmaXh0dXJl"))
            .andRespond(withSuccess("""
                {"id":515291,"name":"160A example","article":"000123_","price":64920.12,
                 "quantity":0.125,"stores":[{"id":2,"name":"Example","quantity":0.125}],
                 "offers":[],"properties":{"NOMINALNYY_TOK":"250 А","RECOMMEND":["001"]}}
                """, MediaType.APPLICATION_JSON));
        var p = client.getProduct(515291);
        assertThat(p.article()).isEqualTo("000123_");
        assertThat(p.price()).isEqualByComparingTo("64920.12");
        assertThat(p.quantity()).isEqualByComparingTo("0.125");
        assertThat(p.properties().get("RECOMMEND").get(0).asText()).isEqualTo("001");
        assertThat(p.properties().get("NOMINALNYY_TOK").asText()).isEqualTo("250 А");
    }
    @Test void missingStockRemainsUnknown() {
        server.expect(requestTo("https://ekt.kz/api/products?page=2"))
            .andRespond(withSuccess("""
              {"page":2,"per_page":20,"count":1,"items":[{"id":1,"name":"Example"}]}
              """, MediaType.APPLICATION_JSON));
        var p = client.getProducts(2);
        assertThat(p.items().getFirst().quantity()).isNull();
        assertThat(p.items().getFirst().price()).isNull();
    }
    @Test void emptyLastPage() {
        server.expect(requestTo("https://ekt.kz/api/products?page=3"))
            .andRespond(withSuccess("{\"page\":3,\"per_page\":20,\"count\":0,\"items\":[]}", MediaType.APPLICATION_JSON));
        assertThat(client.getProducts(3).items()).isEmpty();
    }
    @ParameterizedTest @ValueSource(ints = {401,403,404,429,500,302})
    void errorsAreSanitizedWithoutRetries(int status) {
        server.expect(requestTo("https://ekt.kz/api/products/detail?id=1"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).body("upstream-secret"));
        assertThatThrownBy(() -> client.getProduct(1)).isInstanceOf(EktException.class)
                .hasMessageNotContaining("upstream-secret");
    }
    @Test void temporaryFailureRetriesOnce() {
        server.expect(requestTo("https://ekt.kz/api/products/detail?id=1")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo("https://ekt.kz/api/products/detail?id=1"))
                .andRespond(withSuccess("{\"id\":1,\"name\":\"Example\"}", MediaType.APPLICATION_JSON));
        assertThat(client.getProduct(1).id()).isEqualTo(1);
    }
    @Test void exhaustedRetries() {
        for(int i=0;i<2;i++) server.expect(requestTo("https://ekt.kz/api/products/detail?id=1"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertThatThrownBy(() -> client.getProduct(1)).isInstanceOfSatisfying(EktException.class,
                e -> assertThat(e.getCode()).isEqualTo("EKT_UNAVAILABLE"));
    }
    @Test void timeout() {
        for(int i=0;i<2;i++) server.expect(requestTo("https://ekt.kz/api/products/detail?id=1"))
                .andRespond(withException(new SocketTimeoutException("secret")));
        assertThatThrownBy(() -> client.getProduct(1)).isInstanceOfSatisfying(EktException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }
    @ParameterizedTest @ValueSource(strings = {"not-json", "{}", "{\"id\":2,\"name\":\"wrong\"}", ""})
    void invalidDetails(String body) {
        server.expect(requestTo("https://ekt.kz/api/products/detail?id=1"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.getProduct(1)).isInstanceOfSatisfying(EktException.class,
                e -> assertThat(e.getCode()).isEqualTo("EKT_INVALID_RESPONSE"));
    }
    @Test void disabledMakesNoRequests() {
        config.setEnabled(false);
        assertThatThrownBy(() -> client.getProducts(1)).isInstanceOfSatisfying(EktException.class,
                e -> assertThat(e.getCode()).isEqualTo("EKT_DISABLED"));
    }
}
