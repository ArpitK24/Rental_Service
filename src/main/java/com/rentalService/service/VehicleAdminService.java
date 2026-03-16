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

    VehicleListItemDto updateVehicleStatus(UUID vehicleId,
                                           UpdateVehicleStatusRequest request,
                                           UUID adminId);

    VehicleListItemDto toggleVehicleActive(UUID vehicleId, UUID adminId);
}
