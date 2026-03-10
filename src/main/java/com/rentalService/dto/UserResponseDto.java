package com.rentalService.dto;

import com.rentalService.model.Interest;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class UserResponseDto {

    private UUID id;
    private String mobile;
    private String role;
    private String name;
    private String email;
    private String address;
    private String city;
    private LocalDate dob;
    private Set<Interest> interests;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastActiveAt;

    public UserResponseDto() {
    }

    public UserResponseDto(UUID id, String mobile, String role, String name,
                           String email, String address, String city,
                           LocalDate dob, Set<Interest> interests,
                           OffsetDateTime createdAt, OffsetDateTime updatedAt,
                           OffsetDateTime lastActiveAt) {
        this.id = id;
        this.mobile = mobile;
        this.role = role;
        this.name = name;
        this.email = email;
        this.address = address;
        this.city = city;
        this.dob = dob;
        this.interests = interests;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastActiveAt = lastActiveAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public Set<Interest> getInterests() { return interests; }
    public void setInterests(Set<Interest> interests) { this.interests = interests; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public OffsetDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(OffsetDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
}
