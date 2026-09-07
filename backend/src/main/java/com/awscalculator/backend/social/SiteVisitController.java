package com.awscalculator.backend.social;

import com.awscalculator.backend.social.dto.SiteVisitPingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (no auth) — pinged by every page load of a site we own, so real total traffic can be
 * shown on its promotion profile. Deliberately outside {@code /api/social/**} (ROLE_ADMIN-gated),
 * same reasoning as {@link SocialLinkRedirectController}.
 */
@RestController
@RequestMapping("/api/site-visits")
@RequiredArgsConstructor
public class SiteVisitController {

    private final SiteVisitService siteVisitService;

    @PostMapping("/ping")
    public void ping(@Valid @RequestBody SiteVisitPingRequest request) {
        siteVisitService.recordVisit(request.hostname(), request.visitorId());
    }
}
