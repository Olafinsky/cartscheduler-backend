package org.cartscheduler.controller.web;

import jakarta.validation.Valid;
import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.service.AdminParticipantService;
import org.cartscheduler.service.FieldValidationException;
import org.cartscheduler.web.form.ParticipantForm;
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
public class AdminParticipantController {
    private final AdminParticipantService participantService;

    @GetMapping("/admin/participants")
    public String index(Model model) {
        model.addAttribute("participants", participantService.findAll());
        return "admin/participants/index";
    }

    @GetMapping("/admin/participants/new")
    public String createForm(Model model) {
        populateFormModel(model, new ParticipantForm(), false, null);
        return "admin/participants/form";
    }

    @PostMapping("/admin/participants")
    public String create(
            @Valid @ModelAttribute("form") ParticipantForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, false, null);
            return "admin/participants/form";
        }
        try {
            participantService.create(form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, false, null);
            return "admin/participants/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Dodano uczestnika.");
        return "redirect:/admin/participants";
    }

    @GetMapping("/admin/participants/{id}")
    public String view(@PathVariable long id, Model model) {
        Participant participant = participantService.getRequired(id);
        model.addAttribute("participant", participant);
        model.addAttribute("assignedParticipants", participantService.findAssignedParticipantsForAgent(id));
        return "admin/participants/view";
    }

    @GetMapping("/admin/participants/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        Participant participant = participantService.getRequired(id);
        ParticipantForm form = new ParticipantForm();
        form.setName(participant.getName());
        form.setEmail(participant.getEmail());
        form.setEnabled(participant.isEnabled());
        form.setNewScheduleAutoassignmentEnabled(participant.isNewScheduleAutoassignmentEnabled());
        form.setNewScheduleAutoinvitementEnabled(participant.isNewScheduleAutoinvitementEnabled());
        form.setAllowPairSelection(participant.isAllowPairSelection());
        form.setAllowAlternativeGroupSelection(participant.isAllowAlternativeGroupSelection());
        form.setAssignedParticipantIds(new LinkedHashSet<>(participantService.findAssignedParticipantIdsForAgent(id)));
        form.getAssignedParticipantIds().add(id);
        populateFormModel(model, form, true, id);
        return "admin/participants/form";
    }

    @PostMapping("/admin/participants/{id}/edit")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") ParticipantForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, true, id);
            return "admin/participants/form";
        }
        try {
            participantService.update(id, form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, true, id);
            return "admin/participants/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Zapisano zmiany uczestnika.");
        return "redirect:/admin/participants";
    }

    @PostMapping("/admin/participants/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        participantService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Usunięto uczestnika.");
        return "redirect:/admin/participants";
    }

    private void populateFormModel(Model model, ParticipantForm form, boolean editing, Long participantId) {
        model.addAttribute("form", form);
        model.addAttribute("editing", editing);
        model.addAttribute("participantId", participantId);
        model.addAttribute("allParticipants", participantService.findAll());
    }
}
