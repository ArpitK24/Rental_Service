package com.rentalService.controller;

import com.rentalService.model.Vehicle;
import com.rentalService.service.VehicleService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * Add a new vehicle (Vendor authenticated via JWT)
     * Add access token in headers-Authorization generated through Verify OTP
     */
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Vehicle> addVehicle(
            @RequestParam String dealerName,
            @RequestParam("drivingLicense") MultipartFile drivingLicense,
            @RequestParam String addressLine,
            @RequestParam(required = false) String alternateMobile,
            @RequestParam String postalCode,
            @RequestParam String vehicleType,
            @RequestParam String vehicleName,
            @RequestParam String vehicleBrand,
            @RequestParam(required = false) String vehicleDescription,
            @RequestParam String vehicleColor,
            @RequestParam String fuel,
            @RequestParam double kilometers,
            @RequestParam int seats,
            @RequestParam String transmission,
            @RequestParam String driveType,
            @RequestParam double pricePerDay,
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage,
            @RequestParam(value = "otherImages", required = false) MultipartFile[] otherImages,
            Authentication authentication // Extract vendor from JWT
    ) throws IOException {

        String mobile = authentication.getName(); // JWT gives vendor's mobile number
        Vehicle vehicle = vehicleService.addVehicleForCurrentVendor(
                mobile,
                dealerName,
                drivingLicense,
                addressLine,
                alternateMobile,
                postalCode,
                vehicleType,
                vehicleName,
                vehicleBrand,
                vehicleDescription,
                vehicleColor,
                fuel,
                kilometers,
                seats,
                transmission,
                driveType,
                pricePerDay,
                frontImage,
                backImage,
                otherImages
        );

        return ResponseEntity.ok(vehicle);
    }

    /**
     * Vendor: View their uploaded vehicles
     */
    @GetMapping("/my")
    public ResponseEntity<List<Vehicle>> getVendorVehicles(Authentication authentication) {
        String mobile = authentication.getName();
        return ResponseEntity.ok(vehicleService.getVendorVehiclesByMobile(mobile));
    }

    /**
     * Customer: View all active/approved vehicles
     */
    @GetMapping
    public ResponseEntity<List<Vehicle>> getActiveVehicles() {
        return ResponseEntity.ok(vehicleService.getActiveVehicles());
    }

    /**
     * Get a single vehicle by ID (for customer/vendor)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    /**
     * Vendor: Delete one of their vehicles
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID id, Authentication authentication) throws IOException {
        String mobile = authentication.getName();
        vehicleService.deleteVehicleByVendor(id, mobile);
        return ResponseEntity.noContent().build();
    }

    /**
     * Vendor: Toggle vehicle status between ACTIVE and INACTIVE
     */
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<Vehicle> toggleVehicleStatus(@PathVariable UUID id, Authentication authentication) {
        String mobile = authentication.getName();
        Vehicle updatedVehicle = vehicleService.toggleVehicleStatus(id, mobile);
        return ResponseEntity.ok(updatedVehicle);
    }

    /**
     * Vendor: Update vehicle details
     */
    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> updateVehicle(
            @PathVariable UUID id,
            @RequestBody com.rentalService.dto.UpdateVehicleDto vehicleDto,
            Authentication authentication
    ) {
        String mobile = authentication.getName();
        Vehicle updatedVehicle = vehicleService.updateVehicleDetails(id, mobile, vehicleDto);
        return ResponseEntity.ok(updatedVehicle);
    }

    /**
     * Serve stored vehicle images
     */
    @GetMapping("/images/{filename}")
    public ResponseEntity<FileSystemResource> getImage(@PathVariable String filename) {
        File image = vehicleService.getVehicleImage(filename);
        if (!image.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .body(new FileSystemResource(image));
    }
}
