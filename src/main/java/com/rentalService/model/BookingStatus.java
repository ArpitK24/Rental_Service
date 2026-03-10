package com.rentalService.model;

public enum BookingStatus {
    PENDING,        // Waiting for vendor approval
    CONFIRMED,      // Approved and confirmed by vendor
    REJECTED,       // Rejected by vendor
    CANCELLED,      // Cancelled by customer
    COMPLETED       // Trip finished successfully
}
