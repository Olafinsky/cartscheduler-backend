package org.cartscheduler.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigGeneration;
import org.cartscheduler.repository.ConfigGenerationRepository;
import org.cartscheduler.web.form.GenerationConfigForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminGenerationConfigService {
    private final ConfigGenerationRepository generationRepository;

    public List<ConfigGeneration> findAll() {
        return generationRepository.findAll();
    }

    public ConfigGeneration getRequired(short id) {
        return generationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono konfiguracji generowania grafiku."));
    }

    @Transactional
    public ConfigGeneration update(short id, GenerationConfigForm form) {
        ConfigGeneration configuration = getRequired(id);
        configuration.setEmptyRowPoints(form.getEmptyRowPoints());
        configuration.setFullRowAdjacentToIncompletePoints(form.getFullRowAdjacentToIncompletePoints());
        configuration.setFullRowNotAdjacentToIncompletePoints(form.getFullRowNotAdjacentToIncompletePoints());
        return generationRepository.save(configuration);
    }
}
