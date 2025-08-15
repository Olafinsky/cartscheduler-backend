package org.cartscheduler.controller.rest;

import org.cartscheduler.entity.Schedule;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class SchedulesController {

    @Autowired
    ScheduleService scheduleService;

    @GetMapping("/")
    public List<Schedule> index(@AuthenticationPrincipal RestUserDetails userDetails) {
        return scheduleService.findForParticipant(userDetails.getId());
    }
}
