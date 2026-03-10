package com.rentalService.repository;

import com.rentalService.model.Booking;
import com.rentalService.model.User;
import com.rentalService.model.Vehicle;
import com.rentalService.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // All bookings made by a specific customer
    List<Booking> findByCustomer(User customer);

    // All bookings for a specific vehicle
    List<Booking> findByVehicle(Vehicle vehicle);

    // All bookings by vehicle vendor
    List<Booking> findByVehicle_Vendor(User vendor);

    // All bookings with a specific status
    List<Booking> findByStatus(BookingStatus status);

    @Query("select count(b) from Booking b " +
            "where b.vehicle.id = :vehicleId " +
            "and b.status in :activeStatuses " +
            "and b.startDate <= :endDate " +
            "and b.endDate >= :startDate")
    long countOverlappingBookings(@Param("vehicleId") UUID vehicleId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  @Param("activeStatuses") List<BookingStatus> activeStatuses);

    @Query("select count(b) from Booking b " +
            "where b.vehicle.id = :vehicleId " +
            "and b.id <> :bookingId " +
            "and b.status in :activeStatuses " +
            "and b.startDate <= :endDate " +
            "and b.endDate >= :startDate")
    long countOverlappingBookingsExcluding(@Param("vehicleId") UUID vehicleId,
                                           @Param("bookingId") UUID bookingId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("activeStatuses") List<BookingStatus> activeStatuses);

    List<Booking> findByCustomerAndStatusInAndEndDateGreaterThanEqual(
            User customer, List<BookingStatus> statuses, LocalDate endDate);

    List<Booking> findByCustomerAndStatus(User customer, BookingStatus status);

    boolean existsByCustomerAndVehicleAndStatus(User customer, Vehicle vehicle, BookingStatus status);
}
