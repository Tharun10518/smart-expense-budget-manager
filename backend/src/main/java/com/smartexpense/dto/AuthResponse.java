package com.smartexpense.dto;

public record AuthResponse(String token, UserResponse user) {
}
