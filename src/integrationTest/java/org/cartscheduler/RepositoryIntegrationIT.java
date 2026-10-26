package org.cartscheduler;

import jakarta.persistence.EntityManager;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.ParticipantAccessToken;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.ConfigGeneration;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.repository.ParticipantAccessTokenRepository;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ConfigGenerationRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.cartscheduler.service.generation.ScheduleGenerationResult;
import org.cartscheduler.service.generation.ScheduleGenerationService;
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
    private ProposalRepository proposalRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleDayRepository scheduleDayRepository;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @Autowired
    private ConfigGenerationRepository configGenerationRepository;

    @Autowired
    private ScheduleGenerationService scheduleGenerationService;

    @Test
    void shouldApplyFlywayMigrationsToIsolatedMySqlDatabase() {
        Integer initialMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                Integer.class
        );
        Integer pairAndBreakMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '3' AND success = true",
                Integer.class
        );
        Integer alternativeGroupMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '4' AND success = true",
                Integer.class
        );
        Integer generationConfigurationMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '5' AND success = true",
                Integer.class
        );
        Integer calendarConfigurationMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '6' AND success = true",
                Integer.class
        );
        Integer participantAutomationMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '7' AND success = true",
                Integer.class
        );
        Integer scheduleDateMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '8' AND success = true",
                Integer.class
        );
        Integer proposalStatusIndexMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '9' AND success = true",
                Integer.class
        );
        Integer participantsTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'participants'",
                Integer.class
        );

        Integer allowPairSelectionColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'participants' " +
                        "AND column_name = 'allow_pair_selection'",
                Integer.class
        );
        Integer breakModeColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'proposals' " +
                        "AND column_name = 'break_mode'",
                Integer.class
        );
        Integer allowAlternativeGroupSelectionColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'participants' " +
                        "AND column_name = 'allow_alternative_group_selection'",
                Integer.class
        );
        Integer alternativeGroupColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'proposals' " +
                        "AND column_name = 'alternative_group'",
                Integer.class
        );
        Integer participantEnabledColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'participants' " +
                        "AND column_name = 'is_enabled'",
                Integer.class
        );
        Integer participantAutoassignmentColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'participants' " +
                        "AND column_name = 'new_schedule_autoassignment_enabled'",
                Integer.class
        );
        Integer participantAutoinvitementColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'participants' " +
                        "AND column_name = 'new_schedule_autoinvitement_enabled'",
                Integer.class
        );
        Integer scheduleDateColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'schedule_days' " +
                        "AND column_name = 'schedule_date'",
                Integer.class
        );
        Integer proposalStatusIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() AND table_name = 'proposals' " +
                        "AND index_name = 'idx_proposals_schedule_day_participant'",
                Integer.class
        );
        Integer generationConfigurationTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'config_generation'",
                Integer.class
        );
        Integer participantRulesTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'config_participant_rules'",
                Integer.class
        );
        Integer candidateAvailabilityScoresTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'config_candidate_availability_scores'",
                Integer.class
        );
        Integer scheduleDaysConfigurationTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'config_schedule_days'",
                Integer.class
        );
        Integer restrictedDatesConfigurationTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'config_schedule_restricted_dates'",
                Integer.class
        );
        Integer obsoleteRowScoreTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'config_row_full_or_empty_scores'",
                Integer.class
        );
        Integer globalConfigurationRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM config_generation WHERE id = 1",
                Integer.class
        );

        assertThat(initialMigrationCount).isEqualTo(1);
        assertThat(pairAndBreakMigrationCount).isEqualTo(1);
        assertThat(alternativeGroupMigrationCount).isEqualTo(1);
        assertThat(generationConfigurationMigrationCount).isEqualTo(1);
        assertThat(calendarConfigurationMigrationCount).isEqualTo(1);
        assertThat(participantAutomationMigrationCount).isEqualTo(1);
        assertThat(scheduleDateMigrationCount).isEqualTo(1);
        assertThat(proposalStatusIndexMigrationCount).isEqualTo(1);
        assertThat(participantsTableCount).isEqualTo(1);
        assertThat(allowPairSelectionColumnCount).isEqualTo(1);
        assertThat(breakModeColumnCount).isEqualTo(1);
        assertThat(allowAlternativeGroupSelectionColumnCount).isEqualTo(1);
        assertThat(alternativeGroupColumnCount).isEqualTo(1);
        assertThat(participantEnabledColumnCount).isEqualTo(1);
        assertThat(participantAutoassignmentColumnCount).isEqualTo(1);
        assertThat(participantAutoinvitementColumnCount).isEqualTo(1);
        assertThat(scheduleDateColumnCount).isEqualTo(1);
        assertThat(proposalStatusIndexCount).isEqualTo(2);
        assertThat(generationConfigurationTableCount).isEqualTo(1);
        assertThat(participantRulesTableCount).isEqualTo(1);
        assertThat(candidateAvailabilityScoresTableCount).isEqualTo(1);
        assertThat(scheduleDaysConfigurationTableCount).isEqualTo(1);
        assertThat(restrictedDatesConfigurationTableCount).isEqualTo(1);
        assertThat(obsoleteRowScoreTableCount).isZero();
        assertThat(globalConfigurationRowCount).isEqualTo(1);
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
        long assignedParticipantCount = participantRepository.countAssignedParticipantsForAgentAndSchedule(
                agent.getId(),
                schedule.getId()
        );
        long targetParticipantCount = participantRepository.countAssignedParticipantForAgentAndSchedule(
                agent.getId(),
                assignedInSchedule.getId(),
                schedule.getId()
        );
        long outsideParticipantCount = participantRepository.countAssignedParticipantForAgentAndSchedule(
                agent.getId(),
                assignedOutsideSchedule.getId(),
                schedule.getId()
        );

        assertThat(result)
                .extracting(Participant::getId)
                .containsExactly(assignedInSchedule.getId());
        assertThat(assignedParticipantCount).isEqualTo(1L);
        assertThat(targetParticipantCount).isEqualTo(1L);
        assertThat(outsideParticipantCount).isZero();
    }

    @Test
    void shouldFindSchedulesReachableByAgentThroughAssignedParticipants() {
        Participant agent = saveParticipant("agent@example.test");
        Participant assignedParticipant = saveParticipant("assigned@example.test");
        Participant unrelatedParticipant = saveParticipant("unrelated@example.test");
        Schedule reachableSchedule = saveSchedule("Reachable", assignedParticipant);
        saveSchedule("Not reachable", unrelatedParticipant);

        agent.setAssignedParticipants(new ArrayList<>(List.of(assignedParticipant)));
        participantRepository.saveAndFlush(agent);
        entityManager.clear();

        List<Schedule> result = scheduleRepository.findForAgent(agent.getId());

        assertThat(result)
                .extracting(Schedule::getId)
                .containsExactly(reachableSchedule.getId());
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
    void shouldFindOnlyProposalsAssignedToParticipantForRequestedScheduleDay() {
        Participant author = saveParticipant("author@example.test");
        Participant targetParticipant = saveParticipant("target@example.test");
        Participant anotherTargetParticipant = saveParticipant("another-target@example.test");
        Schedule schedule = saveSchedule("Main schedule", author, targetParticipant);
        ScheduleDay requestedDay = saveScheduleDay(schedule, "Monday", (short) 1);
        ScheduleDay anotherDay = saveScheduleDay(schedule, "Tuesday", (short) 2);
        Proposal firstProposal = saveProposal(targetParticipant, author, requestedDay, (short) 8, (short) 10);
        Proposal secondProposal = saveProposal(targetParticipant, author, requestedDay, (short) 12, (short) 14);
        saveProposal(anotherTargetParticipant, author, requestedDay, (short) 9, (short) 11);
        saveProposal(targetParticipant, author, anotherDay, (short) 15, (short) 17);
        entityManager.clear();

        List<Proposal> result = proposalRepository.findForParticipantAndScheduleDay(
                targetParticipant.getId(),
                requestedDay.getId()
        );

        assertThat(result)
                .extracting(Proposal::getId)
                .containsExactly(firstProposal.getId(), secondProposal.getId());
    }

    @Test
    void shouldFindProposalStatusIdsOnlyForOwnProposalsInRequestedSchedule() {
        Participant author = saveParticipant("author@example.test");
        Participant participantWithProposal = saveParticipant("with-proposal@example.test");
        Participant participantWithOnlyOtherScheduleProposal = saveParticipant("other-schedule@example.test");
        Participant participantSelectedOnlyAsPair = saveParticipant("pair-only@example.test");
        Participant anotherParticipant = saveParticipant("another@example.test");
        Schedule requestedSchedule = saveSchedule("Requested schedule", participantWithProposal);
        Schedule anotherSchedule = saveSchedule("Another schedule", participantWithOnlyOtherScheduleProposal);
        ScheduleDay firstRequestedDay = saveScheduleDay(requestedSchedule, "Monday", (short) 1);
        ScheduleDay secondRequestedDay = saveScheduleDay(requestedSchedule, "Tuesday", (short) 2);
        ScheduleDay otherScheduleDay = saveScheduleDay(anotherSchedule, "Wednesday", (short) 3);

        Proposal ownProposal = saveProposal(participantWithProposal, author, firstRequestedDay, (short) 8, (short) 10);
        ownProposal.setPairParticipant(participantSelectedOnlyAsPair);
        proposalRepository.saveAndFlush(ownProposal);
        saveProposal(anotherParticipant, author, secondRequestedDay, (short) 8, (short) 10);
        saveProposal(
                participantWithOnlyOtherScheduleProposal,
                author,
                otherScheduleDay,
                (short) 8,
                (short) 10
        );
        entityManager.clear();

        List<Long> participantIdsWithProposals = proposalRepository.findParticipantIdsWithProposalsForSchedule(
                requestedSchedule.getId(),
                List.of(
                        participantWithProposal.getId(),
                        participantWithOnlyOtherScheduleProposal.getId(),
                        participantSelectedOnlyAsPair.getId()
                )
        );
        List<Long> proposalDayIds = proposalRepository.findScheduleDayIdsWithProposalsForParticipantAndSchedule(
                participantWithProposal.getId(),
                requestedSchedule.getId()
        );
        List<Long> pairOnlyProposalDayIds = proposalRepository
                .findScheduleDayIdsWithProposalsForParticipantAndSchedule(
                        participantSelectedOnlyAsPair.getId(),
                        requestedSchedule.getId()
                );

        assertThat(participantIdsWithProposals).containsExactly(participantWithProposal.getId());
        assertThat(proposalDayIds).containsExactly(firstRequestedDay.getId());
        assertThat(pairOnlyProposalDayIds).isEmpty();
    }

    @Test
    void shouldGenerateAndPersistParticipantsFromProposals() {
        Participant firstParticipant = saveParticipant("first-generator@example.test");
        Participant secondParticipant = saveParticipant("second-generator@example.test");
        Schedule schedule = saveSchedule("Generator schedule", firstParticipant, secondParticipant);
        ScheduleDay day = saveScheduleDay(schedule, "Monday", (short) 1);
        ScheduleEntry entry = saveScheduleEntry(day, "08:00", (short) 8);
        saveProposal(firstParticipant, firstParticipant, day, (short) 8, (short) 8);
        saveProposal(secondParticipant, secondParticipant, day, (short) 8, (short) 8);
        ConfigGeneration generationConfiguration = configGenerationRepository
                .findById(ConfigGeneration.SINGLETON_ID)
                .orElseThrow();
        generationConfiguration.setEmptyRowPoints(0);
        generationConfiguration.setFullRowAdjacentToIncompletePoints(10);
        generationConfiguration.setFullRowNotAdjacentToIncompletePoints(10);
        configGenerationRepository.saveAndFlush(generationConfiguration);
        entityManager.clear();

        ScheduleGenerationResult result = scheduleGenerationService.generate(schedule.getId());
        entityManager.flush();
        entityManager.clear();

        ScheduleEntry persistedEntry = scheduleEntryRepository.findById(entry.getId()).orElseThrow();
        assertThat(result.assignedParticipantCells()).isEqualTo(2);
        assertThat(persistedEntry.getParticipant1().getId()).isEqualTo(firstParticipant.getId());
        assertThat(persistedEntry.getParticipant2().getId()).isEqualTo(secondParticipant.getId());
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

    private ScheduleEntry saveScheduleEntry(ScheduleDay scheduleDay, String name, short hour) {
        ScheduleEntry entry = new ScheduleEntry();
        entry.setScheduleDay(scheduleDay);
        entry.setName(name);
        entry.setHour(hour);
        return scheduleEntryRepository.saveAndFlush(entry);
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
