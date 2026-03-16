package com.rentalService.controller;

import com.rentalService.model.Review;
import com.rentalService.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Customer: Submit review (only after completed booking)
     * POST /reviews
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Review> submitReview(
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        String mobile = authentication.getName();
        UUID vehicleId = UUID.fromString(String.valueOf(body.get("vehicleId")));
        int rating = Integer.parseInt(String.valueOf(body.get("rating")));
        String comment = body.get("comment") == null ? null : String.valueOf(body.get("comment"));
        Review review = reviewService.submitReview(mobile, vehicleId, rating, comment);
        return ResponseEntity.ok(review);
    }

    /**
     * Public: Get reviews for a vehicle
     * GET /reviews/vehicle/{vehicleId}
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<Review>> getReviews(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(reviewService.getReviewsForVehicle(vehicleId));
    }
}
