package org.cartscheduler.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigPairRule;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.repository.ConfigPairRuleRepository;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.web.form.PairRuleConfigForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPairRuleConfigService {
    private final ConfigPairRuleRepository pairRuleRepository;
    private final ParticipantRepository participantRepository;

    public List<ConfigPairRule> findAll() {
        return pairRuleRepository.findAllWithParticipants();
    }

    public ConfigPairRule getRequired(long id) {
        return pairRuleRepository.findWithParticipantsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono reguły pary o ID " + id + "."));
    }

    @Transactional
    public ConfigPairRule create(PairRuleConfigForm form) {
        CanonicalPair pair = validateAndResolvePair(form, null);
        ConfigPairRule rule = new ConfigPairRule();
        copyValues(form, rule, pair);
        return pairRuleRepository.save(rule);
    }

    @Transactional
    public ConfigPairRule update(long id, PairRuleConfigForm form) {
        ConfigPairRule rule = getRequired(id);
        CanonicalPair pair = validateAndResolvePair(form, id);
        copyValues(form, rule, pair);
        return pairRuleRepository.save(rule);
    }

    @Transactional
    public void delete(long id) {
        pairRuleRepository.delete(getRequired(id));
    }

    private CanonicalPair validateAndResolvePair(PairRuleConfigForm form, Long currentId) {
        if (form.getParticipant1Id().equals(form.getParticipant2Id())) {
            throw new FieldValidationException("participant2Id", "Para musi składać się z dwóch różnych uczestników.");
        }
        Participant first = requiredParticipant(form.getParticipant1Id(), "participant1Id");
        Participant second = requiredParticipant(form.getParticipant2Id(), "participant2Id");
        if (first.getId() > second.getId()) {
            Participant swap = first;
            first = second;
            second = swap;
        }
        final Participant canonicalFirst = first;
        final Participant canonicalSecond = second;
        pairRuleRepository.findByParticipant1_IdAndParticipant2_Id(first.getId(), second.getId())
                .filter(existing -> currentId == null || existing.getId() != currentId)
                .ifPresent(existing -> {
                    throw new FieldValidationException("participant2Id", "Dla tej pary istnieje już reguła.");
                });
        return new CanonicalPair(canonicalFirst, canonicalSecond);
    }

    private Participant requiredParticipant(long id, String field) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new FieldValidationException(field, "Wybrany uczestnik już nie istnieje."));
    }

    private void copyValues(PairRuleConfigForm form, ConfigPairRule rule, CanonicalPair pair) {
        rule.setParticipant1(pair.first());
        rule.setParticipant2(pair.second());
        rule.setCompatibilityMode(form.getCompatibilityMode());
        rule.setScorePoints(form.getScorePoints());
    }

    private record CanonicalPair(Participant first, Participant second) {
    }
}
