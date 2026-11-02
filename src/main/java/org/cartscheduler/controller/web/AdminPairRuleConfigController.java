package org.cartscheduler.controller.web;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigPairCompatibilityMode;
import org.cartscheduler.entity.ConfigPairRule;
import org.cartscheduler.service.AdminPairRuleConfigService;
import org.cartscheduler.service.AdminParticipantService;
import org.cartscheduler.service.FieldValidationException;
import org.cartscheduler.web.form.PairRuleConfigForm;
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
public class AdminPairRuleConfigController {
    private static final Map<ConfigPairCompatibilityMode, String> COMPATIBILITY_MODE_LABELS = Map.of(
            ConfigPairCompatibilityMode.ANY, "Dowolna",
            ConfigPairCompatibilityMode.REQUIRED, "Wymagana",
            ConfigPairCompatibilityMode.FORBIDDEN, "Zakazana"
    );

    private final AdminPairRuleConfigService pairRuleConfigService;
    private final AdminParticipantService participantService;

    @GetMapping("/admin/config/pair-rules")
    public String index(Model model) {
        model.addAttribute("rules", pairRuleConfigService.findAll());
        model.addAttribute("compatibilityModeLabels", COMPATIBILITY_MODE_LABELS);
        return "admin/config/pair-rules/index";
    }

    @GetMapping("/admin/config/pair-rules/new")
    public String createForm(Model model) {
        populateFormModel(model, new PairRuleConfigForm(), false, null);
        return "admin/config/pair-rules/form";
    }

    @PostMapping("/admin/config/pair-rules")
    public String create(
            @Valid @ModelAttribute("form") PairRuleConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, false, null);
            return "admin/config/pair-rules/form";
        }
        try {
            pairRuleConfigService.create(form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, false, null);
            return "admin/config/pair-rules/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Dodano regułę pary.");
        return "redirect:/admin/config/pair-rules";
    }

    @GetMapping("/admin/config/pair-rules/{id}")
    public String view(@PathVariable long id, Model model) {
        model.addAttribute("rule", pairRuleConfigService.getRequired(id));
        model.addAttribute("compatibilityModeLabels", COMPATIBILITY_MODE_LABELS);
        return "admin/config/pair-rules/view";
    }

    @GetMapping("/admin/config/pair-rules/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        ConfigPairRule rule = pairRuleConfigService.getRequired(id);
        populateFormModel(model, formFrom(rule), true, id);
        return "admin/config/pair-rules/form";
    }

    @PostMapping("/admin/config/pair-rules/{id}/edit")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") PairRuleConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, true, id);
            return "admin/config/pair-rules/form";
        }
        try {
            pairRuleConfigService.update(id, form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, true, id);
            return "admin/config/pair-rules/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Zapisano regułę pary.");
        return "redirect:/admin/config/pair-rules";
    }

    @PostMapping("/admin/config/pair-rules/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        pairRuleConfigService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Usunięto regułę pary.");
        return "redirect:/admin/config/pair-rules";
    }

    private PairRuleConfigForm formFrom(ConfigPairRule rule) {
        PairRuleConfigForm form = new PairRuleConfigForm();
        form.setParticipant1Id(rule.getParticipant1().getId());
        form.setParticipant2Id(rule.getParticipant2().getId());
        form.setCompatibilityMode(rule.getCompatibilityMode());
        form.setScorePoints(rule.getScorePoints());
        return form;
    }

    private void populateFormModel(Model model, PairRuleConfigForm form, boolean editing, Long ruleId) {
        model.addAttribute("form", form);
        model.addAttribute("editing", editing);
        model.addAttribute("ruleId", ruleId);
        model.addAttribute("participants", participantService.findAll());
        model.addAttribute("compatibilityModes", ConfigPairCompatibilityMode.values());
        model.addAttribute("compatibilityModeLabels", COMPATIBILITY_MODE_LABELS);
    }
}
