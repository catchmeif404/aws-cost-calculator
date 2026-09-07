package com.awscalculator.backend.metrics;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserActivityEventRepository extends JpaRepository<UserActivityEvent, Long> {

    // ON CONFLICT DO NOTHING against the (user_id, active_date) unique constraint - this runs on
    // every authenticated request (see JwtAuthenticationFilter), so it must be a no-op for the
    // 2nd..nth call within the same day rather than throwing a constraint violation.
    @Modifying
    @Query(value = "INSERT INTO user_activity_events (user_id, active_date) VALUES (:userId, :activeDate) "
            + "ON CONFLICT (user_id, active_date) DO NOTHING", nativeQuery = true)
    void recordActive(@Param("userId") Long userId, @Param("activeDate") LocalDate activeDate);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM UserActivityEvent e WHERE e.activeDate = :date")
    long countDistinctOnDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM UserActivityEvent e WHERE e.activeDate >= :since")
    long countDistinctSince(@Param("since") LocalDate since);
}
