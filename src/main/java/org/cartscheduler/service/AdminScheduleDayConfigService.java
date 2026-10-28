package org.cartscheduler.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigScheduleDay;
import org.cartscheduler.repository.ConfigScheduleDayRepository;
import org.cartscheduler.web.form.ScheduleDayConfigForm;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminScheduleDayConfigService {
    private final ConfigScheduleDayRepository scheduleDayRepository;

    public List<ConfigScheduleDay> findAll() {
        return scheduleDayRepository.findAll(Sort.by(Sort.Order.asc("dayOfWeek")));
    }

    public ConfigScheduleDay getRequired(long id) {
        return scheduleDayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono konfiguracji dnia grafiku o ID " + id + "."));
    }

    @Transactional
    public ConfigScheduleDay create(ScheduleDayConfigForm form) {
        validate(form, null);
        ConfigScheduleDay configuration = new ConfigScheduleDay();
        copyValues(form, configuration);
        return scheduleDayRepository.save(configuration);
    }

    @Transactional
    public ConfigScheduleDay update(long id, ScheduleDayConfigForm form) {
        ConfigScheduleDay configuration = getRequired(id);
        validate(form, id);
        copyValues(form, configuration);
        return scheduleDayRepository.save(configuration);
    }

    @Transactional
    public void delete(long id) {
        scheduleDayRepository.delete(getRequired(id));
    }

    private void validate(ScheduleDayConfigForm form, Long currentId) {
        if (form.getHourFrom() > form.getHourTo()) {
            throw new FieldValidationException(
                    "hourTo",
                    "Godzina zakończenia nie może być wcześniejsza niż godzina rozpoczęcia."
            );
        }
        scheduleDayRepository.findByDayOfWeek(form.getDayOfWeek())
                .filter(existing -> currentId == null || existing.getId() != currentId)
                .ifPresent(existing -> {
                    throw new FieldValidationException("dayOfWeek", "Konfiguracja dla tego dnia tygodnia już istnieje.");
                });
    }

    private void copyValues(ScheduleDayConfigForm form, ConfigScheduleDay configuration) {
        configuration.setDayOfWeek(form.getDayOfWeek());
        configuration.setHourFrom(form.getHourFrom());
        configuration.setHourTo(form.getHourTo());
        configuration.setActive(form.isActive());
    }
}
