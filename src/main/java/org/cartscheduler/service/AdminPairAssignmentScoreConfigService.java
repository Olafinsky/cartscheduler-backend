package org.cartscheduler.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigPairAssignmentScore;
import org.cartscheduler.repository.ConfigPairAssignmentScoreRepository;
import org.cartscheduler.web.form.AssignmentScoreConfigForm;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPairAssignmentScoreConfigService {
    private final ConfigPairAssignmentScoreRepository scoreRepository;

    public List<ConfigPairAssignmentScore> findAll() {
        return scoreRepository.findAll(Sort.by(
                Sort.Order.asc("assignmentCountFrom"),
                Sort.Order.asc("assignmentCountTo")
        ));
    }

    public ConfigPairAssignmentScore getRequired(long id) {
        return scoreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono punktacji wystąpień par o ID " + id + "."));
    }

    @Transactional
    public ConfigPairAssignmentScore create(AssignmentScoreConfigForm form) {
        validate(form, null);
        ConfigPairAssignmentScore score = new ConfigPairAssignmentScore();
        copyValues(form, score);
        return scoreRepository.save(score);
    }

    @Transactional
    public ConfigPairAssignmentScore update(long id, AssignmentScoreConfigForm form) {
        ConfigPairAssignmentScore score = getRequired(id);
        validate(form, id);
        copyValues(form, score);
        return scoreRepository.save(score);
    }

    @Transactional
    public void delete(long id) {
        scoreRepository.delete(getRequired(id));
    }

    private void validate(AssignmentScoreConfigForm form, Long currentId) {
        if (form.getAssignmentCountFrom() > form.getAssignmentCountTo()) {
            throw new FieldValidationException(
                    "assignmentCountTo",
                    "Koniec zakresu nie może być mniejszy od jego początku."
            );
        }
        scoreRepository.findAll().stream()
                .filter(existing -> currentId == null || existing.getId() != currentId)
                .filter(existing -> rangesOverlap(
                        form.getAssignmentCountFrom(),
                        form.getAssignmentCountTo(),
                        existing.getAssignmentCountFrom(),
                        existing.getAssignmentCountTo()
                ))
                .findFirst()
                .ifPresent(existing -> {
                    throw new FieldValidationException(
                            "assignmentCountTo",
                            "Zakres nie może nakładać się na istniejącą punktację."
                    );
                });
    }

    private boolean rangesOverlap(int firstFrom, int firstTo, int secondFrom, int secondTo) {
        return firstFrom <= secondTo && secondFrom <= firstTo;
    }

    private void copyValues(AssignmentScoreConfigForm form, ConfigPairAssignmentScore score) {
        score.setAssignmentCountFrom(form.getAssignmentCountFrom());
        score.setAssignmentCountTo(form.getAssignmentCountTo());
        score.setPoints(form.getPoints());
    }
}
