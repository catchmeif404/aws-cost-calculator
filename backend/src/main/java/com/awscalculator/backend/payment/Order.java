package com.awscalculator.backend.payment;

import com.awscalculator.backend.auth.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Schema/flow design only — no OrderService or OrderController exists yet, since there's no
 * checkout UI or real PG SDK call to drive them yet. Status lifecycle (design intent, not
 * enforced by any code yet):
 *   PENDING  - order row created when checkout is initiated, before redirecting to the PG.
 *   PAID     - PG confirms payment (webhook or client-redirect + server-side approval call);
 *              paidAt is set and a matching CreditTransaction(type=PURCHASE, amount=+creditAmount,
 *              relatedOrderId=this.id) should be inserted in the same transaction.
 *   FAILED   - PG reports approval failure, or the approval API call errors out.
 *   CANCELED - user abandons checkout before completion. Refunding an already-PAID order is a
 *              separate future concern (likely a REFUND CreditTransaction plus its own handling),
 *              not modeled here.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY — see CreditTransaction.user for the same reasoning (no non-transactional read of
    // this relation exists, unlike Resource.project).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "pg_provider", nullable = false)
    private PgProvider pgProvider;

    @Column(name = "pg_order_id", nullable = false, unique = true)
    private String pgOrderId;

    @Column(name = "amount_won", nullable = false)
    private long amountWon;

    @Column(name = "credit_amount", nullable = false)
    private int creditAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    public Order(User user, PgProvider pgProvider, String pgOrderId, long amountWon, int creditAmount) {
        this.user = user;
        this.pgProvider = pgProvider;
        this.pgOrderId = pgOrderId;
        this.amountWon = amountWon;
        this.creditAmount = creditAmount;
        this.status = OrderStatus.PENDING;
    }
}
