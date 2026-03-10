package com.rentalService.service;

import com.rentalService.model.BookingStatus;
import com.rentalService.model.Review;
import com.rentalService.model.User;
import com.rentalService.model.Vehicle;
import com.rentalService.repository.BookingRepository;
import com.rentalService.repository.ReviewRepository;
import com.rentalService.repository.UserRepository;
import com.rentalService.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         VehicleRepository vehicleRepository,
                         BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
    }

    public Review submitReview(String customerMobile, UUID vehicleId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }

        User user = userRepository.findByMobile(customerMobile)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        boolean completed = bookingRepository.existsByCustomerAndVehicleAndStatus(
                user, vehicle, BookingStatus.COMPLETED);
        if (!completed) {
            throw new IllegalArgumentException("You can review only after completing a booking for this vehicle");
        }

        boolean alreadyReviewed = reviewRepository.existsByUser_IdAndVehicle_Id(user.getId(), vehicleId);
        if (alreadyReviewed) {
            throw new IllegalArgumentException("You have already reviewed this vehicle");
        }

        Review review = new Review();
        review.setUser(user);
        review.setVehicle(vehicle);
        review.setRating(rating);
        review.setComment(comment);
        return reviewRepository.save(review);
    }

    public List<Review> getReviewsForVehicle(UUID vehicleId) {
        return reviewRepository.findByVehicle_Id(vehicleId);
    }
}
