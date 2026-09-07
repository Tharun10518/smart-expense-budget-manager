package com.smartexpense.controller;

import com.smartexpense.dto.UserResponse;
import com.smartexpense.service.AuthService;
import com.smartexpense.config.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserProvider currentUserProvider;
    private final AuthService authService;

    public UserController(CurrentUserProvider currentUserProvider, AuthService authService) {
        this.currentUserProvider = currentUserProvider;
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserResponse currentUser() {
        return authService.toResponse(currentUserProvider.getCurrentUser());
    }
}
