package com.awscalculator.backend.payment;

// NOTE: ddl-auto=update won't refresh this column's Postgres CHECK constraint if a value is
// added later — a manual `ALTER TABLE orders DROP CONSTRAINT orders_status_check` will be
// needed then (same gotcha as ResourceType).
public enum OrderStatus {
    PENDING,
    PAID,
    FAILED,
    CANCELED
}
