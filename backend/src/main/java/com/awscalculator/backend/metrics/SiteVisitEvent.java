package com.awscalculator.backend.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row per page-view ping (replaces the old lifetime-only {@code site_visits} counter) so
 * totals can be broken down by day for {@code catchmeif404-admin}'s internal metrics API.
 */
@Entity
@Table(name = "site_visit_events")
@Getter
@NoArgsConstructor
public class SiteVisitEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hostname", nullable = false)
    private String hostname;

    @Column(name = "visited_at", nullable = false)
    private Instant visitedAt;

    public SiteVisitEvent(String hostname, Instant visitedAt) {
        this.hostname = hostname;
        this.visitedAt = visitedAt;
    }
}
