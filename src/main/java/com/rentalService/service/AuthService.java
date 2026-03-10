package com.rentalService.service;

import com.rentalService.config.JwtService;
import com.rentalService.dto.RegisterCustomerDto;
import com.rentalService.dto.RegisterVendorDto;
import com.rentalService.dto.TokenPair;
import com.rentalService.model.OtpCode;
import com.rentalService.model.RefreshToken;
import com.rentalService.model.Role;
import com.rentalService.model.User;
import com.rentalService.repository.OtpCodeRepository;
import com.rentalService.repository.RefreshTokenRepository;
import com.rentalService.repository.UserRepository;

import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final OtpCodeRepository otps;
    private final RefreshTokenRepository refreshTokens;
    private final JwtService jwt;

    private final long refreshTtlMs;
    private final int inactivityDays;
    private static final SecureRandom RAND = new SecureRandom();

    public AuthService(
            UserRepository users,
            OtpCodeRepository otps,
            RefreshTokenRepository refreshTokens,
            JwtService jwt,
            @Value("${jwt.refresh-token.expiration}") long refreshTtlMs,
            @Value("${security.inactivity-days:30}") int inactivityDays
    ) {
        this.users = users;
        this.otps = otps;
        this.refreshTokens = refreshTokens;
        this.jwt = jwt;
        this.refreshTtlMs = refreshTtlMs;
        this.inactivityDays = inactivityDays;
    }

    public void requestOtp(String mobile) {
        String code = String.format("%06d", RAND.nextInt(1_000_000));

        OtpCode otp = new OtpCode();
        otp.setMobile(mobile);
        otp.setCode(code);
        otp.setCreatedAt(OffsetDateTime.now());
        otp.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
        otp.setAttempts(0);
        otp.setUsed(false);

        otps.save(otp);
        // TODO: integrate SMS gateway
    }

    
    //chnagdes in this file 
    @Transactional
    public TokenPair verifyOtpAndIssueTokens(String mobile, String code) {
        OtpCode stored = otps.findTopByMobileAndUsedFalseOrderByCreatedAtDesc(mobile)
                .orElseThrow(new java.util.function.Supplier<IllegalArgumentException>() {
                    @Override
                    public IllegalArgumentException get() {
                        return new IllegalArgumentException("OTP not found");
                    }
                });

        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("OTP expired");
        }

        if (!stored.getCode().equals(code)) {
            stored.setAttempts(stored.getAttempts() + 1);
            otps.save(stored);
            throw new IllegalArgumentException("Invalid OTP");
        }

        stored.setUsed(true);
        otps.save(stored);

        Optional<User> maybeUser = users.findByMobile(mobile);
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();

            boolean inactive = user.getLastActiveAt() == null
                    || ChronoUnit.DAYS.between(user.getLastActiveAt(), OffsetDateTime.now()) >= inactivityDays
                    || user.isLoggedOut();

            // you can use `inactive` for extra checks if needed
            user.setLoggedOut(false);
            user.setLastActiveAt(OffsetDateTime.now());
            users.save(user);

            String at = jwt.generateAccessToken(
                    user.getId().toString(),
                    user.getRole().name(),
                    user.getMobile()
            );
            String rt = createRefresh(user);
            return new TokenPair(at, rt, true);
        } else {
            return new TokenPair(null, null, false);
        }
    }

    public User registerCustomer(RegisterCustomerDto dto) {
        User user = new User();
        user.setMobile(dto.getMobile());
        user.setRole(Role.CUSTOMER);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setAddress(dto.getAddress());
        user.setCity(dto.getCity());
        if (dto.getDob() != null) {
            user.setDob(java.time.LocalDate.parse(dto.getDob()));
        }
        if (dto.getInterests() != null) {
            user.setInterests(dto.getInterests());
        } else {
            user.setInterests(new HashSet<>()); // Java 8 instead of Set.of()
        }
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setLastActiveAt(OffsetDateTime.now());

        return users.save(user);
    }

    public User registerVendor(RegisterVendorDto dto) {
        User user = new User();
        user.setMobile(dto.getMobile());
        user.setRole(Role.VENDOR);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setAddress(dto.getAddress());
        user.setCity(dto.getCity());
        if (dto.getDob() != null) {
            user.setDob(java.time.LocalDate.parse(dto.getDob()));
        }
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setLastActiveAt(OffsetDateTime.now());

        return users.save(user);
    }

    public TokenPair issueTokensForUser(User user) {
        String at = jwt.generateAccessToken(
                user.getId().toString(),
                user.getRole().name(),
                user.getMobile()
        );
        String rt = createRefresh(user);
        return new TokenPair(at, rt, true);
    }

    public TokenPair refresh(String refreshToken) {
        RefreshToken rt = refreshTokens.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(new java.util.function.Supplier<IllegalArgumentException>() {
                    @Override
                    public IllegalArgumentException get() {
                        return new IllegalArgumentException("Invalid refresh token");
                    }
                });

        if (rt.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Refresh expired");
        }

        User user = rt.getUser();
        rt.setRevoked(true);
        refreshTokens.save(rt);

        String newAt = jwt.generateAccessToken(
                user.getId().toString(),
                user.getRole().name(),
                user.getMobile()
        );
        String newRt = createRefresh(user);
        user.setLastActiveAt(OffsetDateTime.now());
        users.save(user);

        return new TokenPair(newAt, newRt, true);
    }

    @Transactional
    public void logout(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(new java.util.function.Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return new RuntimeException("User not found");
                    }
                });
        user.setLoggedOut(true);
        users.save(user);
        refreshTokens.deleteByUser(user);
    }

    private String createRefresh(User user) {
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(token);
        rt.setCreatedAt(OffsetDateTime.now());
        rt.setExpiresAt(OffsetDateTime.now().plusSeconds(refreshTtlMs / 1000));
        rt.setRevoked(false);

        refreshTokens.save(rt);
        return token;
    }
}
