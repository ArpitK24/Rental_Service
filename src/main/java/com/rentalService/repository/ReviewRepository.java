package com.rentalService.repository;

import com.rentalService.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByVehicle_Id(UUID vehicleId);
    boolean existsByUser_IdAndVehicle_Id(UUID userId, UUID vehicleId);
}
