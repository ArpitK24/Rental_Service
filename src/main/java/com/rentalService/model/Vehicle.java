package com.rentalService.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Type(type = "uuid-char")
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private UUID id;

    // vendor (owner)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    @JsonIgnoreProperties({
            "hibernateLazyInitializer",
            "handler",
            "authorities",
            "password",
            "username",
            "enabled",
            "accountNonExpired",
            "accountNonLocked",
            "credentialsNonExpired",
            "interests"
    })
    private User vendor;

    @Column(length = 200)
    private String dealerName;

    private String drivingLicenseImageUrl;

    @Column(length = 500)
    private String addressLine;

    @Column(length = 20)
    private String alternateMobile;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 50)
    private String vehicleType;

    @Column(length = 200)
    private String vehicleName;

    @Column(length = 200)
    private String vehicleBrand;

    @Column(length = 2000)
    private String vehicleDescription;

    @Column(length = 50)
    private String vehicleColor;

    private double kilometers;
    private int seats;

    @Column(length = 50)
    private String fuel;

    @Column(length = 50)
    private String transmission;

    @Column(length = 50)
    private String driveType;

    private double pricePerDay;

    private String frontImageUrl;
    private String backImageUrl;

    @ElementCollection
    @CollectionTable(name = "vehicle_other_images", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "image_url")
    private List<String> otherImageUrls;

    private OffsetDateTime availableFrom;
    private OffsetDateTime availableUntil;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Status column (string values: UNDER_REVIEW, ACTIVE, INACTIVE, etc.)
    @Column(length = 50)
    private String status;

    // --- NEW fields required by admin flow ---
    @Column(length = 1000)
    private String rejectionReason;

    // store approving admin id as UUID
    @Type(type = "uuid-char")
    @Column(name = "approved_by_admin_id", length = 36)
    private UUID approvedByAdminId;

    private OffsetDateTime approvedAt;

    // registration number (new, add column)
    @Column(length = 100)
    private String registrationNumber;

    // ---- lifecycle ----
    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
        if (status == null) status = "UNDER_REVIEW";
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // --- Getters & Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getVendor() { return vendor; }
    public void setVendor(User vendor) { this.vendor = vendor; }

    public String getDealerName() { return dealerName; }
    public void setDealerName(String dealerName) { this.dealerName = dealerName; }

    public String getDrivingLicenseImageUrl() { return drivingLicenseImageUrl; }
    public void setDrivingLicenseImageUrl(String drivingLicenseImageUrl) { this.drivingLicenseImageUrl = drivingLicenseImageUrl; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public String getAlternateMobile() { return alternateMobile; }
    public void setAlternateMobile(String alternateMobile) { this.alternateMobile = alternateMobile; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getVehicleBrand() { return vehicleBrand; }
    public void setVehicleBrand(String vehicleBrand) { this.vehicleBrand = vehicleBrand; }

    public String getVehicleDescription() { return vehicleDescription; }
    public void setVehicleDescription(String vehicleDescription) { this.vehicleDescription = vehicleDescription; }

    public String getVehicleColor() { return vehicleColor; }
    public void setVehicleColor(String vehicleColor) { this.vehicleColor = vehicleColor; }

    public double getKilometers() { return kilometers; }
    public void setKilometers(double kilometers) { this.kilometers = kilometers; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public String getFuel() { return fuel; }
    public void setFuel(String fuel) { this.fuel = fuel; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public String getDriveType() { return driveType; }
    public void setDriveType(String driveType) { this.driveType = driveType; }

    public double getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(double pricePerDay) { this.pricePerDay = pricePerDay; }

    public String getFrontImageUrl() { return frontImageUrl; }
    public void setFrontImageUrl(String frontImageUrl) { this.frontImageUrl = frontImageUrl; }

    public String getBackImageUrl() { return backImageUrl; }
    public void setBackImageUrl(String backImageUrl) { this.backImageUrl = backImageUrl; }

    public List<String> getOtherImageUrls() { return otherImageUrls; }
    public void setOtherImageUrls(List<String> otherImageUrls) { this.otherImageUrls = otherImageUrls; }

    public OffsetDateTime getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(OffsetDateTime availableFrom) { this.availableFrom = availableFrom; }

    public OffsetDateTime getAvailableUntil() { return availableUntil; }
    public void setAvailableUntil(OffsetDateTime availableUntil) { this.availableUntil = availableUntil; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // --- Newly added getters/setters for admin flow ---

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public UUID getApprovedByAdminId() {
        return approvedByAdminId;
    }

    public void setApprovedByAdminId(UUID approvedByAdminId) {
        this.approvedByAdminId = approvedByAdminId;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    // Convenience adapter methods the service expects:

    /**
     * Service previously called getOwner(); keep that contract returning User (vendor).
     */
    @JsonIgnore
    public User getOwner() {
        return this.vendor;
    }

    /**
     * Service previously called getOwnerId(); return vendor id if vendor present.
     */
    @JsonIgnore
    public UUID getOwnerId() {
        return this.vendor == null ? null : this.vendor.getId();
    }

    /**
     * Service previously called getName(); return vehicleName as display name.
     */
    public String getName() {
        return this.vehicleName;
    }

    // If you prefer, also expose setName to maintain symmetry:
    public void setName(String name) {
        this.vehicleName = name;
    }
}
