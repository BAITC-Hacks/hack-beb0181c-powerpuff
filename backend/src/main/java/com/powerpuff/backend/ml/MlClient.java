package com.powerpuff.backend.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** HTTP-клиент к Python ML-сервису (папка ml/, порт 8000). */
@Component
public class MlClient {

    private final RestClient restClient;

    public MlClient(@Value("${app.ml.url}") String mlUrl) {
        this.restClient = RestClient.builder().baseUrl(mlUrl).build();
    }

    public PredictResponse predict(PredictRequest request) {
        return restClient.post()
                .uri("/predict")
                .body(request)
                .retrieve()
                .body(PredictResponse.class);
    }
}
