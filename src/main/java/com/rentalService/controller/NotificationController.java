package com.rentalService.controller;

import com.rentalService.model.Notification;
import com.rentalService.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Customer: Get my notifications
     * GET /notifications
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication authentication) {
        String mobile = authentication.getName();
        return ResponseEntity.ok(notificationService.getMyNotifications(mobile));
    }

    /**
     * Customer: Mark notification as read
     * PATCH /notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String mobile = authentication.getName();
        return ResponseEntity.ok(notificationService.markAsRead(id, mobile));
    }
}
