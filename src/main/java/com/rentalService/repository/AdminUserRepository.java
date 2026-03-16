package com.rentalService.repository;

import com.rentalService.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByMobile(String mobile);
    boolean existsByMobile(String mobile);
}
