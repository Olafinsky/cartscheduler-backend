package org.cartscheduler.controller.web;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigScheduleDay;
import org.cartscheduler.service.AdminScheduleDayConfigService;
import org.cartscheduler.service.FieldValidationException;
import org.cartscheduler.web.form.ScheduleDayConfigForm;
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
public class AdminScheduleDayConfigController {
    private static final Map<Short, String> WEEKDAY_NAMES = Map.of(
            (short) 1, "Poniedziałek",
            (short) 2, "Wtorek",
            (short) 3, "Środa",
            (short) 4, "Czwartek",
            (short) 5, "Piątek",
            (short) 6, "Sobota",
            (short) 7, "Niedziela"
    );

    private final AdminScheduleDayConfigService scheduleDayConfigService;

    @GetMapping("/admin/config/schedule-days")
    public String index(Model model) {
        model.addAttribute("scheduleDays", scheduleDayConfigService.findAll());
        model.addAttribute("weekdayNames", WEEKDAY_NAMES);
        return "admin/config/schedule-days/index";
    }

    @GetMapping("/admin/config/schedule-days/new")
    public String createForm(Model model) {
        populateFormModel(model, new ScheduleDayConfigForm(), false, null);
        return "admin/config/schedule-days/form";
    }

    @PostMapping("/admin/config/schedule-days")
    public String create(
            @Valid @ModelAttribute("form") ScheduleDayConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, false, null);
            return "admin/config/schedule-days/form";
        }
        try {
            scheduleDayConfigService.create(form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, false, null);
            return "admin/config/schedule-days/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Dodano konfigurację dnia grafiku.");
        return "redirect:/admin/config/schedule-days";
    }

    @GetMapping("/admin/config/schedule-days/{id}")
    public String view(@PathVariable long id, Model model) {
        model.addAttribute("scheduleDay", scheduleDayConfigService.getRequired(id));
        model.addAttribute("weekdayNames", WEEKDAY_NAMES);
        return "admin/config/schedule-days/view";
    }

    @GetMapping("/admin/config/schedule-days/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        ConfigScheduleDay scheduleDay = scheduleDayConfigService.getRequired(id);
        populateFormModel(model, formFrom(scheduleDay), true, id);
        return "admin/config/schedule-days/form";
    }

    @PostMapping("/admin/config/schedule-days/{id}/edit")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") ScheduleDayConfigForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, true, id);
            return "admin/config/schedule-days/form";
        }
        try {
            scheduleDayConfigService.update(id, form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            populateFormModel(model, form, true, id);
            return "admin/config/schedule-days/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Zapisano konfigurację dnia grafiku.");
        return "redirect:/admin/config/schedule-days";
    }

    @PostMapping("/admin/config/schedule-days/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        scheduleDayConfigService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Usunięto konfigurację dnia grafiku.");
        return "redirect:/admin/config/schedule-days";
    }

    private ScheduleDayConfigForm formFrom(ConfigScheduleDay scheduleDay) {
        ScheduleDayConfigForm form = new ScheduleDayConfigForm();
        form.setDayOfWeek(scheduleDay.getDayOfWeek());
        form.setHourFrom(scheduleDay.getHourFrom());
        form.setHourTo(scheduleDay.getHourTo());
        form.setActive(scheduleDay.isActive());
        return form;
    }

    private void populateFormModel(Model model, ScheduleDayConfigForm form, boolean editing, Long scheduleDayId) {
        model.addAttribute("form", form);
        model.addAttribute("editing", editing);
        model.addAttribute("scheduleDayId", scheduleDayId);
        model.addAttribute("weekdayNames", WEEKDAY_NAMES);
    }
}
