package org.cartscheduler.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigParticipantRule;
import org.cartscheduler.service.AdminParticipantRuleConfigService;
import org.cartscheduler.service.AdminParticipantService;
import org.cartscheduler.service.FieldValidationException;
import org.cartscheduler.web.form.ParticipantRuleConfigForm;
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
public class AdminParticipantRuleConfigController {
    private final AdminParticipantRuleConfigService participantRuleConfigService;
    private final AdminParticipantService participantService;

    @GetMapping("/admin/config/participant-rules")
    public String index(Model model) {
        model.addAttribute("rules", participantRuleConfigService.findAll());
        return "admin/config/participant-rules/index";
    }

    @GetMapping("/admin/config/participant-rules/new")
    public String createForm(Model model) {
        populateFormModel(model, new ParticipantRuleConfigForm(), false, null);
        return "admin/config/participant-rules/form";
    }

    @PostMapping("/admin/config/participant-rules")
    public String create(
            @Valid @ModelAttribute("form") ParticipantRuleConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, false, null);
            return "admin/config/participant-rules/form";
        }
        try {
            participantRuleConfigService.create(form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, false, null);
            return "admin/config/participant-rules/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Dodano regułę uczestnika.");
        return "redirect:/admin/config/participant-rules";
    }

    @GetMapping("/admin/config/participant-rules/{id}")
    public String view(@PathVariable long id, Model model) {
        model.addAttribute("rule", participantRuleConfigService.getRequired(id));
        return "admin/config/participant-rules/view";
    }

    @GetMapping("/admin/config/participant-rules/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        ConfigParticipantRule rule = participantRuleConfigService.getRequired(id);
        ParticipantRuleConfigForm form = formFrom(rule);
        populateFormModel(model, form, true, id);
        return "admin/config/participant-rules/form";
    }

    @PostMapping("/admin/config/participant-rules/{id}/edit")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") ParticipantRuleConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, true, id);
            return "admin/config/participant-rules/form";
        }
        try {
            participantRuleConfigService.update(id, form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, true, id);
            return "admin/config/participant-rules/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Zapisano regułę uczestnika.");
        return "redirect:/admin/config/participant-rules";
    }

    @PostMapping("/admin/config/participant-rules/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        participantRuleConfigService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Usunięto regułę uczestnika.");
        return "redirect:/admin/config/participant-rules";
    }

    private ParticipantRuleConfigForm formFrom(ConfigParticipantRule rule) {
        ParticipantRuleConfigForm form = new ParticipantRuleConfigForm();
        form.setParticipantId(rule.getParticipant().getId());
        form.setServiceLengthMin(rule.getServiceLengthMin());
        form.setServiceLengthMax(rule.getServiceLengthMax());
        form.setBreakLengthMin(rule.getBreakLengthMin());
        form.setBreakLengthMax(rule.getBreakLengthMax());
        form.setPrivilegePoints(rule.getPrivilegePoints() == null ? 0 : rule.getPrivilegePoints());
        return form;
    }

    private void populateFormModel(Model model, ParticipantRuleConfigForm form, boolean editing, Long ruleId) {
        model.addAttribute("form", form);
        model.addAttribute("editing", editing);
        model.addAttribute("ruleId", ruleId);
        model.addAttribute("participants", participantService.findAll());
    }
}
