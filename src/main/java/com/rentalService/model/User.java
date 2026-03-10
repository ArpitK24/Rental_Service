package com.rentalService.model;


import javax.persistence.*;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "ix_user_mobile", columnList = "mobile", unique = true)
})
public class User implements UserDetails {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Type(type = "uuid-char")
	@Column(name = "id", updatable = false, nullable = false, length = 36)
	private UUID id;



    @Column(nullable = false, length = 20, unique = true)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    private String name;
    private String email;
    private String address;
    private String city;
    private LocalDate dob;

    // Updated: store enum values
    @ElementCollection(targetClass = Interest.class)
    @Enumerated(EnumType.STRING)
    private Set<Interest> interests = new HashSet<>();

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastActiveAt;

    private boolean loggedOut;

    public User() {}

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

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

    public boolean isLoggedOut() { return loggedOut; }
    public void setLoggedOut(boolean loggedOut) { this.loggedOut = loggedOut; }

    // Spring Security methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            (GrantedAuthority) () -> "ROLE_" + role.name()
        );
    }


    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return mobile; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return !loggedOut; }
}
