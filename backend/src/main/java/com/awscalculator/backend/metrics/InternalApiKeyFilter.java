package com.awscalculator.backend.metrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gates {@code /api/internal/**} with a shared secret header instead of the normal user
 * ROLE_ADMIN session/JWT auth - the caller here is catchmeif404-admin's aggregator, a service,
 * not a logged-in human. Deliberately separate from {@code /api/admin/**}'s auth so a leaked
 * metrics key can never reach real admin actions (e.g. PricingAdminController).
 */
@Component
@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.admin.metrics-api-key:}")
    private String expectedKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/internal/")) {
            String provided = request.getHeader("X-Admin-Client-Key");
            if (expectedKey.isBlank() || !expectedKey.equals(provided)) {
                // TEMPORARY - lengths only, never the values, to debug a key mismatch without
                // exposing either secret. Remove once resolved.
                log.warn("Internal API key mismatch: expectedLen={}, providedLen={}, provided null={}",
                        expectedKey.length(), provided == null ? -1 : provided.length(), provided == null);
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\":\"forbidden\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
