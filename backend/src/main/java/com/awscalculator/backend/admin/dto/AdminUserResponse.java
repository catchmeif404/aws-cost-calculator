package com.awscalculator.backend.admin.dto;

import com.awscalculator.backend.auth.User;
import com.awscalculator.backend.auth.UserRole;
import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String email,
        String nickname,
        UserRole role,
        boolean emailVerified,
        Instant createdAt,
        long creditBalance
) {
    public static AdminUserResponse from(User user, long creditBalance) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                creditBalance
        );
    }
}
