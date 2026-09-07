package com.awscalculator.backend.admin;

import com.awscalculator.backend.admin.dto.AdjustCreditsRequest;
import com.awscalculator.backend.admin.dto.AdminUserResponse;
import com.awscalculator.backend.admin.dto.UpdateRoleRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// ROLE_ADMIN-gated as a whole via SecurityConfig's /api/admin/** rule — no per-method check needed.
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public List<AdminUserResponse> listUsers(@RequestParam(required = false) String query) {
        return adminUserService.listUsers(query);
    }

    @PostMapping("/{userId}/credits")
    public AdminUserResponse adjustCredits(
            @PathVariable Long userId,
            @Valid @RequestBody AdjustCreditsRequest request
    ) {
        return adminUserService.adjustCredits(userId, request.amount(), request.description());
    }

    @PutMapping("/{userId}/role")
    public AdminUserResponse updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return adminUserService.updateRole(userId, request.role());
    }
}
