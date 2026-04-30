package com.rentalService.controller;

import com.rentalService.service.VehicleService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Backward-compat image URL for older clients that still use "/api/vehicles/images/{filename}".
 * Newer clients should use "/vehicles/images/{filename}".
 */
@RestController
@RequestMapping("/api/vehicles/images")
public class ApiCompatVehicleImageController {

    private final VehicleService vehicleService;

    public ApiCompatVehicleImageController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/{filename}")
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

