package com.awscalculator.backend.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Without this, a failed OAuth2 login (bad client secret, an exception in
 * {@link CustomOAuth2UserService}, etc.) falls through to Spring Security's default
 * {@code /login?error} — which 404s here since this app has no server-rendered login page
 * ({@code formLogin} is disabled). Logs the real cause (at ERROR, with the stack trace Spring
 * Security itself only logs at DEBUG) and sends the browser back to the actual frontend login page
 * instead of a backend Whitelabel 404.
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception
    ) throws IOException, ServletException {
        log.error("OAuth2 login failed: {}", exception.getMessage(), exception);
        String reason = URLEncoder.encode(exception.getMessage() == null ? "oauth_failed" : exception.getMessage(),
                StandardCharsets.UTF_8);
        response.sendRedirect(frontendBaseUrl + "/login?error=" + reason);
    }
}
