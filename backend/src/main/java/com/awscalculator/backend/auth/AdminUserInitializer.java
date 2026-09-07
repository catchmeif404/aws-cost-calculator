package com.awscalculator.backend.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        adminProperties.emails().stream()
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .forEach(this::promoteIfExists);
    }

    private void promoteIfExists(String email) {
        userRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(email)
                .ifPresentOrElse(user -> {
                    if (user.getRole() != UserRole.ADMIN) {
                        user.promoteToAdmin();
                        log.info("Promoted admin user: {}", email);
                    }
                }, () -> log.warn("Configured admin email has no user yet: {}", email));
    }
}
