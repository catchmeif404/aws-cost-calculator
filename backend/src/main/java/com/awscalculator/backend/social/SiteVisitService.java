package com.awscalculator.backend.social;

import com.awscalculator.backend.metrics.SiteVisitEvent;
import com.awscalculator.backend.metrics.SiteVisitEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SiteVisitService {

    private final SiteVisitEventRepository siteVisitEventRepository;

    @Transactional
    public void recordVisit(String hostname) {
        String key = normalize(hostname);
        if (key == null) {
            return;
        }
        siteVisitEventRepository.save(new SiteVisitEvent(key, Instant.now()));
    }

    private String normalize(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            return null;
        }
        return hostname.trim().toLowerCase();
    }
}
