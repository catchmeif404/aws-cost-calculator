package com.awscalculator.backend.credit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    @Query("select coalesce(sum(c.amount), 0) from CreditTransaction c where c.user.id = :userId")
    long sumAmountByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndType(Long userId, CreditTransactionType type);

    List<CreditTransaction> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
