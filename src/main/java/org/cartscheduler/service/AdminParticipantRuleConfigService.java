package org.cartscheduler.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigParticipantRule;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.repository.ConfigParticipantRuleRepository;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.web.form.ParticipantRuleConfigForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminParticipantRuleConfigService {
    private final ConfigParticipantRuleRepository participantRuleRepository;
    private final ParticipantRepository participantRepository;

    public List<ConfigParticipantRule> findAll() {
        return participantRuleRepository.findAllWithParticipant();
    }

    public ConfigParticipantRule getRequired(long id) {
        return participantRuleRepository.findWithParticipantById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono reguły uczestnika o ID " + id + "."));
    }

    @Transactional
    public ConfigParticipantRule create(ParticipantRuleConfigForm form) {
        validate(form, null);
        ConfigParticipantRule rule = new ConfigParticipantRule();
        copyValues(form, rule);
        return participantRuleRepository.save(rule);
    }

    @Transactional
    public ConfigParticipantRule update(long id, ParticipantRuleConfigForm form) {
        ConfigParticipantRule rule = getRequired(id);
        validate(form, id);
        copyValues(form, rule);
        return participantRuleRepository.save(rule);
    }

    @Transactional
    public void delete(long id) {
        participantRuleRepository.delete(getRequired(id));
    }

    private void validate(ParticipantRuleConfigForm form, Long currentId) {
        validateRange(
                form.getServiceLengthMin(), form.getServiceLengthMax(),
                "serviceLengthMin", "serviceLengthMax", "długość służby"
        );
        validateRange(
                form.getBreakLengthMin(), form.getBreakLengthMax(),
                "breakLengthMin", "breakLengthMax", "długość przerwy"
        );
        participantRuleRepository.findByParticipant_Id(form.getParticipantId())
                .filter(existing -> currentId == null || existing.getId() != currentId)
                .ifPresent(existing -> {
                    throw new FieldValidationException("participantId", "Dla tego uczestnika istnieje już reguła.");
                });
    }

    private void validateRange(Short minimum, Short maximum, String minimumField, String maximumField, String label) {
        if (minimum == null && maximum == null) {
            return;
        }
        if (minimum == null) {
            throw new FieldValidationException(minimumField, "Podaj minimalną " + label + ".");
        }
        if (maximum == null) {
            throw new FieldValidationException(maximumField, "Podaj maksymalną " + label + ".");
        }
        if (minimum > maximum) {
            throw new FieldValidationException(maximumField, "Maksymalna " + label + " nie może być mniejsza od minimalnej.");
        }
    }

    private void copyValues(ParticipantRuleConfigForm form, ConfigParticipantRule rule) {
        rule.setParticipant(requiredParticipant(form.getParticipantId()));
        rule.setServiceLengthMin(form.getServiceLengthMin());
        rule.setServiceLengthMax(form.getServiceLengthMax());
        rule.setBreakLengthMin(form.getBreakLengthMin());
        rule.setBreakLengthMax(form.getBreakLengthMax());
        rule.setPrivilegePoints(form.getPrivilegePoints());
    }

    private Participant requiredParticipant(long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new FieldValidationException("participantId", "Wybrany uczestnik już nie istnieje."));
    }
}
