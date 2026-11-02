package org.cartscheduler.service.generation;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.cartscheduler.entity.ConfigScheduleDay;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.ParticipantAccessToken;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleAccess;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.repository.ConfigScheduleDayRepository;
import org.cartscheduler.repository.ConfigScheduleRestrictedDateRepository;
import org.cartscheduler.repository.ParticipantAccessTokenRepository;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ScheduleAccessRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a calendar schedule from the configured weekday hours and restricted
 * dates. A generated schedule always starts on Monday and ends on Sunday.
 */
@Service
public class ScheduleCalendarGenerationService {
    private static final Locale POLISH_LOCALE = Locale.forLanguageTag("pl-PL");
    private static final int INVITATION_TOKEN_BYTE_LENGTH = 32;
    private static final SecureRandom INVITATION_TOKEN_RANDOM = new SecureRandom();

    private final ScheduleRepository scheduleRepository;
    private final ScheduleDayRepository scheduleDayRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final ConfigScheduleDayRepository configScheduleDayRepository;
    private final ConfigScheduleRestrictedDateRepository restrictedDateRepository;
    private final ParticipantRepository participantRepository;
    private final ScheduleAccessRepository scheduleAccessRepository;
    private final ParticipantAccessTokenRepository participantAccessTokenRepository;

    @Value("${new-schedule-autoinvitement.expiration:P7D}")
    private Duration autoinvitementExpiration = Duration.ofDays(7);

    public ScheduleCalendarGenerationService(
            ScheduleRepository scheduleRepository,
            ScheduleDayRepository scheduleDayRepository,
            ScheduleEntryRepository scheduleEntryRepository,
            ConfigScheduleDayRepository configScheduleDayRepository,
            ConfigScheduleRestrictedDateRepository restrictedDateRepository,
            ParticipantRepository participantRepository,
            ScheduleAccessRepository scheduleAccessRepository,
            ParticipantAccessTokenRepository participantAccessTokenRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleDayRepository = scheduleDayRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.configScheduleDayRepository = configScheduleDayRepository;
        this.restrictedDateRepository = restrictedDateRepository;
        this.participantRepository = participantRepository;
        this.scheduleAccessRepository = scheduleAccessRepository;
        this.participantAccessTokenRepository = participantAccessTokenRepository;
    }

    /** Generates the schedule for the closest month whose first full week has not passed. */
    @Transactional
    public ScheduleCalendarGenerationResult generate() {
        return generate(LocalDate.now());
    }

    /**
     * Generates a schedule using {@code generationDate} to choose the target
     * month. Keeping the date explicit makes backfills and deterministic tests
     * possible without changing application time.
     */
    @Transactional
    public ScheduleCalendarGenerationResult generate(LocalDate generationDate) {
        Objects.requireNonNull(generationDate, "generationDate must not be null");

        SchedulePeriod period = periodFor(generationDate);
        String scheduleName = scheduleName(period.month());
        if (scheduleRepository.existsByName(scheduleName)) {
            return new ScheduleCalendarGenerationResult(
                    false,
                    scheduleName,
                    period.startDate(),
                    period.endDate(),
                    0,
                    0
            );
        }

        Map<Short, ConfigScheduleDay> activeDaysByWeekday = activeDaysByWeekday();
        if (activeDaysByWeekday.isEmpty()) {
            throw new IllegalStateException("At least one active config_schedule_days row is required");
        }
        Set<LocalDate> restrictedDates = restrictedDates(period);

        Schedule schedule = new Schedule();
        schedule.setName(scheduleName);
        schedule = scheduleRepository.save(schedule);
        addConfiguredParticipantAccessAndInvitations(schedule);

        List<ScheduleDay> scheduleDays = new ArrayList<>();
        List<ScheduleEntry> scheduleEntries = new ArrayList<>();
        for (LocalDate date = period.startDate(); !date.isAfter(period.endDate()); date = date.plusDays(1)) {
            ConfigScheduleDay configDay = activeDaysByWeekday.get((short) date.getDayOfWeek().getValue());
            if (configDay == null || restrictedDates.contains(date)) {
                continue;
            }

            ScheduleDay scheduleDay = new ScheduleDay();
            scheduleDay.setSchedule(schedule);
            scheduleDay.setDayOfWeek((short) date.getDayOfWeek().getValue());
            scheduleDay.setScheduleDate(date);
            scheduleDay.setName(scheduleDayName(date));
            scheduleDays.add(scheduleDay);
        }
        schedule.setScheduleDays(scheduleDays);
        scheduleDayRepository.saveAll(scheduleDays);

        for (ScheduleDay scheduleDay : scheduleDays) {
            ConfigScheduleDay configDay = activeDaysByWeekday.get(scheduleDay.getDayOfWeek());
            List<ScheduleEntry> entriesForDay = new ArrayList<>();
            for (short hour = configDay.getHourFrom(); hour <= configDay.getHourTo(); hour++) {
                ScheduleEntry entry = new ScheduleEntry();
                entry.setScheduleDay(scheduleDay);
                entry.setHour(hour);
                entry.setName(String.format(Locale.ROOT, "%02d:00", hour));
                entriesForDay.add(entry);
            }
            scheduleDay.setScheduleEntries(entriesForDay);
            scheduleEntries.addAll(entriesForDay);
        }
        scheduleEntryRepository.saveAll(scheduleEntries);

        return new ScheduleCalendarGenerationResult(
                true,
                scheduleName,
                period.startDate(),
                period.endDate(),
                scheduleDays.size(),
                scheduleEntries.size()
        );
    }

