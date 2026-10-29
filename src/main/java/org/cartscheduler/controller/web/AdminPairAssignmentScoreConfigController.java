package org.cartscheduler.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigPairAssignmentScore;
import org.cartscheduler.service.AdminPairAssignmentScoreConfigService;
import org.cartscheduler.service.FieldValidationException;
import org.cartscheduler.web.form.AssignmentScoreConfigForm;
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
public class AdminPairAssignmentScoreConfigController {
    private final AdminPairAssignmentScoreConfigService scoreConfigService;

    @GetMapping("/admin/config/pair-assignment-scores")
    public String index(Model model) {
        model.addAttribute("scores", scoreConfigService.findAll());
        return "admin/config/pair-assignment-scores/index";
    }

    @GetMapping("/admin/config/pair-assignment-scores/new")
    public String createForm(Model model) {
        populateFormModel(model, new AssignmentScoreConfigForm(), false, null);
        return "admin/config/pair-assignment-scores/form";
    }

    @PostMapping("/admin/config/pair-assignment-scores")
    public String create(
            @Valid @ModelAttribute("form") AssignmentScoreConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, false, null);
            return "admin/config/pair-assignment-scores/form";
        }
        try {
            scoreConfigService.create(form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, false, null);
            return "admin/config/pair-assignment-scores/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Dodano punktację wystąpień par.");
        return "redirect:/admin/config/pair-assignment-scores";
    }

    @GetMapping("/admin/config/pair-assignment-scores/{id}")
    public String view(@PathVariable long id, Model model) {
        model.addAttribute("score", scoreConfigService.getRequired(id));
        return "admin/config/pair-assignment-scores/view";
    }

    @GetMapping("/admin/config/pair-assignment-scores/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        ConfigPairAssignmentScore score = scoreConfigService.getRequired(id);
        populateFormModel(model, formFrom(score), true, id);
        return "admin/config/pair-assignment-scores/form";
    }

    @PostMapping("/admin/config/pair-assignment-scores/{id}/edit")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") AssignmentScoreConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, true, id);
            return "admin/config/pair-assignment-scores/form";
        }
        try {
            scoreConfigService.update(id, form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, true, id);
            return "admin/config/pair-assignment-scores/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Zapisano punktację wystąpień par.");
        return "redirect:/admin/config/pair-assignment-scores";
    }

    @PostMapping("/admin/config/pair-assignment-scores/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        scoreConfigService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Usunięto punktację wystąpień par.");
        return "redirect:/admin/config/pair-assignment-scores";
    }

    private AssignmentScoreConfigForm formFrom(ConfigPairAssignmentScore score) {
        AssignmentScoreConfigForm form = new AssignmentScoreConfigForm();
        form.setAssignmentCountFrom(score.getAssignmentCountFrom());
        form.setAssignmentCountTo(score.getAssignmentCountTo());
        form.setPoints(score.getPoints());
        return form;
    }

    private void populateFormModel(Model model, AssignmentScoreConfigForm form, boolean editing, Long scoreId) {
        model.addAttribute("form", form);
        model.addAttribute("editing", editing);
        model.addAttribute("scoreId", scoreId);
    }
}
