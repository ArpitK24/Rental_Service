package com.rentalService.service;

import com.rentalService.model.User;
import com.rentalService.model.Vehicle;
import com.rentalService.repository.UserRepository;
import com.rentalService.repository.VehicleRepository;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    @Value("${vehicle.storage.path:uploads/vehicles}")
    private String vehicleStoragePath;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Add vehicle for currently authenticated vendor (via JWT)
     */
    public Vehicle addVehicleForCurrentVendor(
            String mobile,
            String dealerName,
            MultipartFile drivingLicense,
            String addressLine,
            String alternateMobile,
            String postalCode,
            String vehicleType,
            String vehicleName,
            String vehicleBrand,
            String vehicleDescription,
            String vehicleColor,
            String fuel,
            double kilometers,
            int seats,
            String transmission,
            String driveType,
            double pricePerDay,
            MultipartFile frontImage,
            MultipartFile backImage,
            MultipartFile[] otherImages
    ) throws IOException {

        // ✅ Find vendor using JWT mobile
        User vendor = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found for mobile: " + mobile));

        // ✅ Ensure storage directory exists
        File dir = new File(vehicleStoragePath);
        if (!dir.exists()) dir.mkdirs();

        // ✅ Save images
        String drivingLicenseUrl = saveFile(drivingLicense, "license_");
        String frontImageUrl = saveFile(frontImage, "front_");
        String backImageUrl = saveFile(backImage, "back_");

        List<String> otherImageUrls = new ArrayList<>();
        if (otherImages != null) {
            int limit = Math.min(otherImages.length, 6); // allow up to 6 additional images
            for (int i = 0; i < limit; i++) {
                MultipartFile img = otherImages[i];
                if (img != null && !img.isEmpty()) {
                    String url = saveFile(img, "other_" + (i + 1) + "_");
                    otherImageUrls.add(url);
                }
            }
        }

        // ✅ Create new vehicle
        Vehicle vehicle = new Vehicle();
        vehicle.setVendor(vendor);
        vehicle.setDealerName(dealerName);
        vehicle.setDrivingLicenseImageUrl(drivingLicenseUrl);
        vehicle.setAddressLine(addressLine);
        vehicle.setAlternateMobile(alternateMobile);
        vehicle.setPostalCode(postalCode);
        vehicle.setVehicleType(vehicleType);
        vehicle.setVehicleName(vehicleName);
        vehicle.setVehicleBrand(vehicleBrand);
        vehicle.setVehicleDescription(vehicleDescription);
        vehicle.setVehicleColor(vehicleColor);
        vehicle.setFuel(fuel);
        vehicle.setKilometers(kilometers);
        vehicle.setSeats(seats);
        vehicle.setTransmission(transmission);
        vehicle.setDriveType(driveType);
        vehicle.setPricePerDay(pricePerDay);
        vehicle.setFrontImageUrl(frontImageUrl);
        vehicle.setBackImageUrl(backImageUrl);
        vehicle.setOtherImageUrls(otherImageUrls);
        vehicle.setStatus("UNDER_REVIEW");
        vehicle.setCreatedAt(OffsetDateTime.now());
        vehicle.setUpdatedAt(OffsetDateTime.now());

        return vehicleRepository.save(vehicle);
    }

    private String saveFile(MultipartFile file, String prefix) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String uniqueName = prefix + UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path destPath = Paths.get(vehicleStoragePath, uniqueName); // Java 8
        Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);
        return "/api/vehicles/images/" + uniqueName;
    }


    // 🔹 Retrieve vendor’s vehicles by mobile
    public List<Vehicle> getVendorVehiclesByMobile(String mobile) {
        User vendor = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
        return vehicleRepository.findByVendor(vendor);
    }

    // 🔹 List all active vehicles for customers
    public List<Vehicle> getActiveVehicles() {
        return vehicleRepository.findByStatus("ACTIVE");
    }

    // 🔹 Get single vehicle details
    public Vehicle getVehicleById(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
    }

    // 🔹 Delete vehicle (vendor-owned)
    public void deleteVehicleByVendor(UUID vehicleId, String mobile) throws IOException {
        User vendor = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        // Ensure vehicle belongs to this vendor
        if (!vehicle.getVendor().getId().equals(vendor.getId())) {
            throw new IllegalArgumentException("You are not authorized to delete this vehicle");
        }

        // Delete images
        deleteFile(vehicle.getDrivingLicenseImageUrl());
        deleteFile(vehicle.getFrontImageUrl());
        deleteFile(vehicle.getBackImageUrl());
        if (vehicle.getOtherImageUrls() != null) {
            for (String url : vehicle.getOtherImageUrls()) deleteFile(url);
        }

        vehicleRepository.delete(vehicle);
    }

    // 🔹 File delete helper
    private void deleteFile(String imageUrl) throws IOException {
        if (imageUrl == null) return;

        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        Path path = Paths.get(vehicleStoragePath, filename);  // Java 8 compatible
        Files.deleteIfExists(path);
    }


    // 🔹 Serve image file
    public File getVehicleImage(String filename) {
        return new File(vehicleStoragePath, filename);
    }

    public Vehicle toggleVehicleStatus(UUID vehicleId, String mobile) {
        User vendor = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        if (!vehicle.getVendor().getId().equals(vendor.getId())) {
            throw new IllegalArgumentException("You are not authorized to modify this vehicle");
        }

        // Toggle between INACTIVE and ACTIVE, but only if approved
        if ("ACTIVE".equals(vehicle.getStatus())) {
            vehicle.setStatus("INACTIVE");
        } else if ("INACTIVE".equals(vehicle.getStatus())) {
            vehicle.setStatus("ACTIVE");
        } else {
            // Do not allow toggling if vehicle is UNDER_REVIEW, REJECTED, etc.
            throw new IllegalStateException("Vehicle status cannot be toggled unless it is ACTIVE or INACTIVE. Current status: " + vehicle.getStatus());
        }

        vehicle.setUpdatedAt(OffsetDateTime.now());
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicleDetails(UUID vehicleId, String mobile, com.rentalService.dto.UpdateVehicleDto dto) {
        User vendor = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        if (!vehicle.getVendor().getId().equals(vendor.getId())) {
            throw new IllegalArgumentException("You are not authorized to modify this vehicle");
        }

        // Update fields from DTO if they are not null
        if (dto.getDealerName() != null) vehicle.setDealerName(dto.getDealerName());
        if (dto.getAddressLine() != null) vehicle.setAddressLine(dto.getAddressLine());
        if (dto.getAlternateMobile() != null) vehicle.setAlternateMobile(dto.getAlternateMobile());
        if (dto.getPostalCode() != null) vehicle.setPostalCode(dto.getPostalCode());
        if (dto.getVehicleDescription() != null) vehicle.setVehicleDescription(dto.getVehicleDescription());
        if (dto.getVehicleColor() != null) vehicle.setVehicleColor(dto.getVehicleColor());
        if (dto.getPricePerDay() != null) vehicle.setPricePerDay(dto.getPricePerDay());
        if (dto.getKilometers() != null) vehicle.setKilometers(dto.getKilometers());
        if (dto.getSeats() != null) vehicle.setSeats(dto.getSeats());

        vehicle.setUpdatedAt(OffsetDateTime.now());
        return vehicleRepository.save(vehicle);
    }
}
