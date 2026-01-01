package org.cartscheduler.controller.rest;

import org.cartscheduler.dto.rest.response.ProposalDto;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.service.ProposalService;
import org.cartscheduler.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/proposals")
public class ProposalsController {

    @Autowired
    ProposalService proposalService;

    @Autowired
    ScheduleService scheduleService;

    @GetMapping("/schedule-day/{scheduleDayId}/participant/{participantId}")
    public List<ProposalDto> index(@AuthenticationPrincipal RestUserDetails userDetails, @PathVariable("scheduleDayId") Long scheduleDayId, @PathVariable("participantId") Long participantId) {
        if (!scheduleService.checkScheduleAccess(userDetails, userDetails.getScheduleId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return proposalService.prepareProposalDto(participantId, scheduleDayId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal RestUserDetails userDetails, @PathVariable("id") Long id)
    {
        if (!scheduleService.checkScheduleAccess(userDetails, userDetails.getScheduleId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        proposalService.delete(id);
    }
}
