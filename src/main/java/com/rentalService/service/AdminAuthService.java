package com.rentalService.service;

import com.rentalService.config.JwtService;
import com.rentalService.dto.AdminRegisterDto;
import com.rentalService.dto.TokenPair;
import com.rentalService.model.AdminRefreshToken;
import com.rentalService.model.AdminUser;
import com.rentalService.model.OtpCode;
import com.rentalService.model.Role;
import com.rentalService.repository.AdminRefreshTokenRepository;
import com.rentalService.repository.AdminUserRepository;
import com.rentalService.repository.OtpCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminAuthService {

    private final AdminUserRepository admins;
    private final OtpCodeRepository otps;
    private final AdminRefreshTokenRepository refreshTokens;
    private final JwtService jwt;

    private final long refreshTtlMs;
    private final int inactivityDays;
    private static final SecureRandom RAND = new SecureRandom();

    public AdminAuthService(AdminUserRepository admins,
                            OtpCodeRepository otps,
                            AdminRefreshTokenRepository refreshTokens,
                            JwtService jwt,
                            @Value("${jwt.refresh-token.expiration}") long refreshTtlMs,
                            @Value("${security.inactivity-days:30}") int inactivityDays) {
        this.admins = admins;
        this.otps = otps;
        this.refreshTokens = refreshTokens;
        this.jwt = jwt;
        this.refreshTtlMs = refreshTtlMs;
        this.inactivityDays = inactivityDays;
    }

    public AdminUser register(AdminRegisterDto dto) {
        if (admins.existsByMobile(dto.getMobile())) {
            throw new IllegalArgumentException("Admin already exists");
        }
        AdminUser admin = new AdminUser();
        admin.setMobile(dto.getMobile());
        admin.setName(dto.getName());
        admin.setEmail(dto.getEmail());
        admin.setRole(Role.ADMIN);
        admin.setCreatedAt(OffsetDateTime.now());
        admin.setUpdatedAt(OffsetDateTime.now());
        admin.setLastActiveAt(OffsetDateTime.now());
        return admins.save(admin);
    }

    public void requestOtp(String mobile) {
        AdminUser admin = admins.findByMobile(mobile)
                .orElseThrow(new java.util.function.Supplier<IllegalArgumentException>() {
                    @Override
                    public IllegalArgumentException get() {
                        return new IllegalArgumentException("Admin not found");
                    }
                });

        String code = String.format("%06d", RAND.nextInt(1_000_000));
        OtpCode otp = new OtpCode();
        otp.setMobile(admin.getMobile());
        otp.setCode(code);
        otp.setCreatedAt(OffsetDateTime.now());
        otp.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
        otp.setAttempts(0);
        otp.setUsed(false);
        otps.save(otp);
        // TODO: integrate SMS gateway
    }

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

        Optional<AdminUser> maybeAdmin = admins.findByMobile(mobile);
        if (!maybeAdmin.isPresent()) {
            return new TokenPair(null, null, false);
        }

        AdminUser admin = maybeAdmin.get();
        boolean inactive = admin.getLastActiveAt() == null
                || ChronoUnit.DAYS.between(admin.getLastActiveAt(), OffsetDateTime.now()) >= inactivityDays
                || admin.isLoggedOut();

        admin.setLoggedOut(false);
        admin.setLastActiveAt(OffsetDateTime.now());
        admins.save(admin);

        String at = jwt.generateAccessToken(
                admin.getId().toString(),
                Role.ADMIN.name(),
                admin.getMobile()
        );
        String rt = createRefresh(admin);
        return new TokenPair(at, rt, true);
    }

    public TokenPair refresh(String refreshToken) {
        AdminRefreshToken rt = refreshTokens.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(new java.util.function.Supplier<IllegalArgumentException>() {
                    @Override
                    public IllegalArgumentException get() {
                        return new IllegalArgumentException("Invalid refresh token");
                    }
                });

        if (rt.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Refresh expired");
        }

        AdminUser admin = rt.getAdmin();
        rt.setRevoked(true);
        refreshTokens.save(rt);

        String newAt = jwt.generateAccessToken(
                admin.getId().toString(),
                Role.ADMIN.name(),
                admin.getMobile()
        );
        String newRt = createRefresh(admin);
        admin.setLastActiveAt(OffsetDateTime.now());
        admins.save(admin);

        return new TokenPair(newAt, newRt, true);
    }

    @Transactional
    public void logout(UUID adminId) {
        AdminUser admin = admins.findById(adminId)
                .orElseThrow(new java.util.function.Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return new RuntimeException("Admin not found");
                    }
                });
        admin.setLoggedOut(true);
        admins.save(admin);
        refreshTokens.deleteByAdmin(admin);
    }

    private String createRefresh(AdminUser admin) {
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        AdminRefreshToken rt = new AdminRefreshToken();
        rt.setAdmin(admin);
        rt.setToken(token);
        rt.setCreatedAt(OffsetDateTime.now());
        rt.setExpiresAt(OffsetDateTime.now().plusSeconds(refreshTtlMs / 1000));
        rt.setRevoked(false);
        refreshTokens.save(rt);
        return token;
    }
}
