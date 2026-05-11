package com.rentalService.controller;

import com.rentalService.dto.AdminRegisterDto;
import com.rentalService.dto.TokenPair;
import com.rentalService.model.AdminUser;
import com.rentalService.service.AdminAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<AdminUser> register(@RequestBody AdminRegisterDto dto) {
        return ResponseEntity.ok(adminAuthService.register(dto));
    }

    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, String>> requestOtp(@RequestBody Map<String, String> request) {
        String mobile = request.get("mobile");
        adminAuthService.requestOtp(mobile);
        Map<String, String> body = new HashMap<String, String>();
        body.put("message", "OTP sent successfully");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<TokenPair> verifyOtp(@RequestBody Map<String, String> request) {
        String mobile = request.get("mobile");
        String code = request.get("code");
        TokenPair tokens = adminAuthService.verifyOtpAndIssueTokens(mobile, code);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        TokenPair tokens = adminAuthService.refresh(refreshToken);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            Map<String, String> body = new HashMap<String, String>();
            body.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(body);
        }
        String mobile = authentication.getName();
        adminAuthService.logoutByMobile(mobile);
        Map<String, String> body = new HashMap<String, String>();
        body.put("message", "Logged out successfully");
        return ResponseEntity.ok(body);
    }
}
