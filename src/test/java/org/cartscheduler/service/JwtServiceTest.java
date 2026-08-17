package org.cartscheduler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtServiceTest {

    private static final long EXPIRATION = 60_000L;

    private JwtService jwtService;

    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "test-secret-that-is-long-enough-for-hs256-signing"
                    .getBytes(StandardCharsets.UTF_8)
    );

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
    }

    @Test
    void shouldGenerateTokenWithUsernameScheduleIdAndExpiration() {
        String username = "test@te.st";
        long scheduleId = 45;

        long timestampBefore = System.currentTimeMillis();
        String token = jwtService.generateToken(username, scheduleId);
        long timestampAfter = System.currentTimeMillis();

        String decodedUsername = jwtService.extractUsername(token);
        Date decodedExpiryDate = jwtService.extractExpiration(token);
        long decodedScheduleId = jwtService.extractClaim(token, claims -> claims.get("schedule_id", Long.class));

        long expectedMinimumExpirySecond = (timestampBefore + EXPIRATION) / 1_000L;
        long expectedMaximumExpirySecond = (timestampAfter + EXPIRATION) / 1_000L;
        long actualExpirySecond = decodedExpiryDate.getTime() / 1_000L;

        assertThat(decodedUsername).isEqualTo(username);
        assertThat(actualExpirySecond).isBetween(expectedMinimumExpirySecond, expectedMaximumExpirySecond);
        assertThat(decodedScheduleId).isEqualTo(scheduleId);
    }
}