    private void addConfiguredParticipantAccessAndInvitations(Schedule schedule) {
        List<ScheduleAccess> accesses = new ArrayList<>();
        List<ParticipantAccessToken> invitationTokens = new ArrayList<>();
        List<Participant> participants = participantRepository.findEnabledForNewScheduleAutomation();
        Instant invitationCreatedAt = null;
        Instant invitationExpiration = null;

        for (Participant participant : participants) {
            // The repository filters disabled participants too, but checking
            // again keeps the business rule intact if its implementation changes.
            if (!participant.isEnabled()) {
                continue;
            }
            if (participant.isNewScheduleAutoassignmentEnabled()) {
                ScheduleAccess access = new ScheduleAccess();
                access.setSchedule(schedule);
                access.setParticipant(participant);
                accesses.add(access);
            }
            if (participant.isNewScheduleAutoinvitementEnabled()) {
                if (invitationExpiration == null) {
                    invitationCreatedAt = Instant.now();
                    invitationExpiration = invitationCreatedAt.plus(requiredAutoinvitementExpiration());
                }
                ParticipantAccessToken accessToken = new ParticipantAccessToken();
                accessToken.setSchedule(schedule);
                accessToken.setParticipant(participant);
                accessToken.setToken(generateInvitationToken());
                accessToken.setCreatedAt(Date.from(invitationCreatedAt));
                accessToken.setExpiresAt(Date.from(invitationExpiration));
                invitationTokens.add(accessToken);
            }
        }

        if (!accesses.isEmpty()) {
            scheduleAccessRepository.saveAll(accesses);
        }
        if (!invitationTokens.isEmpty()) {
            participantAccessTokenRepository.saveAll(invitationTokens);
        }
    }

    private Duration requiredAutoinvitementExpiration() {
        if (autoinvitementExpiration == null || autoinvitementExpiration.isNegative()
                || autoinvitementExpiration.isZero()) {
            throw new IllegalStateException("new-schedule-autoinvitement.expiration must be greater than zero");
        }
        return autoinvitementExpiration;
    }

    private String generateInvitationToken() {
        byte[] bytes = new byte[INVITATION_TOKEN_BYTE_LENGTH];
        INVITATION_TOKEN_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Map<Short, ConfigScheduleDay> activeDaysByWeekday() {
        Map<Short, ConfigScheduleDay> result = new HashMap<>();
        for (ConfigScheduleDay configDay : configScheduleDayRepository.findByActiveTrueOrderByDayOfWeekAsc()) {
            validate(configDay);
            ConfigScheduleDay previous = result.putIfAbsent(configDay.getDayOfWeek(), configDay);
            if (previous != null) {
                throw new IllegalStateException(
                        "More than one active config_schedule_days row exists for day " + configDay.getDayOfWeek()
                );
            }
        }
        return result;
    }

    private void validate(ConfigScheduleDay configDay) {
        if (configDay.getDayOfWeek() < DayOfWeek.MONDAY.getValue()
                || configDay.getDayOfWeek() > DayOfWeek.SUNDAY.getValue()) {
            throw new IllegalStateException("config_schedule_days.day_of_week must be between 1 and 7");
        }
        if (configDay.getHourFrom() < 0 || configDay.getHourTo() > 23
                || configDay.getHourFrom() > configDay.getHourTo()) {
            throw new IllegalStateException("config_schedule_days hours must be between 0 and 23 and ordered");
        }
    }

    private Set<LocalDate> restrictedDates(SchedulePeriod period) {
        Set<LocalDate> result = new HashSet<>();
        restrictedDateRepository.findByRestrictedDateBetween(period.startDate(), period.endDate())
                .forEach(config -> result.add(config.getRestrictedDate()));
        return result;
    }

    private SchedulePeriod periodFor(LocalDate generationDate) {
        YearMonth month = YearMonth.from(generationDate);
        LocalDate firstFullWeekStart = firstFullWeekStart(month);
        if (generationDate.isAfter(firstFullWeekStart)) {
            month = month.plusMonths(1);
            firstFullWeekStart = firstFullWeekStart(month);
        }
        LocalDate endDate = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new SchedulePeriod(month, firstFullWeekStart, endDate);
    }

    private LocalDate firstFullWeekStart(YearMonth month) {
        return month.atDay(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    private String scheduleName(YearMonth month) {
        String monthName = month.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, POLISH_LOCALE);
        return "Grafik " + capitalize(monthName) + " " + month.getYear();
    }

    private String scheduleDayName(LocalDate date) {
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, POLISH_LOCALE);
        return date + " (" + capitalize(dayName) + ")";
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase(POLISH_LOCALE) + value.substring(1);
    }

    private record SchedulePeriod(YearMonth month, LocalDate startDate, LocalDate endDate) {
    }
}
