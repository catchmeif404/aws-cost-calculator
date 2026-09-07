package com.awscalculator.backend.credit;

import com.awscalculator.backend.credit.dto.CreditBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/me")
    public CreditBalanceResponse me(@AuthenticationPrincipal Long userId) {
        return creditService.getBalance(userId);
    }
}
