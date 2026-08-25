package org.cartscheduler.service;

import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cartscheduler.support.TestEntityFactory.participant;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RestUserInfoServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private RestUserInfoService restUserInfoService;

    @Test
    void shouldLoadParticipantAsRestUserDetailsByEmail() {
        given(participantRepository.findByEmail("participant1@example.test"))
                .willReturn(Optional.of(participant(1L)));

        RestUserDetails result = (RestUserDetails) restUserInfoService.loadUserByUsername("participant1@example.test");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("participant1@example.test");
        assertThat(result.getAuthorities()).isEmpty();
        assertThat(result.getScheduleId()).isNull();
    }

    @Test
    void shouldThrowWhenParticipantEmailDoesNotExist() {
        given(participantRepository.findByEmail("missing@example.test")).willReturn(Optional.empty());

        assertThatThrownBy(() -> restUserInfoService.loadUserByUsername("missing@example.test"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: missing@example.test");
    }
}
