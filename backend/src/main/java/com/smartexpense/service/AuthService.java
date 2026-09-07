package com.smartexpense.service;

import com.smartexpense.dto.AuthResponse;
import com.smartexpense.dto.LoginRequest;
import com.smartexpense.dto.RegisterRequest;
import com.smartexpense.dto.UserResponse;
import com.smartexpense.entity.User;
import com.smartexpense.exception.DuplicateEmailException;
import com.smartexpense.repository.UserRepository;
import com.smartexpense.security.CustomUserDetails;
import com.smartexpense.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim());
        return toResponse(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return new AuthResponse(jwtService.generateToken(userDetails), toResponse(userDetails.getUser()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
