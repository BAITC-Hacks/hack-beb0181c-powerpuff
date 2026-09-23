package com.powerpuff.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateItemRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 1000) String description
) {
}
