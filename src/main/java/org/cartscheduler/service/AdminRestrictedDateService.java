package org.cartscheduler.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigScheduleRestrictedDate;
import org.cartscheduler.repository.ConfigScheduleRestrictedDateRepository;
import org.cartscheduler.web.form.RestrictedDateForm;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRestrictedDateService {
    private final ConfigScheduleRestrictedDateRepository restrictedDateRepository;

    public List<ConfigScheduleRestrictedDate> findAll() {
        return restrictedDateRepository.findAll(Sort.by(Sort.Order.asc("restrictedDate")));
    }

    public ConfigScheduleRestrictedDate getRequired(long id) {
        return restrictedDateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono daty zastrzeżonej o ID " + id + "."));
    }

    @Transactional
    public ConfigScheduleRestrictedDate create(RestrictedDateForm form) {
        validateDateAvailable(form.getRestrictedDate(), null);
        ConfigScheduleRestrictedDate restrictedDate = new ConfigScheduleRestrictedDate();
        restrictedDate.setRestrictedDate(form.getRestrictedDate());
        return restrictedDateRepository.save(restrictedDate);
    }

    @Transactional
    public ConfigScheduleRestrictedDate update(long id, RestrictedDateForm form) {
        ConfigScheduleRestrictedDate restrictedDate = getRequired(id);
        validateDateAvailable(form.getRestrictedDate(), id);
        restrictedDate.setRestrictedDate(form.getRestrictedDate());
        return restrictedDateRepository.save(restrictedDate);
    }

    @Transactional
    public void delete(long id) {
        restrictedDateRepository.delete(getRequired(id));
    }

    private void validateDateAvailable(LocalDate date, Long currentId) {
        restrictedDateRepository.findByRestrictedDate(date)
                .filter(existing -> currentId == null || existing.getId() != currentId)
                .ifPresent(existing -> {
                    throw new FieldValidationException("restrictedDate", "Ta data jest już na liście dat zastrzeżonych.");
                });
    }
}
