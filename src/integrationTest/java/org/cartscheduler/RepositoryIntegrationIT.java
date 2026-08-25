package org.cartscheduler;

import jakarta.persistence.EntityManager;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.ParticipantAccessToken;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.repository.ParticipantAccessTokenRepository;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class RepositoryIntegrationIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("cartscheduler_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl() + "?serverTimezone=UTC");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ParticipantAccessTokenRepository participantAccessTokenRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleDayRepository scheduleDayRepository;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @Test
    void shouldApplyInitialFlywayMigrationToIsolatedMySqlDatabase() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                Integer.class
        );
        Integer participantsTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'participants'",
                Integer.class
        );

        assertThat(migrationCount).isEqualTo(1);
        assertThat(participantsTableCount).isEqualTo(1);
    }

    @Test
    void shouldFindParticipantByEmail() {
        Participant participant = saveParticipant("alex@example.test");
        entityManager.flush();
        entityManager.clear();

        Participant result = participantRepository.findByEmail("alex@example.test").orElseThrow();

        assertThat(result.getId()).isEqualTo(participant.getId());
        assertThat(result.getName()).isEqualTo("Alex");
    }

    @Test
    void shouldFindOnlyParticipantsAssignedToAgentWithinRequestedSchedule() {
        Participant agent = saveParticipant("agent@example.test");
        Participant assignedInSchedule = saveParticipant("assigned@example.test");
        Participant assignedOutsideSchedule = saveParticipant("outside@example.test");
        Schedule schedule = saveSchedule("Main schedule", assignedInSchedule);

        agent.setAssignedParticipants(new ArrayList<>(List.of(assignedInSchedule, assignedOutsideSchedule)));
        participantRepository.saveAndFlush(agent);
        entityManager.clear();

        List<Participant> result = participantRepository.findAssignedParticipants(agent.getId(), schedule.getId());

        assertThat(result)
                .extracting(Participant::getId)
                .containsExactly(assignedInSchedule.getId());
    }

    @Test
    void shouldFindOnlySchedulesAccessibleToParticipant() {
        Participant participant = saveParticipant("participant@example.test");
        Participant anotherParticipant = saveParticipant("another@example.test");
        Schedule accessibleSchedule = saveSchedule("Accessible", participant);
        saveSchedule("Not accessible", anotherParticipant);
        entityManager.clear();

        List<Schedule> result = scheduleRepository.findForParticipant(participant.getId());

        assertThat(result)
                .extracting(Schedule::getId)
                .containsExactly(accessibleSchedule.getId());
    }

    @Test
    void shouldScopeScheduleDayQueriesToTheirSchedule() {
        Schedule firstSchedule = saveSchedule("First schedule");
        Schedule secondSchedule = saveSchedule("Second schedule");
        ScheduleDay firstDay = saveScheduleDay(firstSchedule, "Monday", (short) 1);
        ScheduleDay anotherFirstScheduleDay = saveScheduleDay(firstSchedule, "Tuesday", (short) 2);
        ScheduleDay secondScheduleDay = saveScheduleDay(secondSchedule, "Wednesday", (short) 3);
        entityManager.clear();

        List<ScheduleDay> days = scheduleDayRepository.findForSchedule(firstSchedule.getId());
        ScheduleDay matchingDay = scheduleDayRepository.findByIdForSchedule(firstDay.getId(), firstSchedule.getId());
        ScheduleDay foreignDay = scheduleDayRepository.findByIdForSchedule(secondScheduleDay.getId(), firstSchedule.getId());

        assertThat(days)
                .extracting(ScheduleDay::getId)
                .containsExactlyInAnyOrder(firstDay.getId(), anotherFirstScheduleDay.getId());
        assertThat(matchingDay.getId()).isEqualTo(firstDay.getId());
        assertThat(foreignDay).isNull();
    }

    @Test
    void shouldOrderScheduleEntriesByHourForScheduleDayDtoMapping() {
        Schedule schedule = saveSchedule("Main schedule");
        ScheduleDay day = saveScheduleDay(schedule, "Monday", (short) 1);
        saveScheduleEntry(day, "15:00", (short) 15);
        saveScheduleEntry(day, "07:00", (short) 7);
        entityManager.clear();

        ScheduleDay result = scheduleDayRepository.findByIdForSchedule(day.getId(), schedule.getId());

        assertThat(result.getScheduleEntries())
                .extracting(ScheduleEntry::getHour)
                .containsExactly((short) 7, (short) 15);
    }

    @Test
    void shouldReturnOnlyActiveInvitationTokenWithParticipantAndScheduleLoaded() {
        Participant participant = saveParticipant("participant@example.test");
        Schedule schedule = saveSchedule("Main schedule", participant);
        saveAccessToken("expired-token", participant, schedule, Instant.now().plusSeconds(60));
        ParticipantAccessToken activeToken = saveAccessToken(
                "active-token", participant, schedule, Instant.now().plusSeconds(60)
        );
        jdbcTemplate.update(
                "UPDATE participant_access_tokens SET expires_at = DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MINUTE) WHERE token = ?",
                "expired-token"
        );
        entityManager.clear();

        ParticipantAccessToken result = participantAccessTokenRepository.findAccessForToken("active-token");
        ParticipantAccessToken expiredResult = participantAccessTokenRepository.findAccessForToken("expired-token");

        assertThat(result.getId()).isEqualTo(activeToken.getId());
        assertThat(result.getParticipant().getId()).isEqualTo(participant.getId());
        assertThat(result.getSchedule().getId()).isEqualTo(schedule.getId());
        assertThat(Hibernate.isInitialized(result.getParticipant())).isTrue();
        assertThat(Hibernate.isInitialized(result.getSchedule())).isTrue();
        assertThat(expiredResult).isNull();
    }

    private Participant saveParticipant(String email) {
        Participant participant = new Participant();
        participant.setName("Alex");
        participant.setEmail(email);
        return participantRepository.saveAndFlush(participant);
    }

    private Schedule saveSchedule(String name, Participant... accessibleParticipants) {
        Schedule schedule = new Schedule();
        schedule.setName(name);
        schedule.setAccessibleParticipants(List.of(accessibleParticipants));
        return scheduleRepository.saveAndFlush(schedule);
    }

    private ScheduleDay saveScheduleDay(Schedule schedule, String name, short dayOfWeek) {
        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setSchedule(schedule);
        scheduleDay.setName(name);
        scheduleDay.setDayOfWeek(dayOfWeek);
        return scheduleDayRepository.saveAndFlush(scheduleDay);
    }

    private void saveScheduleEntry(ScheduleDay scheduleDay, String name, short hour) {
        ScheduleEntry entry = new ScheduleEntry();
        entry.setScheduleDay(scheduleDay);
        entry.setName(name);
        entry.setHour(hour);
        scheduleEntryRepository.saveAndFlush(entry);
    }

    private ParticipantAccessToken saveAccessToken(
            String token,
            Participant participant,
            Schedule schedule,
            Instant expiresAt
    ) {
        ParticipantAccessToken accessToken = new ParticipantAccessToken();
        accessToken.setToken(token);
        accessToken.setParticipant(participant);
        accessToken.setSchedule(schedule);
        accessToken.setCreatedAt(Date.from(Instant.now()));
        accessToken.setExpiresAt(Date.from(expiresAt));
        return participantAccessTokenRepository.saveAndFlush(accessToken);
    }
}
