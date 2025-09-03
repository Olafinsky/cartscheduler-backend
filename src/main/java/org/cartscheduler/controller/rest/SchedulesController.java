package org.cartscheduler.controller.rest;

import org.cartscheduler.dto.rest.response.ScheduleDayDto;
import org.cartscheduler.dto.rest.response.ScheduleDto;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.service.ScheduleDayService;
import org.cartscheduler.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class SchedulesController {

    @Autowired
    ScheduleService scheduleService;

    @Autowired
    ScheduleDayService scheduleDayService;

    @GetMapping("/{scheduleId}/")
    public ScheduleDto getSchedule(@AuthenticationPrincipal RestUserDetails userDetails, @PathVariable("scheduleId") Long id) {
        return scheduleService.prepareScheduleDtoForParticipant(userDetails.getId(), id);
    }

    @GetMapping("/{scheduleId}/days/")
    public List<ScheduleDayDto> getDaysForSchedule(@AuthenticationPrincipal RestUserDetails userDetails, @PathVariable("scheduleId") Long scheduleId) {
        if (!scheduleService.checkScheduleAccess(userDetails, scheduleId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return scheduleDayService.prepareScheduleDayDtoListForSchedule(userDetails.getId());
    }

    @GetMapping("/{scheduleId}/days/{dayId}/")
    public ScheduleDayDto getDayForSchedule(@AuthenticationPrincipal RestUserDetails userDetails, @PathVariable("scheduleId") Long scheduleId, @PathVariable("dayId") Long dayId) {
        if (!scheduleService.checkScheduleAccess(userDetails, scheduleId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return scheduleDayService.prepareScheduleDayDtoForParticipant(dayId, scheduleId);
    }
}
