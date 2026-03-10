package com.rentalService.repository;

import com.rentalService.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BannerRepository extends JpaRepository<Banner, UUID> {
}
