package com.awscalculator.backend.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

// Runs inside the Security filter chain, before DispatcherServlet — GlobalExceptionHandler
// (a @RestControllerAdvice) never sees requests rejected here, so the same JSON shape has to be
// built by hand instead of reused. ObjectMapper is injected (Spring Boot's auto-configured bean,
// with jsr310/Instant support wired in) rather than `new`d, to avoid re-deriving that config.
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorBody.of(HttpStatus.UNAUTHORIZED, "인증이 필요해요.")
        ));
    }
}
