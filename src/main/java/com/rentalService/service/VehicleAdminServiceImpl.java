package com.rentalService.service;

import com.rentalService.dto.UpdateVehicleStatusRequest;
import com.rentalService.dto.VehicleListItemDto;
import com.rentalService.model.User;
import com.rentalService.model.Vehicle;
import com.rentalService.model.VehicleStatus;
import com.rentalService.repository.UserRepository;
import com.rentalService.repository.VehicleRepository;
import javax.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of VehicleAdminService adapted to:
 * - Vehicle.id is UUID
 * - Vehicle.status is stored as String (e.g. "UNDER_REVIEW", "ACTIVE", "REJECTED")
 * - Vehicle timestamps are OffsetDateTime
 * - Vehicle has vendor (User) relation; vendor.getId() is UUID
 */
@Service
public  class VehicleAdminServiceImpl implements VehicleAdminService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public VehicleAdminServiceImpl(VehicleRepository vehicleRepository,
                                   UserRepository userRepository,
                                   NotificationService notificationService) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * List vehicles with optional status filter. The statusFilter is an enum; persist layer uses String,
     * so we convert enum -> name() before calling repository.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<VehicleListItemDto> listAllVehicles(Pageable pageable,
                                                    Optional<VehicleStatus> statusFilter,
                                                    Optional<String> q) {
        Page<Vehicle> page;
        if (statusFilter.isPresent()) {
            String statusString = statusFilter.get().name();
            page = vehicleRepository.findByStatus(statusString, pageable);
        } else {
            page = vehicleRepository.findAll(pageable);
        }
        return page.map(this::toListItemDto);
    }

    /**
     * Update vehicle status. Uses UUID ids for vehicle and admin.
     *
     * Important: make sure VehicleAdminService interface signature uses:
     *   VehicleListItemDto updateVehicleStatus(UUID vehicleId, UpdateVehicleStatusRequest request, UUID adminId);
     */
    @Override
    @Transactional
    public VehicleListItemDto updateVehicleStatus(UUID vehicleId,
                                                  UpdateVehicleStatusRequest request,
                                                  UUID adminId) {
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("UpdateVehicleStatusRequest and status must be provided");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found with id: " + vehicleId));

        // current status stored as String in entity
        String currentStatusStr = vehicle.getStatus();
        VehicleStatus currentEnum = safeParseStatus(currentStatusStr);
        if (currentEnum == null) {
            throw new IllegalStateException("Vehicle has unknown status: " + currentStatusStr);
        }

        VehicleStatus newStatus = request.getStatus();

        // idempotent: same status
        if (currentEnum == newStatus) {
            // If REJECTED and reason provided, update reason (useful if admin wants to add reason later)
            if (newStatus == VehicleStatus.REJECTED && isNonEmpty(request.getReason())) {
                vehicle.setRejectionReason(request.getReason());
                vehicle.setUpdatedAt(OffsetDateTime.now());
                vehicleRepository.save(vehicle);
            }
            return toListItemDto(vehicle);
        }

        // validate transition using enum rules
        if (!currentEnum.canTransitionTo(newStatus)) {
            throw new IllegalStateException(String.format(
                    "Invalid status transition from %s to %s for vehicle id %s", currentEnum, newStatus, vehicleId));
        }

        // business rule: require a reason for REJECTED
        if (newStatus == VehicleStatus.REJECTED && !isNonEmpty(request.getReason())) {
            throw new IllegalArgumentException("Rejection reason is required when setting status to REJECTED");
        }

        // Apply changes: entity expects String status
        vehicle.setStatus(newStatus.name());
        vehicle.setUpdatedAt(OffsetDateTime.now());
        vehicle.setApprovedByAdminId(adminId);
        vehicle.setApprovedAt(OffsetDateTime.now());

        if (newStatus == VehicleStatus.REJECTED) {
            vehicle.setRejectionReason(request.getReason());
        } else {
            vehicle.setRejectionReason(null);
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        notifyOwnerStatusChange(saved, "ADMIN_VEHICLE_STATUS_CHANGED");

        return toListItemDto(saved);
    }

    // ---------- Helpers ----------

    private boolean isNonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private VehicleStatus safeParseStatus(String s) {
        if (s == null) return null;
        try {
            return VehicleStatus.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Map Vehicle -> VehicleListItemDto (types aligned to corrected DTO).
     */
    private VehicleListItemDto toListItemDto(Vehicle v) {
        VehicleListItemDto dto = new VehicleListItemDto();

        dto.setId(v.getId()); // UUID
        dto.setName(v.getName()); // vehicleName via getName() convenience method
        dto.setRegistrationNumber(v.getRegistrationNumber());

        // owner info
        if (v.getOwner() != null) {
            User owner = v.getOwner();
            dto.setOwnerId(owner.getId());
            dto.setOwnerName(owner.getName() != null ? owner.getName() : owner.getUsername());
        } else {
            dto.setOwnerId(v.getOwnerId());
            // try to resolve ownerName by repository lookup (best-effort)
            try {
                UUID ownerId = v.getOwnerId();
                if (ownerId != null) {
                    Optional<User> maybeUser = userRepository.findById(ownerId);
                    maybeUser.ifPresent(user ->
                            dto.setOwnerName(user.getName() != null ? user.getName() : user.getUsername()));
                }
            } catch (Exception ignored) { }
        }

        dto.setStatus(v.getStatus()); // string status
        dto.setCreatedAt(v.getCreatedAt());
        dto.setUpdatedAt(v.getUpdatedAt());

        return dto;
    }

    @Override
    @Transactional
    public VehicleListItemDto toggleVehicleActive(UUID vehicleId, UUID adminId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found with id: " + vehicleId));

        VehicleStatus current = safeParseStatus(vehicle.getStatus());
        if (current == null) {
            throw new IllegalStateException("Vehicle has unknown status: " + vehicle.getStatus());
        }

        VehicleStatus next;
        if (current == VehicleStatus.UNDER_REVIEW || current == VehicleStatus.INACTIVE) {
            next = VehicleStatus.ACTIVE;
        } else if (current == VehicleStatus.ACTIVE) {
            next = VehicleStatus.INACTIVE;
        } else {
            throw new IllegalStateException("Cannot toggle vehicle from status: " + current);
        }

        vehicle.setStatus(next.name());
        vehicle.setUpdatedAt(OffsetDateTime.now());
        vehicle.setApprovedByAdminId(adminId);
        vehicle.setApprovedAt(OffsetDateTime.now());
        vehicle.setRejectionReason(null);
        Vehicle saved = vehicleRepository.save(vehicle);
        notifyOwnerStatusChange(saved, "ADMIN_VEHICLE_TOGGLED");
        return toListItemDto(saved);
    }

    private void notifyOwnerStatusChange(Vehicle vehicle, String type) {
        try {
            User owner = vehicle.getOwner();
            if (owner == null) return;
            String name = vehicle.getVehicleName() != null ? vehicle.getVehicleName() : String.valueOf(vehicle.getId());
            notificationService.createNotification(
                    owner,
                    "Your vehicle " + name + " status changed to " + vehicle.getStatus(),
                    type
            );
        } catch (Exception ignored) {
            // Notification failures must not fail admin moderation workflow.
        }
    }
}
