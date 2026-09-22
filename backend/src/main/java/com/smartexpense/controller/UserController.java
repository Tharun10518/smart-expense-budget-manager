package com.smartexpense.controller;

import com.smartexpense.dto.ChangePasswordRequest;
import com.smartexpense.dto.UpdateProfileRequest;
import com.smartexpense.dto.UserResponse;
import com.smartexpense.service.AuthService;
import com.smartexpense.config.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PutMapping("/profile")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(currentUserProvider.getCurrentUser(), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUserProvider.getCurrentUser(), request);
        return ResponseEntity.noContent().build();
    }
}
