package com.awscalculator.backend.auth;

// NOTE: ddl-auto=update won't refresh this column's Postgres CHECK constraint if a value is
// added later — a manual `ALTER TABLE users DROP CONSTRAINT users_provider_check` will be
// needed then (same gotcha as ResourceType).
public enum AuthProvider {
    GOOGLE,
    KAKAO
}
