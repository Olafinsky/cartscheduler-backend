package org.cartscheduler.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleAccess;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ScheduleAccessRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.cartscheduler.web.form.ScheduleForm;
import org.cartscheduler.web.model.ScheduleDayDetails;
import org.cartscheduler.web.model.ScheduleDetailsView;
import org.cartscheduler.web.model.ScheduleListItem;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleDayRepository scheduleDayRepository;
    private final ScheduleAccessRepository scheduleAccessRepository;
    private final ParticipantRepository participantRepository;

    public List<ScheduleListItem> findAll() {
        List<ScheduleListItem> schedules = new ArrayList<>();
        for (Schedule schedule : scheduleRepository.findAll(Sort.by(Sort.Order.desc("id")))) {
            schedules.add(new ScheduleListItem(
                    schedule.getId(),
                    schedule.getName(),
                    scheduleDayRepository.findForSchedule(schedule.getId()).size()
            ));
        }
        return schedules;
    }

    public ScheduleDetailsView getDetails(long scheduleId) {
        Schedule schedule = getRequired(scheduleId);
        List<ScheduleDayDetails> days = scheduleDayRepository.findForScheduleWithEntries(scheduleId).stream()
                .sorted(scheduleDayComparator())
                .map(day -> new ScheduleDayDetails(
                        day.getName(),
                        day.getScheduleDate(),
                        day.getDayOfWeek(),
                        day.getScheduleEntries().stream()
                                .map(entry -> entry.getHour())
                                .sorted()
                                .toList()
                ))
                .toList();
        return new ScheduleDetailsView(schedule.getId(), schedule.getName(), days);
    }

    public List<Participant> findAllParticipants() {
        return participantRepository.findAll(Sort.by(
                Sort.Order.asc("name").ignoreCase(),
                Sort.Order.asc("id")
        ));
    }

    public Set<Long> findAccessibleParticipantIds(long scheduleId) {
        Set<Long> participantIds = new LinkedHashSet<>();
        findAccessibleParticipants(scheduleId)
                .forEach(participant -> participantIds.add(participant.getId()));
        return participantIds;
    }

    public List<Participant> findAccessibleParticipants(long scheduleId) {
        getRequired(scheduleId);
        return scheduleAccessRepository.findBySchedule_IdOrderByParticipant_NameAsc(scheduleId).stream()
                .map(ScheduleAccess::getParticipant)
                .toList();
    }

    @Transactional
    public Schedule update(long scheduleId, ScheduleForm form) {
        Schedule schedule = getRequired(scheduleId);
        Set<Long> requestedParticipantIds = form.getParticipantIds() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(form.getParticipantIds());
        List<Participant> participants = participantRepository.findAllById(requestedParticipantIds);
        if (participants.size() != requestedParticipantIds.size()) {
            throw new FieldValidationException(
                    "participantIds",
                    "Wybrano uczestnika, który już nie istnieje."
            );
        }

        schedule.setName(normalized(form.getName()));
        scheduleRepository.save(schedule);
        synchronizeParticipantAccess(schedule, participants);
        return schedule;
    }

    @Transactional
    public void delete(long scheduleId) {
        scheduleRepository.delete(getRequired(scheduleId));
    }

    public Schedule getRequired(long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono grafiku o ID " + scheduleId + "."));
    }

    private void synchronizeParticipantAccess(Schedule schedule, List<Participant> participants) {
        scheduleAccessRepository.deleteBySchedule_Id(schedule.getId());
        if (participants.isEmpty()) {
            return;
        }

        List<ScheduleAccess> accesses = new ArrayList<>();
        for (Participant participant : participants) {
            ScheduleAccess access = new ScheduleAccess();
            access.setSchedule(schedule);
            access.setParticipant(participant);
            accesses.add(access);
        }
        scheduleAccessRepository.saveAll(accesses);
    }

    private String normalized(String value) {
        return value == null ? null : value.strip();
    }

    private Comparator<ScheduleDay> scheduleDayComparator() {
        return Comparator
                .comparing(
                        ScheduleDay::getScheduleDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparingInt(ScheduleDay::getDayOfWeek)
                .thenComparing(ScheduleDay::getName, String.CASE_INSENSITIVE_ORDER);
    }
}
