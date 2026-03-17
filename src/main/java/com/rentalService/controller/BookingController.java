package com.rentalService.controller;

import com.rentalService.model.Booking;
import com.rentalService.model.BookingStatus;
import com.rentalService.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createBooking(
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
        return ResponseEntity.ok(toBookingResponse(booking));
    }

    @PatchMapping("/vendor/{bookingId}/status")
    public ResponseEntity<Map<String, Object>> updateBookingStatus(
            @PathVariable UUID bookingId,
            @RequestParam BookingStatus status,
            Authentication authentication
    ) {
        String vendorMobile = authentication.getName();
        Booking booking = bookingService.updateBookingStatus(bookingId, status, vendorMobile);
        return ResponseEntity.ok(toBookingResponse(booking));
    }

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        String customerMobile = authentication.getName();
        Booking booking = bookingService.cancelBooking(bookingId, customerMobile);
        return ResponseEntity.ok(toBookingResponse(booking));
    }

    @GetMapping("/vendor/my")
    public ResponseEntity<List<Map<String, Object>>> getVendorBookings(Authentication authentication) {
        String vendorMobile = authentication.getName();
        List<Map<String, Object>> response = bookingService.getVendorBookings(vendorMobile)
                .stream().map(this::toBookingResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getCustomerBookings(Authentication authentication) {
        String customerMobile = authentication.getName();
        List<Map<String, Object>> response = bookingService.getCustomerBookings(customerMobile)
                .stream().map(this::toBookingResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> getBookingById(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        String customerMobile = authentication.getName();
        Booking booking = bookingService.getBookingByIdForCustomer(bookingId, customerMobile);
        return ResponseEntity.ok(toBookingResponse(booking));
    }

    @PatchMapping("/{bookingId}/extend")
    public ResponseEntity<Map<String, Object>> extendBooking(
            @PathVariable UUID bookingId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        String customerMobile = authentication.getName();
        String endDate = body.get("endDate");
        if (endDate == null || endDate.trim().isEmpty()) {
            throw new IllegalArgumentException("endDate is required in format YYYY-MM-DD");
        }
        Booking booking = bookingService.extendBooking(bookingId, customerMobile, LocalDate.parse(endDate));
        return ResponseEntity.ok(toBookingResponse(booking));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Map<String, Object>>> getUpcoming(Authentication authentication) {
        String customerMobile = authentication.getName();
        List<Map<String, Object>> response = bookingService.getUpcomingBookings(customerMobile)
                .stream().map(this::toBookingResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<Map<String, Object>>> getCompleted(Authentication authentication) {
        String customerMobile = authentication.getName();
        List<Map<String, Object>> response = bookingService.getCompletedBookings(customerMobile)
                .stream().map(this::toBookingResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toBookingResponse(Booking booking) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", booking.getId());
        m.put("startDate", booking.getStartDate());
        m.put("endDate", booking.getEndDate());
        m.put("pickupLocation", booking.getPickupLocation());
        m.put("dropoffLocation", booking.getDropoffLocation());
        m.put("withDriver", booking.isWithDriver());
        m.put("totalPrice", booking.getTotalPrice());
        m.put("status", booking.getStatus());
        m.put("createdAt", booking.getCreatedAt());
        m.put("updatedAt", booking.getUpdatedAt());

        if (booking.getCustomer() != null) {
            Map<String, Object> customer = new LinkedHashMap<String, Object>();
            customer.put("id", booking.getCustomer().getId());
            customer.put("mobile", booking.getCustomer().getMobile());
            customer.put("name", booking.getCustomer().getName());
            m.put("customer", customer);
        }

        if (booking.getVehicle() != null) {
            Map<String, Object> vehicle = new LinkedHashMap<String, Object>();
            vehicle.put("id", booking.getVehicle().getId());
            vehicle.put("vehicleName", booking.getVehicle().getVehicleName());
            vehicle.put("vehicleBrand", booking.getVehicle().getVehicleBrand());
            vehicle.put("status", booking.getVehicle().getStatus());
            m.put("vehicle", vehicle);
        }
        return m;
    }
}
