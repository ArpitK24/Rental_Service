package com.rentalService.repository;

import com.rentalService.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByRentalOrderId(String rentalOrderId);

    // find payments by status (e.g., CREATED, AUTHORIZED, CAPTURED)
    List<Payment> findByStatus(String status);

    // lightweight custom query example: find by rentalOrderId and status
    Optional<Payment> findFirstByRentalOrderIdAndStatus(String rentalOrderId, String status);

    // Sample native query to fetch recent payments for reconciliation (optional)
    @Query(value = "SELECT * FROM payments WHERE created_at >= NOW() - INTERVAL 1 DAY", nativeQuery = true)
    List<Payment> findPaymentsFromLast24Hours();
}
