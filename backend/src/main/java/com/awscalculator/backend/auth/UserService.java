package com.awscalculator.backend.auth;

import com.awscalculator.backend.common.NotFoundException;
import com.awscalculator.backend.credit.CreditService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final CreditService creditService;

    // Provider id is the primary login key. If the provider also confirms the email is verified,
    // a first-time social login can attach to an existing local account with the same email
    // instead of creating a duplicate account. Signup bonus is still granted only once per user.
    @Transactional
    public FindOrCreateResult findOrCreate(
            AuthProvider provider,
            String providerId,
            String email,
            boolean emailVerified,
            String nickname,
            String profileImageUrl
    ) {
        var existingAccount = userOAuthAccountRepository.findByProviderAndProviderId(provider, providerId);
        if (existingAccount.isPresent()) {
            User user = existingAccount.get().getUser();
            verifyAndGrantBonusIfNeeded(user, emailVerified);
            return new FindOrCreateResult(user, false);
        }

        User user = findVerifiedEmailOwner(email, emailVerified);
        boolean created = false;
        if (user == null) {
            user = userRepository.save(new User(email, nickname, profileImageUrl));
            created = true;
        }
        verifyAndGrantBonusIfNeeded(user, emailVerified);
        userOAuthAccountRepository.save(new UserOAuthAccount(user, provider, providerId));
        return new FindOrCreateResult(user, created);
    }

    public User getOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    public record FindOrCreateResult(User user, boolean isNew) {
    }

    private User findVerifiedEmailOwner(String email, boolean emailVerified) {
        if (!emailVerified || email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(email.trim().toLowerCase()).orElse(null);
    }

    private void verifyAndGrantBonusIfNeeded(User user, boolean emailVerified) {
        if (!emailVerified) {
            return;
        }
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(Instant.now());
        }
        creditService.grantSignupBonus(user);
    }
}
