package org.cartscheduler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import io.jsonwebtoken.JwtException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cartscheduler.support.TestEntityFactory.user;

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

    @Test
    void shouldValidateTokenOnlyForParticipantWithMatchingEmail() {
        String token = jwtService.generateToken("participant1@example.test", 5L);

        boolean result = jwtService.validateToken(token, user(1L));

        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectTokenForParticipantWithDifferentEmail() {
        String token = jwtService.generateToken("participant1@example.test", 5L);

        boolean result = jwtService.validateToken(token, user(2L));

        assertThat(result).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1L);
        String token = jwtService.generateToken("participant1@example.test", 5L);

        boolean result = jwtService.validateToken(token, user(1L));

        assertThat(result).isFalse();
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtService otherJwtService = new JwtService();
        String otherSecret = Base64.getEncoder().encodeToString(
                "another-secret-that-is-long-enough-for-hs256-signing"
                        .getBytes(StandardCharsets.UTF_8)
        );
        ReflectionTestUtils.setField(otherJwtService, "secret", otherSecret);
        ReflectionTestUtils.setField(otherJwtService, "expiration", EXPIRATION);
        String token = otherJwtService.generateToken("participant1@example.test", 5L);

        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }
}
