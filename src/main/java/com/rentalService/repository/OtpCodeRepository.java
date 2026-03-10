package com.rentalService.repository;

import com.rentalService.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    Optional<OtpCode> findTopByMobileAndUsedFalseOrderByCreatedAtDesc(String mobile);
}
