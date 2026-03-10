package com.rentalService.repository;

import com.rentalService.model.RefreshToken;
import com.rentalService.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    void deleteByUser(User user);
}
