package com.awscalculator.backend.metrics;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserActivityRecorder {

    private final UserActivityEventRepository userActivityEventRepository;

    @Transactional
    public void recordActive(Long userId) {
        userActivityEventRepository.recordActive(userId, LocalDate.now());
    }
}
