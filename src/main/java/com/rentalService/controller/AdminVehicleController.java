package com.rentalService.controller;

import com.rentalService.dto.UpdateVehicleStatusRequest;
import com.rentalService.dto.VehicleListItemDto;
import com.rentalService.model.VehicleStatus;
import com.rentalService.service.VehicleAdminService;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Admin controller for vehicle moderation endpoints.
 *
 * Notes:
 * - This controller tries to extract adminId from Authentication.getPrincipal() by calling getId().
 *   If your security principal is a custom type (e.g. CustomUserDetails) that exposes getId(), it will work.
 *   Otherwise adminId will be passed as null to the service (service must handle null audit id).
 */
@RestController
@RequestMapping("/api/admin/vehicles")
public class AdminVehicleController {

    private final VehicleAdminService vehicleAdminService;

    public AdminVehicleController(VehicleAdminService vehicleAdminService) {
        this.vehicleAdminService = vehicleAdminService;
    }

    /**
     * GET /api/admin/vehicles
     * Example: /api/admin/vehicles?page=0&size=20&status=PENDING
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<VehicleListItemDto>> listVehicles(
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            @RequestParam Optional<VehicleStatus> status
    ) {
        int p = page.orElse(0);
        int s = size.orElse(20);
        Pageable pageable = PageRequest.of(p, s);
        Page<VehicleListItemDto> results = vehicleAdminService.listAllVehicles(pageable, status, Optional.empty());
        return ResponseEntity.ok(results);
    }

    /**
     * PATCH /api/admin/vehicles/{id}/status
     *
     * Body: { "status": "APPROVED", "reason": "Checked docs" }
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleListItemDto> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateVehicleStatusRequest request,
            Authentication authentication
    ) {
        UUID adminId = extractAdminIdFromPrincipal(authentication);
        VehicleListItemDto dto = vehicleAdminService.updateVehicleStatus(id, request, adminId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Best-effort extraction of admin id from authentication principal.
     * If your principal class exposes a getId() method returning UUID (or String), this will return it.
     * Otherwise returns null.
     */
    private UUID extractAdminIdFromPrincipal(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal == null) return null;

        try {
            // try getId() that returns UUID
            Object idObj = principal.getClass().getMethod("getId").invoke(principal);
            if (idObj instanceof UUID) return (UUID) idObj;
            if (idObj instanceof String) {
                try { return UUID.fromString((String) idObj); } catch (IllegalArgumentException ignored) {}
            }
        } catch (ReflectiveOperationException ignored) {
            // no getId() method or invocation failed
        }

        // fallback: try "getUserId" or numeric id conversions if you used Longs (not recommended)
        try {
            Object idObj = principal.getClass().getMethod("getUserId").invoke(principal);
            if (idObj instanceof UUID) return (UUID) idObj;
            if (idObj instanceof String) {
                try { return UUID.fromString((String) idObj); } catch (IllegalArgumentException ignored) {}
            }
        } catch (ReflectiveOperationException ignored) { }

        return null;
    }
}
