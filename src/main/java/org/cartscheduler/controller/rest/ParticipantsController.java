package org.cartscheduler.controller.rest;

import org.cartscheduler.dto.rest.response.ParticipantDto;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.service.ParticipantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participants")
public class ParticipantsController {

    @Autowired
    ParticipantService participantService;

    @GetMapping("/")
    public List<ParticipantDto> index(@AuthenticationPrincipal RestUserDetails userDetails) {
        return participantService.prepareAssignedParticipantDtoListForAgentParticipantAndSchedule(userDetails.getId(), userDetails.getScheduleId());
    }
}
