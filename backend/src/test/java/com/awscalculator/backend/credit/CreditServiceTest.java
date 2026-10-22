package com.awscalculator.backend.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.awscalculator.backend.auth.User;
import com.awscalculator.backend.auth.UserRepository;
import com.awscalculator.backend.credit.CreditService.CreditReservation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Runs against the real database (row locks only mean something there) with committed data —
 * deliberately not {@code @Transactional}, since the concurrency test needs each thread's
 * reservation to commit on its own connection. Each test creates its own throwaway user and
 * removes it, plus its ledger rows, afterwards.
 */
@SpringBootTest
class CreditServiceTest {

    @Autowired
    private CreditService creditService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User(
                "credit-test-" + UUID.randomUUID() + "@example.test", "credit-test", null));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from credit_transactions where user_id = ?", user.getId());
        userRepository.deleteById(user.getId());
    }

    @Test
    void reserveSpendsUpFrontAndRefundGivesItBack() {
        grant(3);

        CreditReservation reservation = creditService.reserve(user.getId(), 1, "AI 설명");

        assertThat(reservation.charged()).isTrue();
        assertThat(balance()).isEqualTo(2);

        creditService.refund(reservation);

        assertThat(balance()).isEqualTo(3);
        assertThat(creditTransactionRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId()))
                .extracting(CreditTransaction::getType)
                .containsExactlyInAnyOrder(
                        CreditTransactionType.ADMIN_ADJUST, CreditTransactionType.USAGE, CreditTransactionType.REFUND);
    }

    @Test
    void reserveRejectsInsufficientCreditsWithoutChargingAnything() {
        assertThatThrownBy(() -> creditService.reserve(user.getId(), 1, "AI 설명"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));

        assertThat(balance()).isZero();
        assertThat(creditTransactionRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId())).isEmpty();
    }

    @Test
    void adminReservationChargesNothingAndRefundIsANoOp() {
        user.promoteToAdmin();
        user = userRepository.save(user);

        CreditReservation reservation = creditService.reserve(user.getId(), 1, "AI 설명");
        creditService.refund(reservation);

        assertThat(reservation.charged()).isFalse();
        assertThat(creditTransactionRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId())).isEmpty();
    }

    @Test
    void concurrentReservationsNeverSpendMoreThanTheBalance() throws Exception {
        int startingBalance = 3;
        int concurrentRequests = 8;
        grant(startingBalance);

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < concurrentRequests; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        creditService.reserve(user.getId(), 1, "AI 설명");
                        return true;
                    } catch (ResponseStatusException e) {
                        return false;
                    }
                }));
            }
            start.countDown();

            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }

            assertThat(succeeded).isEqualTo(startingBalance);
            assertThat(balance()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    private void grant(int amount) {
        creditTransactionRepository.save(
                new CreditTransaction(user, amount, CreditTransactionType.ADMIN_ADJUST, null, "test grant"));
    }

    private long balance() {
        return creditTransactionRepository.sumAmountByUserId(user.getId());
    }
}
