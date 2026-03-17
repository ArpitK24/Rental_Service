package com.rentalService.controller;

import com.rentalService.model.Notification;
import com.rentalService.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public ResponseEntity<List<Map<String, Object>>> getMyNotifications(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        String mobile = authentication.getName();
        List<Map<String, Object>> response = notificationService.getMyNotifications(mobile)
                .stream().map(this::toNotificationResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Customer: Mark notification as read
     * PATCH /notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markRead(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        String mobile = authentication.getName();
        Notification n = notificationService.markAsRead(id, mobile);
        return ResponseEntity.ok(toNotificationResponse(n));
    }

    private Map<String, Object> toNotificationResponse(Notification n) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", n.getId());
        m.put("message", n.getMessage());
        m.put("type", n.getType());
        m.put("read", n.isRead());
        m.put("createdAt", n.getCreatedAt());
        return m;
    }
}
