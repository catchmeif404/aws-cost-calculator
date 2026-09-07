package com.awscalculator.backend.credit;

import com.awscalculator.backend.auth.User;
import com.awscalculator.backend.auth.UserRepository;
import com.awscalculator.backend.auth.UserRole;
import com.awscalculator.backend.credit.dto.CreditBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditTransactionRepository creditTransactionRepository;
    private final UserRepository userRepository;

    @Value("${app.credit.signup-bonus:3}")
    private int signupBonusAmount;

    @Transactional
    public void grantSignupBonus(User user) {
        if (user.getId() != null && creditTransactionRepository.existsByUserIdAndType(user.getId(), CreditTransactionType.SIGNUP_BONUS)) {
            return;
        }
        creditTransactionRepository.save(
                new CreditTransaction(user, signupBonusAmount, CreditTransactionType.SIGNUP_BONUS, null, "회원가입 축하 크레딧")
        );
    }

    @Transactional
    public void spendUsage(Long userId, int amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit spend amount must be positive");
        }
        if (isAdmin(userId)) {
            return;
        }

        ensureSufficient(userId, amount);

        User user = userRepository.getReferenceById(userId);
        creditTransactionRepository.save(
                new CreditTransaction(user, -amount, CreditTransactionType.USAGE, null, description)
        );
    }

    @Transactional(readOnly = true)
    public void ensureSufficient(Long userId, int amount) {
        if (isAdmin(userId)) {
            return;
        }
        long balance = creditTransactionRepository.sumAmountByUserId(userId);
        if (balance < amount) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "크레딧이 부족합니다.");
        }
    }

    /** Admins use credit-gated features (calculate, AI recommendation) without spending credits. */
    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public CreditBalanceResponse getBalance(Long userId) {
        long balance = creditTransactionRepository.sumAmountByUserId(userId);
        var recent = creditTransactionRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        return CreditBalanceResponse.of(balance, recent);
    }
}
