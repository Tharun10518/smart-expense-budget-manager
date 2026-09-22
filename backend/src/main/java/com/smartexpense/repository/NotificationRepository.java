package com.smartexpense.repository;

import com.smartexpense.entity.Notification;
import com.smartexpense.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

	List<Notification> findAllByUserIdAndStatus(UUID userId, NotificationStatus status);

	java.util.Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
}
