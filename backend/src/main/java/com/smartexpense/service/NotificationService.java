package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.NotificationResponse;
import com.smartexpense.entity.Notification;
import com.smartexpense.exception.ResourceNotFoundException;
import com.smartexpense.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    public NotificationService(NotificationRepository notificationRepository, CurrentUserProvider currentUserProvider) {
        this.notificationRepository = notificationRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findAll() {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserProvider.getCurrentUser().getId())
                .stream().map(this::toResponse).toList();
    }

    public NotificationResponse markAsRead(UUID id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, currentUserProvider.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.markAsRead(Instant.now());
        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(), notification.getMessage(),
                notification.getStatus(), notification.getReadAt(), notification.getCreatedAt(), notification.getUpdatedAt());
    }
}
