package com.awscalculator.backend.auth;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

// 계정 자체는 로그인 방식이 아니라 사람을 의미한다. OAuth 연결 정보는 UserOAuthAccount에 둔다.
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "password_hash")
    private String passwordHash;

    // Set only when an OAuth provider itself asserts the email is verified — see UserService.
    // Local email/password signups never set this; there's no email-sending path to earn it
    // through, only signup's duplicate-email check.
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role = UserRole.USER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public User(String email, String nickname, String profileImageUrl) {
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public User(String email, String nickname, String profileImageUrl, String passwordHash) {
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.passwordHash = passwordHash;
    }

    public void promoteToAdmin() {
        this.role = UserRole.ADMIN;
    }

    public UserRole getRole() {
        return role == null ? UserRole.USER : role;
    }
}
