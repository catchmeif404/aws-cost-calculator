package com.awscalculator.backend.metrics;

import com.awscalculator.backend.auth.UserRepository;
import com.awscalculator.backend.metrics.dto.ActiveUsersSummary;
import com.awscalculator.backend.metrics.dto.DailyCount;
import com.awscalculator.backend.metrics.dto.MetricsSummaryResponse;
import com.awscalculator.backend.metrics.dto.ViewsSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalMetricsService {

    private static final int DAILY_WINDOW_DAYS = 30;

    private final UserRepository userRepository;
    private final SiteVisitEventRepository siteVisitEventRepository;

    @Transactional(readOnly = true)
    public MetricsSummaryResponse getSummary() {
        long memberCount = userRepository.count();

        Instant since = Instant.now().minus(DAILY_WINDOW_DAYS, ChronoUnit.DAYS);
        long totalViews = siteVisitEventRepository.count();
        var daily = siteVisitEventRepository.countDailySince(since).stream()
                .map(row -> new DailyCount(row.getDay(), row.getCount()))
                .toList();
        ViewsSummary views = new ViewsSummary(totalViews, daily);

        // Distinct anonymous visitor id from site-visit pings, not logged-in activity - the ping
        // fires on every page load regardless of auth state, so this already covers logged-in
        // users' browsers too without needing a separate (and double-counting) signal.
        long dau = siteVisitEventRepository.countDistinctVisitorsOnDate(LocalDate.now());
        long mau = siteVisitEventRepository.countDistinctVisitorsSince(since);
        ActiveUsersSummary activeUsers = new ActiveUsersSummary(dau, mau);

        return new MetricsSummaryResponse(memberCount, views, activeUsers);
    }
}
