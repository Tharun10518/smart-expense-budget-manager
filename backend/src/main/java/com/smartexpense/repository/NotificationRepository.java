package com.smartexpense.repository;

import com.smartexpense.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

	java.util.Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
}
