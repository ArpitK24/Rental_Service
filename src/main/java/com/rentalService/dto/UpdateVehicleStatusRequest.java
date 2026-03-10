package com.rentalService.dto;

import com.rentalService.model.VehicleStatus;
import javax.validation.constraints.*;

/**
 * Request body for admin vehicle status updates (approve / reject / suspend).
 *
 * Example JSON:
 * {
 *   "status": "APPROVED",
 *   "reason": "All documents verified"
 * }
 */
public class UpdateVehicleStatusRequest {

    /**
     * New status to set for the vehicle. Required.
     * Expected values: PENDING, APPROVED, REJECTED, SUSPENDED
     */
    @NotNull(message = "status is required")
    private VehicleStatus status;

    /**
     * Optional reason text — for example, rejection reason or notes when suspending.
     * Keep within reasonable length for storage and display.
     */
    @Size(max = 1000, message = "reason must be at most 1000 characters")
    private String reason;

    public UpdateVehicleStatusRequest() {
    }

    public UpdateVehicleStatusRequest(VehicleStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
