package org.cartscheduler.request_filter;

import io.jsonwebtoken.MalformedJwtException;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cartscheduler.support.TestEntityFactory.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldLeaveRequestUnauthenticatedWhenAuthorizationHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/schedules/1/");

        jwtAuthFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void shouldAuthenticateRequestWithValidTokenAndScheduleClaim() throws Exception {
        String token = "valid-token";
        RestUserDetails userDetails = user(1L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/schedules/5/");
        request.addHeader("Authorization", "Bearer " + token);
        given(jwtService.extractUsername(token)).willReturn(userDetails.getUsername());
        given(jwtService.extractClaim(eq(token), any())).willReturn(5L);
        given(userDetailsService.loadUserByUsername(userDetails.getUsername())).willReturn(userDetails);
        given(jwtService.validateToken(token, userDetails)).willReturn(true);

        jwtAuthFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(userDetails);
        assertThat(userDetails.getScheduleId()).isEqualTo(5L);
        verify(userDetailsService).loadUserByUsername(userDetails.getUsername());
    }

    @Test
    void shouldTreatMalformedTokenAsUnauthenticatedRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/schedules/1/");
        request.addHeader("Authorization", "Bearer malformed-token");
        given(jwtService.extractUsername("malformed-token")).willThrow(new MalformedJwtException("Malformed token"));

        jwtAuthFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void shouldNotAuthenticateTokenWithoutScheduleClaim() throws Exception {
        String token = "token-without-schedule";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/schedules/1/");
        request.addHeader("Authorization", "Bearer " + token);
        given(jwtService.extractUsername(token)).willReturn("participant1@example.test");
        given(jwtService.extractClaim(eq(token), any())).willReturn(null);

        jwtAuthFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }
}
