package org.cartscheduler.service;

import org.cartscheduler.dto.rest.response.ScheduleDayDto;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ScheduleDayRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ScheduleDayService {

    @Autowired
    private ScheduleDayRepository scheduleDayRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    public List<ScheduleDayDto> prepareScheduleDayDtoListForSchedule(Long id) {
        List<ScheduleDayDto> scheduleDays = new ArrayList<>();
        for (ScheduleDay scheduleDay : scheduleDayRepository.findForSchedule(id)) {
            scheduleDays.add(new ScheduleDayDto(scheduleDay));
        }
        return scheduleDays;
    }

    public List<ScheduleDayDto> prepareScheduleDayDtoListForScheduleAndParticipant(
            Long scheduleId,
            Long participantId
    ) {
        List<ScheduleDay> scheduleDays = scheduleDayRepository.findForSchedule(scheduleId);
        if (scheduleDays.isEmpty()) {
            return List.of();
        }

        Set<Long> scheduleDayIdsWithProposals = new HashSet<>(
                proposalRepository.findScheduleDayIdsWithProposalsForParticipantAndSchedule(participantId, scheduleId)
        );
        List<ScheduleDayDto> result = new ArrayList<>();
        for (ScheduleDay scheduleDay : scheduleDays) {
            result.add(new ScheduleDayDto(
                    scheduleDay,
                    scheduleDayIdsWithProposals.contains(scheduleDay.getId())
            ));
        }
        return result;
    }

    public ScheduleDayDto prepareScheduleDayDtoForParticipant(Long id, Long scheduleId) {
        ScheduleDay scheduleDay = scheduleDayRepository.findByIdForSchedule(id, scheduleId);
        if (scheduleDay == null) {
            return null;
        }
        return new ScheduleDayDto(scheduleDay);
    }
}
