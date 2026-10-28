package org.cartscheduler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.ParticipantAgent;
import org.cartscheduler.repository.ParticipantAgentRepository;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.web.form.ParticipantForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ParticipantAgentRepository participantAgentRepository;

    @InjectMocks
    private AdminParticipantService participantService;

    @Test
    void readsParticipantsAssignedToTheCurrentAgent() {
        Participant agent = participant(25L, "Olaf Sokoliński");
        Participant assignedOne = participant(8L, "Anna Kowalska");
        Participant assignedTwo = participant(9L, "Jan Nowak");
        given(participantAgentRepository.findByAgentParticipant_IdOrderByTargetParticipant_NameAsc(25L))
                .willReturn(List.of(relation(agent, assignedOne), relation(agent, assignedTwo)));

        List<Participant> result = participantService.findAssignedParticipantsForAgent(25L);

        assertThat(result).containsExactly(assignedOne, assignedTwo);
        verify(participantAgentRepository).findByAgentParticipant_IdOrderByTargetParticipant_NameAsc(25L);
        verify(participantAgentRepository, never())
                .findByTargetParticipant_IdOrderByAgentParticipant_NameAsc(any(Long.class));
    }

    @Test
    void updatesAssignmentsWithTheCurrentParticipantAsTheAgent() {
        Participant agent = participant(25L, "Olaf Sokoliński");
        Participant assignedParticipant = participant(8L, "Anna Kowalska");
        ParticipantForm form = new ParticipantForm();
        form.setName(agent.getName());
        form.setEmail("olaf@example.test");
        form.setAssignedParticipantIds(Set.of(8L));
        given(participantRepository.findById(25L)).willReturn(java.util.Optional.of(agent));
        given(participantRepository.findAllById(Set.of(8L, 25L)))
                .willReturn(List.of(agent, assignedParticipant));

        participantService.update(25L, form);

        ArgumentCaptor<ParticipantAgent> relationCaptor = ArgumentCaptor.forClass(ParticipantAgent.class);
        verify(participantAgentRepository).deleteByAgentParticipant_Id(25L);
        verify(participantAgentRepository, never()).deleteByTargetParticipant_Id(any(Long.class));
        verify(participantAgentRepository, org.mockito.Mockito.times(2)).save(relationCaptor.capture());
        assertThat(relationCaptor.getAllValues())
                .allSatisfy(relation -> assertThat(relation.getAgentParticipant()).isSameAs(agent));
        assertThat(relationCaptor.getAllValues())
                .extracting(relation -> relation.getTargetParticipant().getId())
                .containsExactlyInAnyOrder(8L, 25L);
    }

    private Participant participant(long id, String name) {
        Participant participant = new Participant();
        participant.setId(id);
        participant.setName(name);
        return participant;
    }

    private ParticipantAgent relation(Participant agent, Participant target) {
        ParticipantAgent relation = new ParticipantAgent();
        relation.setAgentParticipant(agent);
        relation.setTargetParticipant(target);
        return relation;
    }
}
