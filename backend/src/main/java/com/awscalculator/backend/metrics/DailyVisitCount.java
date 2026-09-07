package com.awscalculator.backend.metrics;

import java.time.LocalDate;

// Native-query interface projection - column aliases in SiteVisitEventRepository's query
// (day, count) must match these getter names.
public interface DailyVisitCount {
    LocalDate getDay();

    long getCount();
}
