package com.powerpuff.backend.ml;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Совпадает с ml/app/schemas.py → PredictRequest */
public record PredictRequest(@NotEmpty List<Double> features) {
}
