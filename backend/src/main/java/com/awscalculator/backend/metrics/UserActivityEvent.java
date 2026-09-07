package com.awscalculator.backend.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row per (user, day) an authenticated request was made - the basis for DAU/MAU. Deliberately
 * separate from raw {@link SiteVisitEvent} page-view pings: this tracks logged-in activity, which
 * a fully anonymous ping can't (there's no persistent visitor id in that flow).
 */
@Entity
@Table(name = "user_activity_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_activity_user_date", columnNames = {"user_id", "active_date"}))
@Getter
@NoArgsConstructor
public class UserActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "active_date", nullable = false)
    private LocalDate activeDate;

    public UserActivityEvent(Long userId, LocalDate activeDate) {
        this.userId = userId;
        this.activeDate = activeDate;
    }
}
