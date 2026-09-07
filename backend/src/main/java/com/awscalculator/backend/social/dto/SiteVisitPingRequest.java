package com.awscalculator.backend.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SiteVisitPingRequest(
        @NotBlank @Size(max = 255) String hostname,
        // Client-generated, persisted in localStorage - same value across page loads from the
        // same browser regardless of login state, so it's what distinct-visitor DAU/MAU counts
        // against. Optional: older frontend builds and clients with localStorage blocked won't
        // send one, and a ping without it still counts toward raw view totals.
        @Size(max = 64) String visitorId
) {
}
