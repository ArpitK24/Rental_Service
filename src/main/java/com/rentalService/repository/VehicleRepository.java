package com.rentalService.repository;

import com.rentalService.model.Vehicle;
import com.rentalService.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    // 🔹 Vendor-specific vehicles
    List<Vehicle> findByVendor(User vendor);

    // 🔹 Filter active/approved vehicles for customer browsing
    List<Vehicle> findByStatus(String status);

    // 🔹 Optional: search by city (for customer filtering)
    List<Vehicle> findByStatusAndVendor_City(String status, String city);

	Page<Vehicle> findByStatus(String statusString, Pageable pageable);
}
