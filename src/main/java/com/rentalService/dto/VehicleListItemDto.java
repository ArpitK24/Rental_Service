package com.rentalService.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lightweight projection of a Vehicle entity for admin views.
 * Types aligned with Vehicle entity (UUID ids, OffsetDateTime timestamps, status as String).
 */
public class VehicleListItemDto {

    private UUID id;
    private String name;
    private String registrationNumber;

    private UUID ownerId;
    private String ownerName;

    // Keep status as String to match the entity column values (UNDER_REVIEW, ACTIVE, etc.)
    private String status;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public VehicleListItemDto() {}

    public VehicleListItemDto(
            UUID id,
            String name,
            String registrationNumber,
            UUID ownerId,
            String ownerName,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ------ Getters ------

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ------ Setters ------

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
