package org.cartscheduler.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.ConfigScheduleRestrictedDate;
import org.cartscheduler.service.AdminRestrictedDateService;
import org.cartscheduler.service.FieldValidationException;
import org.cartscheduler.web.form.RestrictedDateForm;
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
public class AdminRestrictedDateController {
    private final AdminRestrictedDateService restrictedDateService;

    @GetMapping({"/admin/config/restricted-dates", "/admin/restricted-dates"})
    public String index(Model model) {
        model.addAttribute("restrictedDates", restrictedDateService.findAll());
        return "admin/restricted-dates/index";
    }

    @GetMapping({"/admin/config/restricted-dates/new", "/admin/restricted-dates/new"})
    public String createForm(Model model) {
        populateFormModel(model, new RestrictedDateForm(), false, null);
        return "admin/restricted-dates/form";
    }

    @PostMapping({"/admin/config/restricted-dates", "/admin/restricted-dates"})
    public String create(
            @Valid @ModelAttribute("form") RestrictedDateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, false, null);
            return "admin/restricted-dates/form";
        }
        try {
            restrictedDateService.create(form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            populateFormModel(model, form, false, null);
            return "admin/restricted-dates/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Dodano datę zastrzeżoną.");
        return "redirect:/admin/config/restricted-dates";
    }

    @GetMapping({"/admin/config/restricted-dates/{id}", "/admin/restricted-dates/{id}"})
    public String view(@PathVariable long id, Model model) {
        model.addAttribute("restrictedDate", restrictedDateService.getRequired(id));
        return "admin/restricted-dates/view";
    }

    @GetMapping({"/admin/config/restricted-dates/{id}/edit", "/admin/restricted-dates/{id}/edit"})
    public String editForm(@PathVariable long id, Model model) {
        ConfigScheduleRestrictedDate restrictedDate = restrictedDateService.getRequired(id);
        RestrictedDateForm form = new RestrictedDateForm();
        form.setRestrictedDate(restrictedDate.getRestrictedDate());
        populateFormModel(model, form, true, id);
        return "admin/restricted-dates/form";
    }

    @PostMapping({"/admin/config/restricted-dates/{id}/edit", "/admin/restricted-dates/{id}/edit"})
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") RestrictedDateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, true, id);
            return "admin/restricted-dates/form";
        }
        try {
            restrictedDateService.update(id, form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            populateFormModel(model, form, true, id);
            return "admin/restricted-dates/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Zapisano zmiany daty zastrzeżonej.");
        return "redirect:/admin/config/restricted-dates";
    }

    @PostMapping({"/admin/config/restricted-dates/{id}/delete", "/admin/restricted-dates/{id}/delete"})
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        restrictedDateService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Usunięto datę zastrzeżoną.");
        return "redirect:/admin/config/restricted-dates";
    }

    private void populateFormModel(Model model, RestrictedDateForm form, boolean editing, Long restrictedDateId) {
        model.addAttribute("form", form);
        model.addAttribute("editing", editing);
        model.addAttribute("restrictedDateId", restrictedDateId);
    }
}
