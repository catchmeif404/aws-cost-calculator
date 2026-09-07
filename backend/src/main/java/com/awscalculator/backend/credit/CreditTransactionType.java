package com.awscalculator.backend.credit;

// NOTE: ddl-auto=update won't refresh this column's Postgres CHECK constraint if a value is
// added later — a manual `ALTER TABLE credit_transactions DROP CONSTRAINT
// credit_transactions_type_check` will be needed then (same gotcha as ResourceType).
public enum CreditTransactionType {
    SIGNUP_BONUS,
    USAGE,
    PURCHASE,
    REFUND,
    ADMIN_ADJUST
}
