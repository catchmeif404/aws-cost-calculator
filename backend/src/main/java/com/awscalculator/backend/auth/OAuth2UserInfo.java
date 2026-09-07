package com.awscalculator.backend.auth;

/**
 * Normalizes provider-specific OAuth2 user-info shapes (Google's flat top-level claims vs
 * Kakao's nested kakao_account/properties structure) into one internal interface.
 */
public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();
    String getNickname();
    String getProfileImageUrl();
    boolean isEmailVerified();
}
