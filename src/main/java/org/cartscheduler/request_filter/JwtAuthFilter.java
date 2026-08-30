package org.cartscheduler.request_filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtService jwtService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        Long scheduleId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtService.extractUsername(token);
                Object scheduleIdClaim = jwtService.extractClaim(token, claims -> claims.get("schedule_id"));
                if (scheduleIdClaim instanceof Number number) {
                    scheduleId = number.longValue();
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // An invalid JWT is treated as an unauthenticated request.
            }
        }

        if (username != null && scheduleId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                RestUserDetails userDetails = (RestUserDetails) userDetailsService.loadUserByUsername(username);
                userDetails.setScheduleId(scheduleId);
                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (UsernameNotFoundException ignored) {
                // A token for a removed participant must not authenticate the request.
            }
        }
        filterChain.doFilter(request, response);
    }
}
