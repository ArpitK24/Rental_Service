package com.rentalService.model;


import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_rental_order_id", columnList = "rental_order_id"),
    @Index(name = "idx_payments_user_id", columnList = "user_id"),
    @Index(name = "idx_payments_razorpay_payment_id", columnList = "razorpay_payment_id")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to your internal rental order id (string to allow UUIDs or other formats)
    @Column(name = "rental_order_id", nullable = false, length = 128)
    private String rentalOrderId;

    // Reference to User (store user id; map to your User.id)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Amount in smallest currency unit (e.g., paise for INR)
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency = "INR";

    // Payment lifecycle status: CREATED, AUTHORIZED, CAPTURED, FAILED, REFUNDED
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "razorpay_order_id", length = 128, unique = false)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 128, unique = true)
    private String razorpayPaymentId;

    @Column(name = "method", length = 64)
    private String method; // e.g., "card", "upi", "netbanking"

    @Column(name = "notes", columnDefinition = "json")
    private String notes; 
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Payment() {}

    public Payment(String rentalOrderId, Long userId, Long amount, String currency, String status) {
        this.rentalOrderId = rentalOrderId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    // --- getters and setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRentalOrderId() { return rentalOrderId; }
    public void setRentalOrderId(String rentalOrderId) { this.rentalOrderId = rentalOrderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment)) return false;
        Payment payment = (Payment) o;
        return Objects.equals(getId(), payment.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "Payment{" +
            "id=" + id +
            ", rentalOrderId='" + rentalOrderId + '\'' +
            ", userId=" + userId +
            ", amount=" + amount +
            ", currency='" + currency + '\'' +
            ", status='" + status + '\'' +
            ", razorpayOrderId='" + razorpayOrderId + '\'' +
            ", razorpayPaymentId='" + razorpayPaymentId + '\'' +
            ", method='" + method + '\'' +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}';
    }
}
