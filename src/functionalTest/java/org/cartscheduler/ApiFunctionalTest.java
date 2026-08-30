package org.cartscheduler;

import org.cartscheduler.dto.rest.request.RestAuthRequest;
import org.cartscheduler.dto.rest.request.CreateProposalRequest;
import org.cartscheduler.dto.rest.response.ParticipantDto;
import org.cartscheduler.dto.rest.response.ProposalDto;
import org.cartscheduler.dto.rest.response.RestAuthResponse;
import org.cartscheduler.dto.rest.response.ScheduleDayDto;
import org.cartscheduler.dto.rest.response.ScheduleDto;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.ParticipantAccessToken;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.repository.ParticipantAccessTokenRepository;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.cartscheduler.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiFunctionalTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("cartscheduler_functional_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl() + "?serverTimezone=UTC");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ParticipantAccessTokenRepository participantAccessTokenRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleDayRepository scheduleDayRepository;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM proposals");
        jdbcTemplate.update("DELETE FROM participant_access_tokens");
        jdbcTemplate.update("DELETE FROM schedule_entries");
        jdbcTemplate.update("DELETE FROM schedule_days");
        jdbcTemplate.update("DELETE FROM schedule_access");
        jdbcTemplate.update("DELETE FROM participant_agents");
        jdbcTemplate.update("DELETE FROM schedules");
        jdbcTemplate.update("DELETE FROM participants");
    }

    @Test
    void shouldAuthenticateAndServeScheduleDaysAndAssignedParticipantsThroughHttpApi() {
        Fixture fixture = seedFixture();

        ResponseEntity<RestAuthResponse> authentication = restTemplate.postForEntity(
                "/api/auth/",
                new RestAuthRequest(fixture.invitationToken()),
                RestAuthResponse.class
        );

        assertThat(authentication.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authentication.getBody()).isNotNull();
        assertThat(authentication.getBody().getScheduleId()).isEqualTo(fixture.schedule().getId());
        assertThat(jwtService.extractUsername(authentication.getBody().getJwtToken()))
                .isEqualTo(fixture.agent().getEmail());
        Long tokenScheduleId = jwtService.extractClaim(
                authentication.getBody().getJwtToken(),
                claims -> claims.get("schedule_id", Long.class)
        );
        assertThat(tokenScheduleId).isEqualTo(fixture.schedule().getId());

        HttpEntity<Void> authorizedRequest = authorizedRequest(authentication.getBody().getJwtToken());
        ResponseEntity<ScheduleDto> scheduleResponse = restTemplate.exchange(
                "/api/schedules/{scheduleId}/",
                HttpMethod.GET,
                authorizedRequest,
                ScheduleDto.class,
                fixture.schedule().getId()
        );
        ResponseEntity<ScheduleDayDto[]> daysResponse = restTemplate.exchange(
                "/api/schedules/{scheduleId}/days/",
                HttpMethod.GET,
                authorizedRequest,
                ScheduleDayDto[].class,
                fixture.schedule().getId()
        );
        ResponseEntity<ScheduleDayDto> dayResponse = restTemplate.exchange(
                "/api/schedules/{scheduleId}/days/{dayId}/",
                HttpMethod.GET,
                authorizedRequest,
                ScheduleDayDto.class,
                fixture.schedule().getId(),
                fixture.scheduleDay().getId()
        );
        ResponseEntity<ParticipantDto[]> participantsResponse = restTemplate.exchange(
                "/api/participants/",
                HttpMethod.GET,
                authorizedRequest,
                ParticipantDto[].class
        );

        assertThat(scheduleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(scheduleResponse.getBody()).isNotNull();
        assertThat(scheduleResponse.getBody().getId()).isEqualTo(fixture.schedule().getId());
        assertThat(scheduleResponse.getBody().getName()).isEqualTo("Main schedule");

        assertThat(daysResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(daysResponse.getBody()).isNotNull();
        assertThat(daysResponse.getBody())
                .extracting(ScheduleDayDto::getId, ScheduleDayDto::getName,
                        ScheduleDayDto::getFirstHour, ScheduleDayDto::getLastHour)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        fixture.scheduleDay().getId(), "Monday", 7, 17
                ));

        assertThat(dayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dayResponse.getBody()).isNotNull();
        assertThat(dayResponse.getBody().getId()).isEqualTo(fixture.scheduleDay().getId());
        assertThat(dayResponse.getBody().getFirstHour()).isEqualTo(7);
        assertThat(dayResponse.getBody().getLastHour()).isEqualTo(17);

        assertThat(participantsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(participantsResponse.getBody()).isNotNull();
        assertThat(participantsResponse.getBody())
                .extracting(ParticipantDto::getId, ParticipantDto::getName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        fixture.assignedParticipant().getId(), fixture.assignedParticipant().getName()
                ));
    }

    @Test
    void shouldRejectExpiredInvitationAndMissingOrMalformedJwt() {
        Fixture fixture = seedFixture();
        saveAccessToken("expired-invitation", fixture.agent(), fixture.schedule(), Instant.now().plusSeconds(600));
        jdbcTemplate.update(
                "UPDATE participant_access_tokens SET expires_at = DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MINUTE) WHERE token = ?",
                "expired-invitation"
        );

        ResponseEntity<String> expiredAuthentication = restTemplate.postForEntity(
                "/api/auth/",
                new RestAuthRequest("expired-invitation"),
                String.class
        );
        ResponseEntity<String> anonymousRequest = restTemplate.getForEntity(
                "/api/schedules/{scheduleId}/",
                String.class,
                fixture.schedule().getId()
        );
        ResponseEntity<String> malformedJwtRequest = restTemplate.exchange(
                "/api/schedules/{scheduleId}/",
                HttpMethod.GET,
                authorizedRequest("not-a-jwt"),
                String.class,
                fixture.schedule().getId()
        );
        ResponseEntity<String> anonymousProposalCreation = restTemplate.postForEntity(
                "/api/proposals",
                new CreateProposalRequest(fixture.assignedParticipant().getId(), fixture.scheduleDay().getId(), 7, 15, 4, 6, null, null),
                String.class
        );

        assertThat(expiredAuthentication.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(anonymousRequest.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(malformedJwtRequest.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(anonymousProposalCreation.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldForbidAuthenticatedParticipantWhoIsNotAgentOfScheduledParticipant() {
        Fixture fixture = seedFixture();
        saveAccessToken(
                "direct-participant-invitation",
                fixture.directlyScheduledParticipant(),
                fixture.schedule(),
                Instant.now().plusSeconds(600)
        );
        String directParticipantToken = authenticate("direct-participant-invitation").getJwtToken();

        ResponseEntity<String> scheduleResponse = restTemplate.exchange(
                "/api/schedules/{scheduleId}/",
                HttpMethod.GET,
                authorizedRequest(directParticipantToken),
                String.class,
                fixture.schedule().getId()
        );
        ResponseEntity<String> daysResponse = restTemplate.exchange(
                "/api/schedules/{scheduleId}/days/",
                HttpMethod.GET,
                authorizedRequest(directParticipantToken),
                String.class,
                fixture.schedule().getId()
        );
        ResponseEntity<String> dayResponse = restTemplate.exchange(
                "/api/schedules/{scheduleId}/days/{dayId}/",
                HttpMethod.GET,
                authorizedRequest(directParticipantToken),
                String.class,
                fixture.schedule().getId(),
                fixture.scheduleDay().getId()
        );

        assertThat(scheduleResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(daysResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(dayResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnProposalsAssignedToParticipantAndNotImplementedForDeletion() {
        Fixture fixture = seedFixture();
        Proposal expectedProposal = saveProposal(
                fixture.assignedParticipant(),
                fixture.agent(),
                fixture.scheduleDay(),
                (short) 8,
                (short) 12
        );
        saveProposal(
                fixture.directlyScheduledParticipant(),
                fixture.agent(),
                fixture.scheduleDay(),
                (short) 13,
                (short) 15
        );
        String jwt = authenticate(fixture.invitationToken()).getJwtToken();

        ResponseEntity<ProposalDto[]> listResponse = restTemplate.exchange(
                "/api/proposals/schedule-day/{scheduleDayId}/participant/{participantId}",
                HttpMethod.GET,
                authorizedRequest(jwt),
                ProposalDto[].class,
                fixture.scheduleDay().getId(),
                fixture.assignedParticipant().getId()
        );
        ResponseEntity<String> forbiddenListResponse = restTemplate.exchange(
                "/api/proposals/schedule-day/{scheduleDayId}/participant/{participantId}",
                HttpMethod.GET,
                authorizedRequest(jwt),
                String.class,
                fixture.scheduleDay().getId(),
                fixture.directlyScheduledParticipant().getId()
        );
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/proposals/{proposalId}",
                HttpMethod.DELETE,
                authorizedRequest(jwt),
                String.class,
                999L
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();
        assertThat(listResponse.getBody())
                .extracting(
                        ProposalDto::getId,
                        ProposalDto::getParticipantId,
                        ProposalDto::getInsertingParticipantId,
                        ProposalDto::getScheduleDayId,
                        ProposalDto::getHourStart,
                        ProposalDto::getHourEnd
                )
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        expectedProposal.getId(),
                        fixture.assignedParticipant().getId(),
                        fixture.agent().getId(),
                        fixture.scheduleDay().getId(),
                        8,
                        12
                ));
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(forbiddenListResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldCreateProposalThroughHttpApiWithAuthenticatedParticipantAsInserter() {
        Fixture fixture = seedFixture();
        String jwt = authenticate(fixture.invitationToken()).getJwtToken();
        CreateProposalRequest request = new CreateProposalRequest(
                fixture.assignedParticipant().getId(),
                fixture.scheduleDay().getId(),
                7,
                15,
                4,
                6,
                1,
                2
        );

        ResponseEntity<ProposalDto> response = restTemplate.exchange(
                "/api/proposals",
                HttpMethod.POST,
                authorizedRequest(jwt, request),
                ProposalDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody())
                .extracting(
                        ProposalDto::getParticipantId,
                        ProposalDto::getInsertingParticipantId,
                        ProposalDto::getPairParticipantId,
                        ProposalDto::getScheduleDayId,
                        ProposalDto::getHourStart,
                        ProposalDto::getHourEnd,
                        ProposalDto::getServiceLengthStart,
                        ProposalDto::getServiceLengthEnd,
                        ProposalDto::getBreakLengthStart,
                        ProposalDto::getBreakLengthEnd
                )
                .containsExactly(
                        fixture.assignedParticipant().getId(),
                        fixture.agent().getId(),
                        null,
                        fixture.scheduleDay().getId(),
                        7,
                        15,
                        (short) 4,
                        (short) 6,
                        (short) 1,
                        (short) 2
                );
        assertThat(response.getBody().getId()).isPositive();
        assertThat(response.getBody().getDateAdd()).isNotNull();

        Proposal savedProposal = proposalRepository.findById(response.getBody().getId()).orElseThrow();
        assertThat(savedProposal.getDateAdd()).isNotNull();
        assertThat(savedProposal.getParticipant().getId()).isEqualTo(fixture.assignedParticipant().getId());
        assertThat(savedProposal.getInsertingParticipant().getId()).isEqualTo(fixture.agent().getId());
        assertThat(savedProposal.getPairParticipant()).isNull();
    }

    @Test
    void shouldCreateProposalForSingleScheduleHourThroughHttpApi() {
        Fixture fixture = seedFixture();
        String jwt = authenticate(fixture.invitationToken()).getJwtToken();
        CreateProposalRequest request = new CreateProposalRequest(
                fixture.assignedParticipant().getId(),
                fixture.scheduleDay().getId(),
                17,
                17,
                1,
                1,
                null,
                null
        );

        ResponseEntity<ProposalDto> response = restTemplate.exchange(
                "/api/proposals",
                HttpMethod.POST,
                authorizedRequest(jwt, request),
                ProposalDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody())
                .extracting(
                        ProposalDto::getHourStart,
                        ProposalDto::getHourEnd,
                        ProposalDto::getServiceLengthStart,
                        ProposalDto::getServiceLengthEnd,
                        ProposalDto::getBreakLengthStart,
                        ProposalDto::getBreakLengthEnd
                )
                .containsExactly(17, 17, (short) 1, (short) 1, null, null);
    }

    @Test
    void shouldForbidProposalCreationForParticipantNotAssignedToAuthenticatedAgent() {
        Fixture fixture = seedFixture();
        String jwt = authenticate(fixture.invitationToken()).getJwtToken();
        CreateProposalRequest request = new CreateProposalRequest(
                fixture.directlyScheduledParticipant().getId(),
                fixture.scheduleDay().getId(),
                7,
                15,
                4,
                6,
                null,
                null
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/proposals",
                HttpMethod.POST,
                authorizedRequest(jwt, request),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(proposalRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectProposalWithHoursOutsideScheduleEntries() {
        Fixture fixture = seedFixture();
        String jwt = authenticate(fixture.invitationToken()).getJwtToken();
        CreateProposalRequest request = new CreateProposalRequest(
                fixture.assignedParticipant().getId(),
                fixture.scheduleDay().getId(),
                8,
                15,
                4,
                6,
                null,
                null
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/proposals",
                HttpMethod.POST,
                authorizedRequest(jwt, request),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(proposalRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectProposalWhenRequiredFieldsAreMissing() {
        Fixture fixture = seedFixture();
        String jwt = authenticate(fixture.invitationToken()).getJwtToken();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/proposals",
                HttpMethod.POST,
                authorizedRequest(jwt, new CreateProposalRequest()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(proposalRepository.findAll()).isEmpty();
    }

    private Fixture seedFixture() {
        Participant agent = saveParticipant("Agent", "agent@example.test");
        Participant assignedParticipant = saveParticipant("Assigned", "assigned@example.test");
        Participant directlyScheduledParticipant = saveParticipant("Directly scheduled", "direct@example.test");
        Schedule schedule = saveSchedule("Main schedule", assignedParticipant, directlyScheduledParticipant);

        agent.setAssignedParticipants(new ArrayList<>(List.of(assignedParticipant)));
        participantRepository.saveAndFlush(agent);

        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setSchedule(schedule);
        scheduleDay.setDayOfWeek((short) 1);
        scheduleDay.setName("Monday");
        scheduleDay = scheduleDayRepository.saveAndFlush(scheduleDay);
        saveScheduleEntry(scheduleDay, "17:00", (short) 17);
        saveScheduleEntry(scheduleDay, "15:00", (short) 15);
        saveScheduleEntry(scheduleDay, "07:00", (short) 7);

        String invitationToken = "valid-invitation";
        saveAccessToken(invitationToken, agent, schedule, Instant.now().plusSeconds(600));

        return new Fixture(agent, assignedParticipant, directlyScheduledParticipant, schedule, scheduleDay, invitationToken);
    }

    private Participant saveParticipant(String name, String email) {
        Participant participant = new Participant();
        participant.setName(name);
        participant.setEmail(email);
        return participantRepository.saveAndFlush(participant);
    }

    private Schedule saveSchedule(String name, Participant... accessibleParticipants) {
        Schedule schedule = new Schedule();
        schedule.setName(name);
        schedule.setAccessibleParticipants(List.of(accessibleParticipants));
        return scheduleRepository.saveAndFlush(schedule);
    }

    private void saveScheduleEntry(ScheduleDay scheduleDay, String name, short hour) {
        ScheduleEntry entry = new ScheduleEntry();
        entry.setScheduleDay(scheduleDay);
        entry.setName(name);
        entry.setHour(hour);
        scheduleEntryRepository.saveAndFlush(entry);
    }

    private Proposal saveProposal(
            Participant participant,
            Participant insertingParticipant,
            ScheduleDay scheduleDay,
            short hourStart,
            short hourEnd
    ) {
        Proposal proposal = new Proposal();
        proposal.setDateAdd(Date.from(Instant.now()));
        proposal.setParticipant(participant);
        proposal.setInsertingParticipant(insertingParticipant);
        proposal.setScheduleDay(scheduleDay);
        proposal.setHourStart(hourStart);
        proposal.setHourEnd(hourEnd);
        return proposalRepository.saveAndFlush(proposal);
    }

    private void saveAccessToken(String token, Participant participant, Schedule schedule, Instant expiresAt) {
        ParticipantAccessToken accessToken = new ParticipantAccessToken();
        accessToken.setToken(token);
        accessToken.setParticipant(participant);
        accessToken.setSchedule(schedule);
        accessToken.setCreatedAt(Date.from(Instant.now()));
        accessToken.setExpiresAt(Date.from(expiresAt));
        participantAccessTokenRepository.saveAndFlush(accessToken);
    }

    private RestAuthResponse authenticate(String invitationToken) {
        ResponseEntity<RestAuthResponse> response = restTemplate.postForEntity(
                "/api/auth/",
                new RestAuthRequest(invitationToken),
                RestAuthResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private HttpEntity<Void> authorizedRequest(String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> authorizedRequest(String jwt, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        return new HttpEntity<>(body, headers);
    }

    private record Fixture(
            Participant agent,
            Participant assignedParticipant,
            Participant directlyScheduledParticipant,
            Schedule schedule,
            ScheduleDay scheduleDay,
            String invitationToken
    ) {
    }
}
