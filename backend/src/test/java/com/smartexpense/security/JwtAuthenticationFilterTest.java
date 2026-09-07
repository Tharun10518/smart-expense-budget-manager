package com.smartexpense.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private CustomUserDetails userDetails;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validJwtPopulatesAuthenticationContext() throws Exception {
        JwtService jwtService = new JwtService("01234567890123456789012345678901", 60_000);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        when(userDetails.getUsername()).thenReturn("john@example.com");
        when(userDetails.getUserId()).thenReturn(UUID.randomUUID());
        String token = jwtService.generateToken(userDetails);
        when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertSame(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals("john@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
