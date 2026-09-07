package com.smartexpense.dto;

import com.smartexpense.entity.NotificationStatus;
import com.smartexpense.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        NotificationStatus status,
        Instant readAt,
        Instant createdAt,
        Instant updatedAt) {
}
