package org.cartscheduler.controller.rest;

import org.cartscheduler.dto.rest.request.RestAuthRequest;
import org.cartscheduler.entity.ParticipantAccessToken;
import org.cartscheduler.repository.ParticipantAccessTokenRepository;
import org.cartscheduler.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cartscheduler.support.TestEntityFactory.participant;
import static org.cartscheduler.support.TestEntityFactory.schedule;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private ParticipantAccessTokenRepository participantAccessTokenRepository;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldReturnJwtAndScheduleForValidInvitationToken() {
        ParticipantAccessToken accessToken = new ParticipantAccessToken();
        accessToken.setToken("valid-invitation");
        accessToken.setParticipant(participant(1L));
        accessToken.setSchedule(schedule(5L, "Main schedule", java.util.List.of()));
        given(participantAccessTokenRepository.findAccessForToken("valid-invitation")).willReturn(accessToken);
        given(jwtService.generateToken("participant1@example.test", 5L)).willReturn("jwt-token");
        given(jwtService.getExpirationTime()).willReturn(123_456L);

        var response = authController.authenticateAndGetToken(new RestAuthRequest("valid-invitation"));

        assertThat(response.getJwtToken()).isEqualTo("jwt-token");
        assertThat(response.getScheduleId()).isEqualTo(5L);
        assertThat(response.getExpiresAt()).isEqualTo(123_456L);
    }

    @Test
    void shouldRejectInvalidOrExpiredInvitationToken() {
        given(participantAccessTokenRepository.findAccessForToken("invalid-invitation")).willReturn(null);

        assertThatThrownBy(() -> authController.authenticateAndGetToken(new RestAuthRequest("invalid-invitation")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
