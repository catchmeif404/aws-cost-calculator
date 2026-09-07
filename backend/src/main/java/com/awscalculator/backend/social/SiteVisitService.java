package com.awscalculator.backend.social;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SiteVisitService {

    private final SiteVisitRepository siteVisitRepository;

    @Transactional
    public void recordVisit(String hostname) {
        String key = normalize(hostname);
        if (key == null) {
            return;
        }
        SiteVisit visit = siteVisitRepository.findById(key).orElseGet(() -> new SiteVisit(key));
        visit.increment();
        siteVisitRepository.save(visit);
    }

    @Transactional(readOnly = true)
    public long getVisitCount(String url) {
        String key = normalizeUrl(url);
        if (key == null) {
            return 0;
        }
        return siteVisitRepository.findById(key).map(SiteVisit::getVisitCount).orElse(0L);
    }

    private String normalize(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            return null;
        }
        return hostname.trim().toLowerCase();
    }

    // Promotion profiles store a full product URL (e.g. https://aws-costpilot.trade/), but visits
    // are pinged and keyed by bare hostname — parse it the same way here so lookups match.
    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String host = URI.create(url.trim()).getHost();
            return normalize(host);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
