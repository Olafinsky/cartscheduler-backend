package org.cartscheduler.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigCandidateAvailabilityScore;
import org.cartscheduler.repository.ConfigCandidateAvailabilityScoreRepository;
import org.cartscheduler.web.form.CandidateAvailabilityScoreConfigForm;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCandidateAvailabilityScoreConfigService {
    private final ConfigCandidateAvailabilityScoreRepository scoreRepository;

    public List<ConfigCandidateAvailabilityScore> findAll() {
        return scoreRepository.findAll(Sort.by(
                Sort.Order.asc("appearanceCountFrom"),
                Sort.Order.asc("appearanceCountTo")
        ));
    }

    public ConfigCandidateAvailabilityScore getRequired(long id) {
        return scoreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono punktacji dostępności o ID " + id + "."));
    }

    @Transactional
    public ConfigCandidateAvailabilityScore create(CandidateAvailabilityScoreConfigForm form) {
        validate(form, null);
        ConfigCandidateAvailabilityScore score = new ConfigCandidateAvailabilityScore();
        copyValues(form, score);
        return scoreRepository.save(score);
    }

    @Transactional
    public ConfigCandidateAvailabilityScore update(long id, CandidateAvailabilityScoreConfigForm form) {
        ConfigCandidateAvailabilityScore score = getRequired(id);
        validate(form, id);
        copyValues(form, score);
        return scoreRepository.save(score);
    }

    @Transactional
    public void delete(long id) {
        scoreRepository.delete(getRequired(id));
    }

    private void validate(CandidateAvailabilityScoreConfigForm form, Long currentId) {
        if (form.getAppearanceCountFrom() > form.getAppearanceCountTo()) {
            throw new FieldValidationException(
                    "appearanceCountTo",
                    "Koniec zakresu nie może być mniejszy od jego początku."
            );
        }
        scoreRepository.findByAppearanceCountFromAndAppearanceCountTo(
                        form.getAppearanceCountFrom(), form.getAppearanceCountTo()
                )
                .filter(existing -> currentId == null || existing.getId() != currentId)
                .ifPresent(existing -> {
                    throw new FieldValidationException("appearanceCountTo", "Taki zakres jest już skonfigurowany.");
                });
    }

    private void copyValues(CandidateAvailabilityScoreConfigForm form, ConfigCandidateAvailabilityScore score) {
        score.setAppearanceCountFrom(form.getAppearanceCountFrom());
        score.setAppearanceCountTo(form.getAppearanceCountTo());
        score.setPoints(form.getPoints());
    }
}
