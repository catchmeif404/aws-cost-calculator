package com.awscalculator.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustCreditsRequest(
        @NotNull Integer amount,
        @NotBlank @Size(max = 200) String description
) {
}
