package com.awscalculator.backend.auth;

import java.time.Instant;
import org.springframework.http.HttpStatus;

// Mirrors the {timestamp, status, error, message} shape GlobalExceptionHandler produces for
// exceptions caught inside the MVC dispatch — kept as a small shared record here since Security
// filter-chain rejections (401/403) happen before DispatcherServlet and can't go through that
// @RestControllerAdvice.
record ErrorBody(Instant timestamp, int status, String error, String message) {
    static ErrorBody of(HttpStatus status, String message) {
        return new ErrorBody(Instant.now(), status.value(), status.getReasonPhrase(), message);
    }
}
