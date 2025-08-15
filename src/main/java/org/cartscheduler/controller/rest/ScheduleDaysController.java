package org.cartscheduler.controller.rest;

import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.service.ScheduleDayService;
import org.cartscheduler.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/schedule-days")
public class ScheduleDaysController {

    @Autowired
    ScheduleDayService scheduleDayService;

    @Autowired
    ScheduleService scheduleService;

    @GetMapping("/{scheduleId}/")
    public List<ScheduleDay> index(@AuthenticationPrincipal RestUserDetails userDetails, @PathVariable("scheduleId") Long scheduleId) {
        if (!scheduleService.checkScheduleAccess(userDetails, scheduleId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return scheduleDayService.findForSchedule(userDetails.getId());
    }
}
