package org.cartscheduler.service;

import org.cartscheduler.dto.rest.response.ProposalDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProposalService {

    public List<ProposalDto> prepareProposalDto(Long participantId, Long scheduleDayId) {
        throw notImplemented();
    }

    public void delete(Long id) {
        throw notImplemented();
    }

    private ResponseStatusException notImplemented() {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Proposal support has not been implemented yet");
    }
}
