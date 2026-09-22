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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @Test
    void registerHashesPasswordAndReturnsSafeUser() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthService authService = service();

        UserResponse response = authService.register(new RegisterRequest("John Doe", "JOHN@example.com", "Password123!"));

        assertEquals("john@example.com", response.email());
        assertEquals("John Doe", response.fullName());
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void duplicateEmailRegistrationIsRejected() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        AuthService authService = service();

        assertThrows(DuplicateEmailException.class,
                () -> authService.register(new RegisterRequest("John Doe", "john@example.com", "Password123!")));
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsJwtAndSafeUser() {
        User user = new User("john@example.com", "bcrypt-hash", "John Doe");
        CustomUserDetails userDetails = new CustomUserDetails(user);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        AuthService authService = service();

        AuthResponse response = authService.login(new LoginRequest("john@example.com", "Password123!"));

        assertEquals("jwt-token", response.token());
        assertEquals("john@example.com", response.user().email());
    }

    @Test
    void invalidLoginIsRejected() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));
        AuthService authService = service();

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("john@example.com", "wrong-password")));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void updateProfileUpdatesDisplayName() {
        User user = new User("john@example.com", "bcrypt-hash", "John Doe");
        when(userRepository.save(user)).thenReturn(user);
        AuthService authService = service();

        UserResponse response = authService.updateProfile(user, new com.smartexpense.dto.UpdateProfileRequest("Jane Doe"));

        assertEquals("Jane Doe", response.fullName());
        assertEquals("Jane Doe", user.getDisplayName());
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordSucceedsWithValidCurrentPassword() {
        User user = new User("john@example.com", "bcrypt-hash", "John Doe");
        when(passwordEncoder.matches("OldPassword123!", "bcrypt-hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword123!", "bcrypt-hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-bcrypt-hash");
        when(userRepository.save(user)).thenReturn(user);
        AuthService authService = service();

        authService.changePassword(user, new com.smartexpense.dto.ChangePasswordRequest("OldPassword123!", "NewPassword123!"));

        assertEquals("new-bcrypt-hash", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordFailsWithWrongCurrentPassword() {
        User user = new User("john@example.com", "bcrypt-hash", "John Doe");
        when(passwordEncoder.matches("WrongPassword!", "bcrypt-hash")).thenReturn(false);
        AuthService authService = service();

        assertThrows(com.smartexpense.exception.InvalidPasswordException.class,
                () -> authService.changePassword(user, new com.smartexpense.dto.ChangePasswordRequest("WrongPassword!", "NewPassword123!")));
        verify(userRepository, never()).save(any());
    }

    private AuthService service() {
        return new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }
}
