package com.awscalculator.backend.admin.dto;

import com.awscalculator.backend.auth.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull UserRole role
) {
}
