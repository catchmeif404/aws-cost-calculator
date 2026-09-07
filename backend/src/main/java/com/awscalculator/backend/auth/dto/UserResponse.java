package com.awscalculator.backend.auth.dto;

import com.awscalculator.backend.auth.User;
import com.awscalculator.backend.auth.UserRole;
import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        boolean emailVerified,
        UserRole role,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.isEmailVerified(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
