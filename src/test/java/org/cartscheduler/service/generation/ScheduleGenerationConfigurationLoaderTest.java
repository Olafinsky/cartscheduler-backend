package org.cartscheduler.service.generation;

import java.util.List;
import java.util.Optional;
import org.cartscheduler.entity.ConfigCandidateAssignmentScore;
import org.cartscheduler.entity.ConfigGeneration;
import org.cartscheduler.entity.ConfigPairAssignmentScore;
import org.cartscheduler.entity.ConfigPairCompatibilityMode;
import org.cartscheduler.entity.ConfigPairRule;
import org.cartscheduler.entity.ConfigParticipantRule;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.IntRange;
import org.cartscheduler.generator.OccurrenceScoreRule;
import org.cartscheduler.repository.ConfigCandidateAssignmentScoreRepository;
import org.cartscheduler.repository.ConfigGenerationRepository;
import org.cartscheduler.repository.ConfigPairAssignmentScoreRepository;
import org.cartscheduler.repository.ConfigPairRuleRepository;
import org.cartscheduler.repository.ConfigParticipantRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cartscheduler.support.TestEntityFactory.participant;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ScheduleGenerationConfigurationLoaderTest {
    @Mock
    private ConfigGenerationRepository generationRepository;

    @Mock
    private ConfigParticipantRuleRepository participantRuleRepository;

    @Mock
    private ConfigPairRuleRepository pairRuleRepository;

    @Mock
    private ConfigCandidateAssignmentScoreRepository candidateAssignmentScoreRepository;

    @Mock
    private ConfigPairAssignmentScoreRepository pairAssignmentScoreRepository;

    private ScheduleGenerationConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ScheduleGenerationConfigurationLoader(
                generationRepository,
                participantRuleRepository,
                pairRuleRepository,
                candidateAssignmentScoreRepository,
                pairAssignmentScoreRepository
        );
    }

    @Test
    void loadsCohesiveParticipantAndPairConfigurationRows() {
        Participant first = participant(1L);
        Participant second = participant(2L);
        ConfigGeneration generation = generationConfiguration();

        ConfigParticipantRule participantRule = new ConfigParticipantRule();
        participantRule.setParticipant(first);
        participantRule.setServiceLengthMin((short) 2);
        participantRule.setServiceLengthMax((short) 3);
        participantRule.setBreakLengthMin((short) 1);
        participantRule.setBreakLengthMax((short) 2);
        participantRule.setPrivilegePoints(6);

        ConfigPairRule pairRule = new ConfigPairRule();
        pairRule.setParticipant1(first);
        pairRule.setParticipant2(second);
        pairRule.setCompatibilityMode(ConfigPairCompatibilityMode.REQUIRED);
        pairRule.setScorePoints(8);

        ConfigCandidateAssignmentScore candidateAssignmentScore = new ConfigCandidateAssignmentScore();
        candidateAssignmentScore.setAssignmentCountFrom(3);
        candidateAssignmentScore.setAssignmentCountTo(5);
        candidateAssignmentScore.setPoints(7);
        ConfigPairAssignmentScore pairAssignmentScore = new ConfigPairAssignmentScore();
        pairAssignmentScore.setAssignmentCountFrom(1);
        pairAssignmentScore.setAssignmentCountTo(2);
        pairAssignmentScore.setPoints(5);

        given(generationRepository.findById(ConfigGeneration.SINGLETON_ID)).willReturn(Optional.of(generation));
        given(participantRuleRepository.findAll()).willReturn(List.of(participantRule));
        given(pairRuleRepository.findAll()).willReturn(List.of(pairRule));
        given(candidateAssignmentScoreRepository.findAll()).willReturn(List.of(candidateAssignmentScore));
        given(pairAssignmentScoreRepository.findAll()).willReturn(List.of(pairAssignmentScore));

        ScheduleGenerationConfiguration configuration = loader.load();

        assertThat(configuration.rowScore()).isEqualTo(new RowFullOrEmptyScore(1, 2, 3));
        assertThat(configuration.configuredServiceRanges()).containsEntry(1L, new IntRange(2, 3));
        assertThat(configuration.configuredBreakRanges()).containsEntry(1L, new IntRange(1, 2));
        assertThat(configuration.participantPrivilegePoints()).containsEntry(1L, 6);
        assertThat(configuration.globalPairRules().accepts(1L, 2L)).isTrue();
        assertThat(configuration.globalPairRules().accepts(1L, 3L)).isFalse();
        assertThat(configuration.pairScorePoints()).containsEntry(new CandidatePair(1L, 2L), 8);
        assertThat(configuration.candidateAssignmentScoreRules())
                .containsExactly(new OccurrenceScoreRule(3, 5, 7));
        assertThat(configuration.pairAssignmentScoreRules())
                .containsExactly(new OccurrenceScoreRule(1, 2, 5));
    }

    @Test
    void rejectsAnIncompleteParticipantLengthRange() {
        ConfigParticipantRule participantRule = new ConfigParticipantRule();
        participantRule.setParticipant(participant(1L));
        participantRule.setServiceLengthMin((short) 2);
        given(generationRepository.findById(ConfigGeneration.SINGLETON_ID))
                .willReturn(Optional.of(generationConfiguration()));
        given(participantRuleRepository.findAll()).willReturn(List.of(participantRule));

        assertThatThrownBy(loader::load)
                .isInstanceOf(ScheduleGenerationException.class)
                .hasMessageContaining("Incomplete configured service length");
    }

    private ConfigGeneration generationConfiguration() {
        ConfigGeneration generation = new ConfigGeneration();
        generation.setId(ConfigGeneration.SINGLETON_ID);
        generation.setEmptyRowPoints(1);
        generation.setFullRowAdjacentToIncompletePoints(2);
        generation.setFullRowNotAdjacentToIncompletePoints(3);
        return generation;
    }
}
