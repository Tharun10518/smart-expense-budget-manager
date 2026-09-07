package com.smartexpense.config;

import com.smartexpense.entity.User;
import com.smartexpense.exception.UnauthorizedException;
import com.smartexpense.repository.UserRepository;
import com.smartexpense.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("Authentication is required");
        }
        return userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
    }
}
