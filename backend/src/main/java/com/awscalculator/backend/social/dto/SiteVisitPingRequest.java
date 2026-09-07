package com.awscalculator.backend.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SiteVisitPingRequest(
        @NotBlank @Size(max = 255) String hostname
) {
}
