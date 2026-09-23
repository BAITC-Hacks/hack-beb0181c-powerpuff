package com.powerpuff.backend.ml;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Фронт → /api/predict → Spring Boot → ML-сервис. */
@RestController
@RequestMapping("/api/predict")
public class PredictController {

    private final MlClient mlClient;

    public PredictController(MlClient mlClient) {
        this.mlClient = mlClient;
    }

    @PostMapping
    public PredictResponse predict(@Valid @RequestBody PredictRequest request) {
        return mlClient.predict(request);
    }
}
