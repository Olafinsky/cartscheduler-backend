package org.cartscheduler.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.service.AdminScheduleService;
import org.cartscheduler.service.FieldValidationException;
import org.cartscheduler.web.form.ScheduleForm;
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
public class AdminScheduleController {
    private final AdminScheduleService scheduleService;

    @GetMapping("/admin/schedules")
    public String index(Model model) {
        model.addAttribute("schedules", scheduleService.findAll());
        return "admin/schedules/index";
    }

    @GetMapping("/admin/schedules/{id}")
    public String view(@PathVariable long id, Model model) {
        model.addAttribute("schedule", scheduleService.getDetails(id));
        model.addAttribute("accessibleParticipants", scheduleService.findAccessibleParticipants(id));
        return "admin/schedules/view";
    }

    @GetMapping("/admin/schedules/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        Schedule schedule = scheduleService.getRequired(id);
        ScheduleForm form = new ScheduleForm();
        form.setName(schedule.getName());
        form.setParticipantIds(scheduleService.findAccessibleParticipantIds(id));
        populateFormModel(model, form, id);
        return "admin/schedules/form";
    }

    @PostMapping("/admin/schedules/{id}/edit")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") ScheduleForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, id);
            return "admin/schedules/form";
        }
        try {
            scheduleService.update(id, form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, id);
            return "admin/schedules/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Zapisano zmiany grafiku.");
        return "redirect:/admin/schedules";
    }

    @PostMapping("/admin/schedules/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        scheduleService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Usunięto grafik.");
        return "redirect:/admin/schedules";
    }

    private void populateFormModel(Model model, ScheduleForm form, long scheduleId) {
        model.addAttribute("form", form);
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("allParticipants", scheduleService.findAllParticipants());
    }
}
