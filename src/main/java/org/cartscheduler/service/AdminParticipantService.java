package org.cartscheduler.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.ParticipantAgent;
import org.cartscheduler.repository.ParticipantAgentRepository;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.web.form.ParticipantForm;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminParticipantService {
    private final ParticipantRepository participantRepository;
    private final ParticipantAgentRepository participantAgentRepository;

    public List<Participant> findAll() {
        return participantRepository.findAll(Sort.by(Sort.Order.asc("name").ignoreCase()));
    }

    public Participant getRequired(long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono uczestnika o ID " + id + "."));
    }

    public Set<Long> findAssignedParticipantIdsForAgent(long agentParticipantId) {
        Set<Long> participantIds = new LinkedHashSet<>();
        participantAgentRepository.findByAgentParticipant_IdOrderByTargetParticipant_NameAsc(agentParticipantId)
                .forEach(relation -> participantIds.add(relation.getTargetParticipant().getId()));
        return participantIds;
    }

    public List<Participant> findAssignedParticipantsForAgent(long agentParticipantId) {
        return participantAgentRepository.findByAgentParticipant_IdOrderByTargetParticipant_NameAsc(agentParticipantId)
                .stream()
                .map(ParticipantAgent::getTargetParticipant)
                .toList();
    }

    @Transactional
    public Participant create(ParticipantForm form) {
        Participant participant = new Participant();
        copyValues(form, participant);
        Participant saved = participantRepository.saveAndFlush(participant);
        synchronizeAssignedParticipants(saved, form.getAssignedParticipantIds());
        return saved;
    }

    @Transactional
    public Participant update(long id, ParticipantForm form) {
        Participant participant = getRequired(id);
        copyValues(form, participant);
        participantRepository.save(participant);
        synchronizeAssignedParticipants(participant, form.getAssignedParticipantIds());
        return participant;
    }

    @Transactional
    public void delete(long id) {
        participantRepository.delete(getRequired(id));
    }

    private void synchronizeAssignedParticipants(Participant agent, Set<Long> requestedParticipantIds) {
        Set<Long> participantIds = requestedParticipantIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(requestedParticipantIds);
        // Every agent may always submit proposals for themselves and cannot be unchecked in the UI.
        participantIds.add(agent.getId());

        List<Participant> assignedParticipants = participantRepository.findAllById(participantIds);
        if (assignedParticipants.size() != participantIds.size()) {
            throw new FieldValidationException("assignedParticipantIds", "Wybrano uczestnika, który już nie istnieje.");
        }

        participantAgentRepository.deleteByAgentParticipant_Id(agent.getId());
        for (Participant assignedParticipant : assignedParticipants) {
            ParticipantAgent relation = new ParticipantAgent();
            relation.setAgentParticipant(agent);
            relation.setTargetParticipant(assignedParticipant);
            participantAgentRepository.save(relation);
        }
    }

    private void copyValues(ParticipantForm form, Participant participant) {
        participant.setName(normalized(form.getName()));
        participant.setEmail(normalized(form.getEmail()));
        participant.setEnabled(form.isEnabled());
        participant.setNewScheduleAutoassignmentEnabled(form.isNewScheduleAutoassignmentEnabled());
        participant.setNewScheduleAutoinvitementEnabled(form.isNewScheduleAutoinvitementEnabled());
        participant.setAllowPairSelection(form.isAllowPairSelection());
        participant.setAllowAlternativeGroupSelection(form.isAllowAlternativeGroupSelection());
    }

    private String normalized(String value) {
        return value == null ? null : value.strip();
    }
}
