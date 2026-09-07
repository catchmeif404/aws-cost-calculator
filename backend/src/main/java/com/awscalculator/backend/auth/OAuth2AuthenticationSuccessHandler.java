package com.awscalculator.backend.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        String token = resolveToken(authentication);

        // Fragment (#token=), not a query param: fragments are never sent to the server on the
        // redirect request itself and never appear in access logs or a Referer header sent from
        // the callback page onward.
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(frontendBaseUrl + "/auth/callback#token=" + encodedToken);
    }

    private String resolveToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomOAuth2User customUser) {
            return jwtTokenProvider.generateToken(customUser.getUserId(), customUser.getEmail());
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.from(registrationId, oauthUser.getAttributes());
        AuthProvider provider = OAuth2UserInfoFactory.providerOf(registrationId);

        User user = userService.findOrCreate(
                provider,
                userInfo.getProviderId(),
                userInfo.getEmail(),
                userInfo.isEmailVerified(),
                userInfo.getNickname(),
                userInfo.getProfileImageUrl()
        ).user();
        return jwtTokenProvider.generateToken(user.getId(), user.getEmail());
    }
}
