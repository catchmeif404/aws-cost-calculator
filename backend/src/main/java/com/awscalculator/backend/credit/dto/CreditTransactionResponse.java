package com.awscalculator.backend.credit.dto;

import com.awscalculator.backend.credit.CreditTransaction;
import com.awscalculator.backend.credit.CreditTransactionType;
import java.time.Instant;

public record CreditTransactionResponse(
        Long id,
        int amount,
        CreditTransactionType type,
        String description,
        Instant createdAt
) {
    public static CreditTransactionResponse from(CreditTransaction transaction) {
        return new CreditTransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
