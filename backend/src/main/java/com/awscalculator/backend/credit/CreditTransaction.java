package com.awscalculator.backend.credit;

import com.awscalculator.backend.auth.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "credit_transactions")
@Getter
@Setter
@NoArgsConstructor
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY (unlike Resource.project's deliberate EAGER): nothing reads this relation outside an
    // open transaction — CreditService's balance/history reads run inside a short
    // @Transactional(readOnly = true) method, and callers already have the userId, so there's no
    // AI-call-style long gap between fetch and read like the one that forced Resource.project EAGER.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTransactionType type;

    // Intentionally not a @ManyToOne Order — this is an informational pointer only, so the
    // credit ledger doesn't take on a hard dependency on the payment schema this round.
    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(nullable = false)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CreditTransaction(User user, int amount, CreditTransactionType type, Long relatedOrderId, String description) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.relatedOrderId = relatedOrderId;
        this.description = description;
    }
}
