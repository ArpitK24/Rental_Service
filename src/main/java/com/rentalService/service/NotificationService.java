package com.rentalService.service;

import com.rentalService.model.Notification;
import com.rentalService.model.User;
import com.rentalService.repository.NotificationRepository;
import com.rentalService.repository.UserRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<Notification> getMyNotifications(String mobile) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Notification markAsRead(UUID notificationId, String mobile) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to modify this notification");
        }
        if (!n.isRead()) {
            n.setRead(true);
            return notificationRepository.save(n);
        }
        return n;
    }
}
