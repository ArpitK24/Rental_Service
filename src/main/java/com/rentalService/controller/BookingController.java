package com.rentalService.controller;

import com.rentalService.model.Booking;
import com.rentalService.model.BookingStatus;
import com.rentalService.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     *  Customer: Create new booking
     * 
     * Example POST: /api/bookings/create
     * Headers: Authorization: Bearer <JWT>
     * Body (form-data or JSON):
     * {
     *   "vehicleId": "uuid",
     *   "startDate": "2025-11-10",
     *   "endDate": "2025-11-15",
     *   "withDriver": true
     * }
     */
    @PostMapping("/create")
    public ResponseEntity<Booking> createBooking(
            @RequestParam UUID vehicleId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam boolean withDriver,
            Authentication authentication
    ) {
        String mobile = authentication.getName();
        Booking booking = bookingService.createBooking(
                mobile,
                vehicleId,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate),
                withDriver
        );
        return ResponseEntity.ok(booking);
    }

    /**
     *  Vendor: Update booking status (approve/reject)
     * 
     * Example PATCH: /api/bookings/vendor/{bookingId}/status
     * Body param: status=CONFIRMED or REJECTED
     */
    @PatchMapping("/vendor/{bookingId}/status")
    public ResponseEntity<Booking> updateBookingStatus(
            @PathVariable UUID bookingId,
            @RequestParam BookingStatus status,
            Authentication authentication
    ) {
        String vendorMobile = authentication.getName();
        Booking booking = bookingService.updateBookingStatus(bookingId, status, vendorMobile);
        return ResponseEntity.ok(booking);
    }

    /**
     *  Customer: Cancel booking
     * 
     * Example PATCH: /api/bookings/{bookingId}/cancel
     */
    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        String customerMobile = authentication.getName();
        Booking booking = bookingService.cancelBooking(bookingId, customerMobile);
        return ResponseEntity.ok(booking);
    }

    /**
     *  Vendor: Get all bookings (requests, confirmed, etc.)
     * 
     * Example GET: /api/bookings/vendor/my
     */
    @GetMapping("/vendor/my")
    public ResponseEntity<List<Booking>> getVendorBookings(Authentication authentication) {
        String vendorMobile = authentication.getName();
        return ResponseEntity.ok(bookingService.getVendorBookings(vendorMobile));
    }

    /**
     *  Customer: Get all my bookings
     * 
     * Example GET: /api/bookings/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<Booking>> getCustomerBookings(Authentication authentication) {
        String customerMobile = authentication.getName();
        return ResponseEntity.ok(bookingService.getCustomerBookings(customerMobile));
    }

    /**
     * Customer: Get booking details
     * GET /bookings/{bookingId}
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBookingById(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        String customerMobile = authentication.getName();
        return ResponseEntity.ok(bookingService.getBookingByIdForCustomer(bookingId, customerMobile));
    }

    /**
     * Customer: Extend booking end date
     * PATCH /bookings/{bookingId}/extend
     * Body: { "endDate": "YYYY-MM-DD" }
     */
    @PatchMapping("/{bookingId}/extend")
    public ResponseEntity<Booking> extendBooking(
            @PathVariable UUID bookingId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        String customerMobile = authentication.getName();
        String endDate = body.get("endDate");
        Booking booking = bookingService.extendBooking(bookingId, customerMobile, LocalDate.parse(endDate));
        return ResponseEntity.ok(booking);
    }

    /**
     * Customer: Upcoming bookings
     * GET /bookings/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<Booking>> getUpcoming(Authentication authentication) {
        String customerMobile = authentication.getName();
        return ResponseEntity.ok(bookingService.getUpcomingBookings(customerMobile));
    }

    /**
     * Customer: Completed bookings
     * GET /bookings/completed
     */
    @GetMapping("/completed")
    public ResponseEntity<List<Booking>> getCompleted(Authentication authentication) {
        String customerMobile = authentication.getName();
        return ResponseEntity.ok(bookingService.getCompletedBookings(customerMobile));
    }
}
