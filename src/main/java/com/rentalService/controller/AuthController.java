package com.rentalService.controller;

import com.rentalService.dto.RegisterCustomerDto;
import com.rentalService.dto.RegisterVendorDto;
import com.rentalService.dto.TokenPair;
import com.rentalService.model.User;
import com.rentalService.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // 1. Request OTP
    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, String>> requestOtp(@RequestBody Map<String, String> request) {
        String mobile = request.get("mobile");
        authService.requestOtp(mobile);

        Map<String, String> body = new java.util.HashMap<>();
        body.put("message", "OTP sent successfully");
        return ResponseEntity.ok(body);
    }



    // 2. Verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<TokenPair> verifyOtp(@RequestBody Map<String, String> request) {
        String mobile = request.get("mobile");
        String code = request.get("code");
        TokenPair tokens = authService.verifyOtpAndIssueTokens(mobile, code);
        return ResponseEntity.ok(tokens);
    }

    // 3. Register Customer
    @PostMapping("/register-customer")
    public ResponseEntity<User> registerCustomer(@RequestBody RegisterCustomerDto dto) {
        User user = authService.registerCustomer(dto);
        TokenPair tokens = authService.issueTokensForUser(user);
        return ResponseEntity.ok(user);
    }

    // 4. Register Vendor
    @PostMapping("/register-vendor")
    public ResponseEntity<User> registerVendor(@RequestBody RegisterVendorDto dto) {
        User user = authService.registerVendor(dto);
        TokenPair tokens = authService.issueTokensForUser(user);
        return ResponseEntity.ok(user);
    }

    // 5. Refresh Token
    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        TokenPair tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok(tokens);
    }

    // 6. Logout

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        authService.logout(userId);

        Map<String, String> body = new java.util.HashMap<>();
        body.put("message", "Logged out successfully");
        return ResponseEntity.ok(body);
    }
}
