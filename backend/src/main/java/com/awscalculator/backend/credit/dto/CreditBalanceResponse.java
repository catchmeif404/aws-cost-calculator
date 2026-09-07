package com.awscalculator.backend.credit.dto;

import com.awscalculator.backend.credit.CreditTransaction;
import java.util.List;

public record CreditBalanceResponse(long balance, List<CreditTransactionResponse> recentTransactions) {
    public static CreditBalanceResponse of(long balance, List<CreditTransaction> recentTransactions) {
        return new CreditBalanceResponse(
                balance,
                recentTransactions.stream().map(CreditTransactionResponse::from).toList()
        );
    }
}
