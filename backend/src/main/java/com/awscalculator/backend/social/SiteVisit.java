package com.awscalculator.backend.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Raw visit counter for a hostname, keyed by the hostname itself — pinged by the frontend on
 * every page load. Only ever has real numbers for hostnames we actually own (the frontend can't
 * ping this for someone else's site).
 */
@Entity
@Table(name = "site_visits")
@Getter
@NoArgsConstructor
public class SiteVisit {

    @Id
    @Column(name = "hostname", nullable = false)
    private String hostname;

    @Column(name = "visit_count", nullable = false)
    private long visitCount;

    public SiteVisit(String hostname) {
        this.hostname = hostname;
        this.visitCount = 0;
    }

    public void increment() {
        this.visitCount++;
    }
}
