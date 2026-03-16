package com.rentalService.repository;

import com.rentalService.model.AdminRefreshToken;
import com.rentalService.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, UUID> {
    Optional<AdminRefreshToken> findByTokenAndRevokedFalse(String token);
    void deleteByAdmin(AdminUser admin);
}
