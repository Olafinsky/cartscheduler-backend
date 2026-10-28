package org.cartscheduler.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigGeneration;
import org.cartscheduler.service.AdminGenerationConfigService;
import org.cartscheduler.web.form.GenerationConfigForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminGenerationConfigController {
    private final AdminGenerationConfigService generationConfigService;

    @GetMapping("/admin/config/generation")
    public String index(Model model) {
        model.addAttribute("configurations", generationConfigService.findAll());
        return "admin/config/generation/index";
    }

    @GetMapping("/admin/config/generation/{id}")
    public String view(@PathVariable short id, Model model) {
        model.addAttribute("configuration", generationConfigService.getRequired(id));
        return "admin/config/generation/view";
    }

    @GetMapping("/admin/config/generation/{id}/edit")
    public String editForm(@PathVariable short id, Model model) {
        ConfigGeneration configuration = generationConfigService.getRequired(id);
        GenerationConfigForm form = new GenerationConfigForm();
        form.setEmptyRowPoints(configuration.getEmptyRowPoints());
        form.setFullRowAdjacentToIncompletePoints(configuration.getFullRowAdjacentToIncompletePoints());
        form.setFullRowNotAdjacentToIncompletePoints(configuration.getFullRowNotAdjacentToIncompletePoints());
        form.setUniquePairPoints(configuration.getUniquePairPoints() == null ? 0 : configuration.getUniquePairPoints());
        populateFormModel(model, form, id);
        return "admin/config/generation/form";
    }

    @PostMapping("/admin/config/generation/{id}/edit")
    public String update(
            @PathVariable short id,
            @Valid @ModelAttribute("form") GenerationConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, id);
            return "admin/config/generation/form";
        }
        generationConfigService.update(id, form);
        redirectAttributes.addFlashAttribute("successMessage", "Zapisano konfigurację generowania grafiku.");
        return "redirect:/admin/config/generation";
    }

    private void populateFormModel(Model model, GenerationConfigForm form, short configurationId) {
        model.addAttribute("form", form);
        model.addAttribute("configurationId", configurationId);
    }
}
