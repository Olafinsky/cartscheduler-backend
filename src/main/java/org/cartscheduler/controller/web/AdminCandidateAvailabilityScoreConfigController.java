package org.cartscheduler.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigCandidateAvailabilityScore;
import org.cartscheduler.service.AdminCandidateAvailabilityScoreConfigService;
import org.cartscheduler.service.FieldValidationException;
import org.cartscheduler.web.form.CandidateAvailabilityScoreConfigForm;
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
public class AdminCandidateAvailabilityScoreConfigController {
    private final AdminCandidateAvailabilityScoreConfigService scoreConfigService;

    @GetMapping("/admin/config/candidate-availability-scores")
    public String index(Model model) {
        model.addAttribute("scores", scoreConfigService.findAll());
        return "admin/config/candidate-availability-scores/index";
    }

    @GetMapping("/admin/config/candidate-availability-scores/new")
    public String createForm(Model model) {
        populateFormModel(model, new CandidateAvailabilityScoreConfigForm(), false, null);
        return "admin/config/candidate-availability-scores/form";
    }

    @PostMapping("/admin/config/candidate-availability-scores")
    public String create(
            @Valid @ModelAttribute("form") CandidateAvailabilityScoreConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, false, null);
            return "admin/config/candidate-availability-scores/form";
        }
        try {
            scoreConfigService.create(form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, false, null);
            return "admin/config/candidate-availability-scores/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Dodano punktację dostępności.");
        return "redirect:/admin/config/candidate-availability-scores";
    }

    @GetMapping("/admin/config/candidate-availability-scores/{id}")
    public String view(@PathVariable long id, Model model) {
        model.addAttribute("score", scoreConfigService.getRequired(id));
        return "admin/config/candidate-availability-scores/view";
    }

    @GetMapping("/admin/config/candidate-availability-scores/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        ConfigCandidateAvailabilityScore score = scoreConfigService.getRequired(id);
        populateFormModel(model, formFrom(score), true, id);
        return "admin/config/candidate-availability-scores/form";
    }

    @PostMapping("/admin/config/candidate-availability-scores/{id}/edit")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") CandidateAvailabilityScoreConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, true, id);
            return "admin/config/candidate-availability-scores/form";
        }
        try {
            scoreConfigService.update(id, form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, true, id);
            return "admin/config/candidate-availability-scores/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Zapisano punktację dostępności.");
        return "redirect:/admin/config/candidate-availability-scores";
    }

    @PostMapping("/admin/config/candidate-availability-scores/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        scoreConfigService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Usunięto punktację dostępności.");
        return "redirect:/admin/config/candidate-availability-scores";
    }

    private CandidateAvailabilityScoreConfigForm formFrom(ConfigCandidateAvailabilityScore score) {
        CandidateAvailabilityScoreConfigForm form = new CandidateAvailabilityScoreConfigForm();
        form.setAppearanceCountFrom(score.getAppearanceCountFrom());
        form.setAppearanceCountTo(score.getAppearanceCountTo());
        form.setPoints(score.getPoints());
        return form;
    }

    private void populateFormModel(
            Model model,
            CandidateAvailabilityScoreConfigForm form,
            boolean editing,
            Long scoreId
    ) {
        model.addAttribute("form", form);
        model.addAttribute("editing", editing);
        model.addAttribute("scoreId", scoreId);
    }
}
