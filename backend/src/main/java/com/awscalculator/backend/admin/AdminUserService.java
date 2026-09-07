package com.awscalculator.backend.admin;

import com.awscalculator.backend.admin.dto.AdminUserResponse;
import com.awscalculator.backend.auth.User;
import com.awscalculator.backend.auth.UserRepository;
import com.awscalculator.backend.auth.UserRole;
import com.awscalculator.backend.common.NotFoundException;
import com.awscalculator.backend.credit.CreditTransaction;
import com.awscalculator.backend.credit.CreditTransactionRepository;
import com.awscalculator.backend.credit.CreditTransactionType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(String query) {
        List<User> users = (query == null || query.isBlank())
                ? userRepository.findTop200ByOrderByCreatedAtDesc()
                : userRepository.findTop200ByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCaseOrderByCreatedAtDesc(
                        query.trim(), query.trim());
        return users.stream()
                .map(user -> AdminUserResponse.from(user, creditTransactionRepository.sumAmountByUserId(user.getId())))
                .toList();
    }

    @Transactional
    public AdminUserResponse adjustCredits(Long userId, int amount, String description) {
        if (amount == 0) {
            throw new IllegalArgumentException("Adjustment amount must not be zero");
        }
        User user = requireUser(userId);
        creditTransactionRepository.save(
                new CreditTransaction(user, amount, CreditTransactionType.ADMIN_ADJUST, null, description.trim()));
        return AdminUserResponse.from(user, creditTransactionRepository.sumAmountByUserId(userId));
    }

    @Transactional
    public AdminUserResponse updateRole(Long userId, UserRole role) {
        User user = requireUser(userId);
        user.setRole(role);
        return AdminUserResponse.from(user, creditTransactionRepository.sumAmountByUserId(userId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
