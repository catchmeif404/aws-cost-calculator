package com.awscalculator.backend.auth;

import com.awscalculator.backend.auth.dto.AuthResponse;
import com.awscalculator.backend.auth.dto.LoginRequest;
import com.awscalculator.backend.auth.dto.SignupRequest;
import com.awscalculator.backend.auth.dto.UserResponse;
import com.awscalculator.backend.credit.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

// No actual email verification: sending a real verification email requires an SMTP/SES
// integration this project doesn't have, so it never worked in any deployed environment (only
// via a dev-only link previously shown in the UI). Signup only guards against duplicate emails
// now; User.emailVerified stays reserved for what OAuth providers themselves assert (see
// UserService) — local signups never set it.
@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final UserRepository userRepository;
    private final CreditService creditService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        userRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(email).ifPresent(user -> {
            throw new ResponseStatusException(CONFLICT, "이미 가입된 이메일입니다.");
        });

        String nickname = request.nickname() == null || request.nickname().isBlank()
                ? email.substring(0, email.indexOf("@"))
                : request.nickname().trim();
        User user = userRepository.save(new User(email, nickname, null, passwordEncoder.encode(request.password())));
        creditService.grantSignupBonus(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(request.email().trim())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }
}
