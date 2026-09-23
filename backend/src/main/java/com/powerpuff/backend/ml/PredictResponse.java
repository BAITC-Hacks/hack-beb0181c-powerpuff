package com.powerpuff.backend.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Совпадает с ml/app/schemas.py → PredictResponse */
public record PredictResponse(
        Object prediction,
        Double confidence,
        @JsonProperty("model_loaded") boolean modelLoaded
) {
}
