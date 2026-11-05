package com.awscalculator.backend.credit;

import com.awscalculator.backend.auth.User;
import com.awscalculator.backend.auth.UserRepository;
import com.awscalculator.backend.auth.UserRole;
import com.awscalculator.backend.common.NotFoundException;
import com.awscalculator.backend.credit.dto.CreditBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Credit ledger. Credit-gated AI endpoints use reserve-then-refund rather than
 * check-then-charge-afterwards:
 * <ol>
 *   <li>{@link #reserve} — one short transaction that row-locks the user, checks the balance and
 *   inserts the USAGE row, so concurrent requests on one account can't all pass the check before
 *   any of them is charged;</li>
 *   <li>the caller runs the AI call with no transaction (and no DB connection) held;</li>
 *   <li>{@link #refund} — inserts a matching REFUND row if the call threw or the answer was the
 *   rule-based fallback rather than AI-generated, so users only pay for real AI output.</li>
 * </ol>
 * Both {@link #reserve} and {@link #refund} must be called from another bean (the controllers),
 * never from inside this class — self-invocation would bypass the transactional proxy.
 */
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

    /**
     * Atomically checks the balance and spends {@code amount} credits up front. The
     * {@code SELECT ... FOR UPDATE} on the user row serializes concurrent reservations for the same
     * account; under READ COMMITTED the balance sum that follows it is a fresh statement, so it
     * sees every USAGE row committed by whoever held the lock before. Admins use credit-gated
     * features for free, so their reservation charges nothing (and refunding it is a no-op).
     */
    @Transactional
    public CreditReservation reserve(Long userId, int amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit spend amount must be positive");
        }
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (user.getRole() == UserRole.ADMIN) {
            return new CreditReservation(userId, amount, description, false);
        }

        long balance = creditTransactionRepository.sumAmountByUserId(userId);
        if (balance < amount) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "크레딧이 부족합니다.");
        }
        creditTransactionRepository.save(
                new CreditTransaction(user, -amount, CreditTransactionType.USAGE, null, description)
        );
        return new CreditReservation(userId, amount, description, true);
    }

    /**
     * Gives back a reservation's credits as a separate REFUND row (the USAGE row stays, so the
     * ledger shows both the charge and its reversal). No lock needed: a refund only ever raises
     * the balance, so it can't let a concurrent reservation overspend.
     */
    @Transactional
    public void refund(CreditReservation reservation) {
        if (!reservation.charged()) {
            return;
        }
        User user = userRepository.getReferenceById(reservation.userId());
        creditTransactionRepository.save(
                new CreditTransaction(user, reservation.amount(), CreditTransactionType.REFUND, null,
                        reservation.description() + " 환불")
        );
    }

    @Transactional(readOnly = true)
    public CreditBalanceResponse getBalance(Long userId) {
        long balance = creditTransactionRepository.sumAmountByUserId(userId);
        var recent = creditTransactionRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        return CreditBalanceResponse.of(balance, recent);
    }

    /**
     * Result of {@link #reserve}, handed back to {@link #refund}. {@code charged} is false for
     * admins, whose reservation spent nothing and so has nothing to refund.
     */
    public record CreditReservation(Long userId, int amount, String description, boolean charged) {
    }
}
