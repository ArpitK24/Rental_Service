package com.rentalService.service;

import com.rentalService.controller.CardController;
import com.rentalService.model.*;
import com.rentalService.repository.BookingRepository;
import com.rentalService.repository.UserRepository;
import com.rentalService.repository.VehicleRepository;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BookingService {

    private final CardController cardController;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          VehicleRepository vehicleRepository, CardController cardController) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.cardController = cardController;
    }

    /**
     * ✅ Create a new booking (customer side)
     */
    public Booking createBooking(String customerMobile, UUID vehicleId,
                                 LocalDate startDate, LocalDate endDate, boolean withDriver) {

        // Fetch customer
        User customer = userRepository.findByMobile(customerMobile)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // Fetch vehicle
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        // Date validation
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Check if vehicle is already booked for that period
        List<Booking> existing = bookingRepository.findByVehicle(vehicle);
        for (Booking b : existing) {
            if (!(endDate.isBefore(b.getStartDate()) || startDate.isAfter(b.getEndDate()))) {
                throw new IllegalArgumentException("Vehicle already booked for this period");
            }
        }

        // Calculate price
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days == 0) days = 1; // at least one day
        double totalPrice = days * vehicle.getPricePerDay();

        // Create booking
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setVehicle(vehicle);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setWithDriver(withDriver);
        booking.setPickupLocation(vehicle.getAddressLine()); // default, optional
        booking.setDropoffLocation(vehicle.getAddressLine());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }

    /**
     * ✅ Vendor approves or rejects booking
     */
    public Booking updateBookingStatus(UUID bookingId, BookingStatus status, String vendorMobile) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Validate vendor
        User vendor = userRepository.findByMobile(vendorMobile)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        if (!booking.getVehicle().getVendor().getId().equals(vendor.getId())) {
            throw new IllegalArgumentException("You are not authorized to modify this booking");
        }

        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    /**
     * ✅ Customer cancels their booking
     */
    public Booking cancelBooking(UUID bookingId, String customerMobile) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getCustomer().getMobile().equals(customerMobile)) {
            throw new IllegalArgumentException("Unauthorized cancel attempt");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    /**
     * ✅ Vendor: view all bookings of their vehicles
     */
    public List<Booking> getVendorBookings(String vendorMobile) {
        User vendor = userRepository.findByMobile(vendorMobile)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
        return bookingRepository.findByVehicle_Vendor(vendor);
    }

    /**
     * ✅ Customer: view all their bookings
     */
    public List<Booking> getCustomerBookings(String customerMobile) {
        User customer = userRepository.findByMobile(customerMobile)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        return bookingRepository.findByCustomer(customer);
    }

    /**
     * ✅ Get booking details (customer only)
     */
    public Booking getBookingByIdForCustomer(UUID bookingId, String customerMobile) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getCustomer().getMobile().equals(customerMobile)) {
            throw new IllegalArgumentException("You are not authorized to view this booking");
        }
        return booking;
    }

    /**
     * ✅ Extend booking end date (customer only)
     */
    public Booking extendBooking(UUID bookingId, String customerMobile, LocalDate newEndDate) {
        if (newEndDate == null) {
            throw new IllegalArgumentException("newEndDate is required");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getCustomer().getMobile().equals(customerMobile)) {
            throw new IllegalArgumentException("You are not authorized to modify this booking");
        }

        if (newEndDate.isBefore(booking.getEndDate())) {
            throw new IllegalArgumentException("newEndDate must be after current endDate");
        }

        List<BookingStatus> active = new java.util.ArrayList<BookingStatus>();
        active.add(BookingStatus.PENDING);
        active.add(BookingStatus.CONFIRMED);

        long conflicts = bookingRepository.countOverlappingBookingsExcluding(
                booking.getVehicle().getId(),
                booking.getId(),
                booking.getStartDate(),
                newEndDate,
                active
        );
        if (conflicts > 0) {
            throw new IllegalArgumentException("Vehicle already booked for the extended period");
        }

        long days = ChronoUnit.DAYS.between(booking.getStartDate(), newEndDate);
        if (days == 0) days = 1;
        booking.setEndDate(newEndDate);
        booking.setTotalPrice(days * booking.getVehicle().getPricePerDay());
        return bookingRepository.save(booking);
    }

    /**
     * ✅ Upcoming bookings (customer only)
     */
    public List<Booking> getUpcomingBookings(String customerMobile) {
        User customer = userRepository.findByMobile(customerMobile)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        List<BookingStatus> statuses = new java.util.ArrayList<BookingStatus>();
        statuses.add(BookingStatus.PENDING);
        statuses.add(BookingStatus.CONFIRMED);
        return bookingRepository.findByCustomerAndStatusInAndEndDateGreaterThanEqual(
                customer, statuses, LocalDate.now());
    }

    /**
     * ✅ Completed bookings (customer only)
     */
    public List<Booking> getCompletedBookings(String customerMobile) {
        User customer = userRepository.findByMobile(customerMobile)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        return bookingRepository.findByCustomerAndStatus(customer, BookingStatus.COMPLETED);
    }
}
