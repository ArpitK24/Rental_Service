package com.rentalService.controller;

import com.rentalService.model.AdminUser;
import com.rentalService.model.Booking;
import com.rentalService.model.BookingStatus;
import com.rentalService.model.Role;
import com.rentalService.repository.AdminUserRepository;
import com.rentalService.repository.BookingRepository;
import com.rentalService.repository.UserRepository;
import com.rentalService.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final AdminUserRepository adminUserRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public AdminDashboardController(AdminUserRepository adminUserRepository,
                                    BookingRepository bookingRepository,
                                    UserRepository userRepository,
                                    VehicleRepository vehicleRepository) {
        this.adminUserRepository = adminUserRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> profile(Authentication authentication) {
        AdminUser admin = adminUserRepository.findByMobile(authentication.getName())
                .orElseThrow(new java.util.function.Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return new RuntimeException("Admin not found");
                    }
                });

        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", admin.getId());
        m.put("name", admin.getName());
        m.put("email", admin.getEmail());
        m.put("mobile", admin.getMobile());
        m.put("role", admin.getRole());
        m.put("createdAt", admin.getCreatedAt());
        m.put("lastActiveAt", admin.getLastActiveAt());
        return ResponseEntity.ok(m);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> bookings(
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            @RequestParam Optional<String> status
    ) {
        int p = page.orElse(0);
        int s = size.orElse(20);
        Pageable pageable = PageRequest.of(p, s);

        Page<Booking> data;
        if (status.isPresent() && status.get().trim().length() > 0) {
            BookingStatus st = BookingStatus.valueOf(status.get().trim().toUpperCase(Locale.ROOT));
            data = bookingRepository.findByStatusOrderByCreatedAtDesc(st, pageable);
        } else {
            data = bookingRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        for (Booking b : data.getContent()) {
            content.add(toBookingResponse(b));
        }

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("content", content);
        response.put("page", data.getNumber());
        response.put("size", data.getSize());
        response.put("totalElements", data.getTotalElements());
        response.put("totalPages", data.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reportsSummary() {
        List<Booking> all = bookingRepository.findAll();
        long totalBookings = all.size();
        long completed = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long confirmed = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long pending = bookingRepository.countByStatus(BookingStatus.PENDING);
        long cancelled = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long rejected = bookingRepository.countByStatus(BookingStatus.REJECTED);

        double revenue = 0.0d;
        for (Booking booking : all) {
            if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED) {
                revenue += booking.getTotalPrice();
            }
        }

        int customers = 0;
        int vendors = 0;
        List<com.rentalService.model.User> users = userRepository.findAll();
        for (com.rentalService.model.User user : users) {
            if (user.getRole() == Role.CUSTOMER) customers++;
            if (user.getRole() == Role.VENDOR) vendors++;
        }

        List<Map<String, Object>> monthlyTrend = buildLastSixMonthsRevenueTrend(all);

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("totalBookings", totalBookings);
        out.put("completedBookings", completed);
        out.put("confirmedBookings", confirmed);
        out.put("pendingBookings", pending);
        out.put("cancelledBookings", cancelled);
        out.put("rejectedBookings", rejected);
        out.put("totalRevenue", revenue);
        out.put("customersCount", customers);
        out.put("vendorsCount", vendors);
        out.put("vehiclesCount", vehicleRepository.count());
        out.put("monthlyRevenueTrend", monthlyTrend);
        return ResponseEntity.ok(out);
    }

    private List<Map<String, Object>> buildLastSixMonthsRevenueTrend(List<Booking> all) {
        Map<YearMonth, Double> bucket = new LinkedHashMap<YearMonth, Double>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            bucket.put(ym, 0.0d);
        }

        for (Booking booking : all) {
            if (booking.getCreatedAt() == null) continue;
            if (!(booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED)) {
                continue;
            }
            YearMonth ym = YearMonth.from(booking.getCreatedAt());
            if (bucket.containsKey(ym)) {
                bucket.put(ym, bucket.get(ym) + booking.getTotalPrice());
            }
        }

        List<Map<String, Object>> trend = new ArrayList<Map<String, Object>>();
        for (Map.Entry<YearMonth, Double> e : bucket.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<String, Object>();
            point.put("month", e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            point.put("year", e.getKey().getYear());
            point.put("revenue", e.getValue());
            trend.add(point);
        }
        return trend;
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
