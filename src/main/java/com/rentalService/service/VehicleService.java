package com.rentalService.service;

import com.rentalService.dto.NearbyVehicleDto;
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
import java.util.Collections;
import java.util.Comparator;
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
            MultipartFile[] otherImages,
            Double latitude,
            Double longitude
    ) throws IOException {
        validateCoordinates(latitude, longitude);

        // ✅ Find vendor using JWT mobile
        User vendor = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found for mobile: " + mobile));

        // ✅ Ensure storage directory exists
        File dir = new File(vehicleStoragePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Unable to create vehicle upload directory: " + dir.getAbsolutePath());
        }
        if (!dir.isDirectory() || !dir.canWrite()) {
            throw new IllegalStateException("Vehicle upload directory is not writable: " + dir.getAbsolutePath());
        }

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
        vehicle.setLatitude(latitude);
        vehicle.setLongitude(longitude);
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
        // No "/api" prefix in this project; keep URLs consistent with controller mapping.
        return "/vehicles/images/" + uniqueName;
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
        validateCoordinates(dto.getLatitude(), dto.getLongitude());

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
        if (dto.getLatitude() != null) vehicle.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) vehicle.setLongitude(dto.getLongitude());

        vehicle.setUpdatedAt(OffsetDateTime.now());
        return vehicleRepository.save(vehicle);
    }

    public List<NearbyVehicleDto> getNearbyVehicles(String mobile, int radiusKm) {
        if (radiusKm != 10 && radiusKm != 20 && radiusKm != 30 && radiusKm != 50) {
            throw new IllegalArgumentException("radiusKm must be one of 10, 20, 30, 50");
        }

        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getCurrentLatitude() == null || user.getCurrentLongitude() == null) {
            throw new IllegalArgumentException("Please set your location first");
        }

        List<Vehicle> activeVehicles = vehicleRepository.findByStatus("ACTIVE");
        if (activeVehicles.isEmpty()) {
            return Collections.emptyList();
        }

        List<NearbyVehicleDto> nearby = new ArrayList<NearbyVehicleDto>();
        for (Vehicle vehicle : activeVehicles) {
            if (vehicle.getLatitude() == null || vehicle.getLongitude() == null) {
                continue;
            }
            double distanceKm = haversineKm(
                    user.getCurrentLatitude(),
                    user.getCurrentLongitude(),
                    vehicle.getLatitude(),
                    vehicle.getLongitude()
            );
            if (distanceKm <= radiusKm) {
                NearbyVehicleDto dto = new NearbyVehicleDto();
                dto.setId(vehicle.getId());
                dto.setVehicleName(vehicle.getVehicleName());
                dto.setVehicleBrand(vehicle.getVehicleBrand());
                dto.setVehicleType(vehicle.getVehicleType());
                dto.setAddressLine(vehicle.getAddressLine());
                dto.setLatitude(vehicle.getLatitude());
                dto.setLongitude(vehicle.getLongitude());
                dto.setPricePerDay(vehicle.getPricePerDay());
                dto.setFrontImageUrl(vehicle.getFrontImageUrl());
                dto.setStatus(vehicle.getStatus());
                dto.setDistanceKm(roundDistance(distanceKm));
                nearby.add(dto);
            }
        }

        nearby.sort(Comparator.comparing(NearbyVehicleDto::getDistanceKm));
        return nearby;
    }

    private double haversineKm(double userLat, double userLng, double vehicleLat, double vehicleLng) {
        double earthRadiusKm = 6371.0d;
        double latDiff = Math.toRadians(vehicleLat - userLat);
        double lngDiff = Math.toRadians(vehicleLng - userLng);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(vehicleLat))
                * Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private double roundDistance(double distanceKm) {
        return Math.round(distanceKm * 100.0d) / 100.0d;
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Both latitude and longitude must be provided together");
        }
        if (latitude < -90.0d || latitude > 90.0d) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (longitude < -180.0d || longitude > 180.0d) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }
}
