package com.awscalculator.backend.auth;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

    List<User> findTop200ByOrderByCreatedAtDesc();

    List<User> findTop200ByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCaseOrderByCreatedAtDesc(
            String email, String nickname);

    // SELECT ... FOR UPDATE on the user row. CreditService.reserve uses it as a per-user mutex so
    // the balance check and the USAGE insert can't interleave with a concurrent spend on the same
    // account. Only call this inside a short transaction — the row lock is held until commit.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
