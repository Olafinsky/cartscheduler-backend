package org.cartscheduler.service;

import org.cartscheduler.dto.rest.response.ProposalDto;
import org.cartscheduler.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;

    @Transactional(readOnly = true)
    public List<ProposalDto> prepareProposalDto(Long participantId, Long scheduleDayId) {
        return proposalRepository.findForParticipantAndScheduleDay(participantId, scheduleDayId).stream()
                .map(ProposalDto::new)
                .toList();
    }

    public void delete(Long id) {
        throw notImplemented();
    }

    private ResponseStatusException notImplemented() {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Proposal support has not been implemented yet");
    }
}
