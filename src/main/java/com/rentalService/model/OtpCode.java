package com.rentalService.model;


import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "otp_codes", indexes = {
        @Index(name = "ix_otp_mobile", columnList = "mobile")
})
public class OtpCode {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Type(type = "uuid-char")
	@Column(name = "id", updatable = false, nullable = false, length = 36)
	private UUID id;


    @Column(nullable = false, length = 20)
    private String mobile;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    private int attempts;
    private boolean used;
    private OffsetDateTime createdAt;

    public OtpCode() {}

    public OtpCode(UUID id, String mobile, String code, OffsetDateTime expiresAt,
                   int attempts, boolean used, OffsetDateTime createdAt) {
        this.id = id;
        this.mobile = mobile;
        this.code = code;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
        this.used = used;
        this.createdAt = createdAt;
    }

    // ✅ Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
