package com.rentalService.service;

import com.rentalService.dto.UpdateVehicleStatusRequest;
import com.rentalService.dto.VehicleListItemDto;
import com.rentalService.model.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Service contract for admin operations on Vehicle entities.
 *
 * Implementations must enforce business rules (allowed status transitions,
 * audit fields, notifications) and throw appropriate exceptions for invalid requests
 * (e.g. EntityNotFoundException, IllegalStateException for invalid transitions).
 */
public interface VehicleAdminService {

    /**
     * Return a paged list of vehicles for admin listing.
     *
     * @param pageable pagination and sorting information
     * @param statusFilter optional status to filter by (e.g. PENDING); use Optional.empty() for no status filter
     * @param q optional free-text search across registration number, owner name, etc. (implementation-defined)
     * @return a page of VehicleListItemDto
     */
    Page<VehicleListItemDto> listAllVehicles(Pageable pageable,
                                             Optional<VehicleStatus> statusFilter,
                                             Optional<String> q);

    /**
     * Update a vehicle's status (approve / reject / suspend).
     *
     * Implementations should:
     *  - validate the vehicle exists
     *  - verify the requested transition is allowed (use VehicleStatus.canTransitionTo(...) or custom rules)
     *  - persist audit information (approvedByAdminId, approvedAt, rejectionReason, etc.)
     *  - optionally trigger notifications
     *
     * @param vehicleId the target vehicle id
     * @param request contains the new status and optional reason
     * @param adminId id of the admin performing the action (for audit)
     * @return the updated VehicleListItemDto
     * @throws javax.persistence.EntityNotFoundException if vehicle not found
     * @throws IllegalStateException if transition is not allowed
     * @throws IllegalArgumentException for invalid input
     */
    VehicleListItemDto updateVehicleStatus(Long vehicleId,
                                           UpdateVehicleStatusRequest request,
                                           Long adminId);

	/**
	 * Update vehicle status. Uses UUID ids for vehicle and admin.
	 *
	 * Important: make sure VehicleAdminService interface signature uses:
	 *   VehicleListItemDto updateVehicleStatus(UUID vehicleId, UpdateVehicleStatusRequest request, UUID adminId);
	 */
	VehicleListItemDto updateVehicleStatus(UUID vehicleId, UpdateVehicleStatusRequest request, UUID adminId);
}
