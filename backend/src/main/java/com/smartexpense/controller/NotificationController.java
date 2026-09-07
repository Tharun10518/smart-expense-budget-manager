package com.smartexpense.controller;

import com.smartexpense.dto.NotificationResponse;
import com.smartexpense.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> findAll() {
        return notificationService.findAll();
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable UUID id) {
        return notificationService.markAsRead(id);
    }
}
