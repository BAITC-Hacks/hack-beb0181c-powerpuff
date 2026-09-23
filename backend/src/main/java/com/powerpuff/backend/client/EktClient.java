package com.powerpuff.backend.client;

import com.powerpuff.backend.config.EktProperties;
import com.powerpuff.backend.exception.EktException;
import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class EktClient {
    private final RestClient http;
    private final EktProperties properties;
    public EktClient(@Qualifier("ektRestClient") RestClient http, EktProperties properties) {
        this.http = http;
        this.properties = properties;
    }

    public EktDtos.Page getProducts(int page) {
        if (page < 1) throw new IllegalArgumentException("page must be positive");
        var result = read("/api/products?page=" + page, EktDtos.Page.class, false);
        if (result.page() == null || result.page() != page || result.perPage() == null
                || result.perPage() < 1 || result.count() == null || result.items() == null
                || result.count() != result.items().size()
                || result.items().stream().anyMatch(p -> !valid(p))) throw invalid();
        return result;
    }

    public EktDtos.Product getProduct(long id) {
        if (id < 1) throw new IllegalArgumentException("id must be positive");
        var result = read("/api/products/detail?id=" + id, EktDtos.Product.class, true);
        if (!valid(result) || result.id() != id) throw invalid();
        return result;
    }

    private boolean valid(EktDtos.Product p) {
        return p != null && p.id() != null && p.id() > 0 && p.name() != null && !p.name().isBlank();
    }

    private <T> T read(String path, Class<T> type, boolean detail) {
        if (!properties.isEnabled()) throw new EktException(HttpStatus.SERVICE_UNAVAILABLE,
                "EKT_DISABLED", "EKT integration is not configured");
        // Только GET; максимум две попытки. 429, ошибки доступа и JSON не повторяем.
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                T result = http.get().uri(path).retrieve()
                    .onStatus(s -> !s.is2xxSuccessful(), (request, response) -> {
                        int status = response.getStatusCode().value();
                        if (attemptRetryable(status)) throw new TemporaryFailure();
                        if (status == 404 && detail) throw new EktException(HttpStatus.NOT_FOUND,
                                "PRODUCT_NOT_FOUND", "Product not found in EKT");
                        if (status == 429) throw new EktException(HttpStatus.SERVICE_UNAVAILABLE,
                                "EKT_RATE_LIMITED", "EKT request limit reached; try later");
                        if (status == 401 || status == 403) throw new EktException(HttpStatus.BAD_GATEWAY,
                                "EKT_AUTH_ERROR", "EKT access failed");
                        throw new EktException(HttpStatus.BAD_GATEWAY, "EKT_HTTP_ERROR", "EKT request failed");
                    }).body(type);
                if (result == null) throw invalid();
                return result;
            } catch (TemporaryFailure ex) {
                if (attempt == 1) throw new EktException(HttpStatus.BAD_GATEWAY,
                        "EKT_UNAVAILABLE", "EKT is temporarily unavailable");
            } catch (ResourceAccessException ex) {
                if (attempt == 1) {
                    boolean timeout = isTimeout(ex);
                    throw new EktException(timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                            timeout ? "EKT_TIMEOUT" : "EKT_CONNECTION_ERROR", "Cannot receive data from EKT");
                }
            } catch (RestClientException ex) {
                throw invalid();
            }
            try { Thread.sleep(250); }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new EktException(HttpStatus.SERVICE_UNAVAILABLE, "EKT_INTERRUPTED", "Request interrupted");
            }
        }
        throw new IllegalStateException("Unreachable");
    }
    private boolean attemptRetryable(int status) { return status == 502 || status == 503 || status == 504; }
    private boolean isTimeout(Throwable t) {
        for (; t != null; t = t.getCause())
            if (t instanceof HttpTimeoutException || t instanceof SocketTimeoutException) return true;
        return false;
    }
    private EktException invalid() {
        return new EktException(HttpStatus.BAD_GATEWAY, "EKT_INVALID_RESPONSE", "EKT returned an invalid response");
    }
    private static class TemporaryFailure extends RuntimeException {}
}
