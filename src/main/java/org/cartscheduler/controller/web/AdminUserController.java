package org.cartscheduler.controller.web;

import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.AdminUser;
import org.cartscheduler.impl.AdminUserDetails;
import org.cartscheduler.service.AdminUserService;
import org.cartscheduler.service.FieldValidationException;
import org.cartscheduler.web.form.AdminUserForm;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class AdminUserController {
    private final AdminUserService userService;

    @GetMapping("/admin/users")
    public String index(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users/index";
    }

    @GetMapping("/admin/users/new")
    public String createForm(Model model) {
        populateFormModel(model, new AdminUserForm(), false, null);
        return "admin/users/form";
    }

    @PostMapping("/admin/users")
    public String create(
            @Valid @ModelAttribute("form") AdminUserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        validateUsername(form, bindingResult);
        validatePassword(form, bindingResult, true);
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, false, null);
            return "admin/users/form";
        }
        try {
            userService.create(form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            populateFormModel(model, form, false, null);
            return "admin/users/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Dodano użytkownika.");
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{id}")
    public String view(@PathVariable long id, Model model) {
        model.addAttribute("user", userService.getRequired(id));
        return "admin/users/view";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        AdminUser user = userService.getRequired(id);
        AdminUserForm form = new AdminUserForm();
        form.setUsername(user.getUsername());
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        populateFormModel(model, form, true, id);
        return "admin/users/form";
    }

    @PostMapping("/admin/users/{id}/edit")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") AdminUserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        validateUsername(form, bindingResult);
        validatePassword(form, bindingResult, false);
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, true, id);
            return "admin/users/form";
        }
        try {
            userService.update(id, form);
        } catch (FieldValidationException exception) {
            bindingResult.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            populateFormModel(model, form, true, id);
            return "admin/users/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Zapisano zmiany użytkownika.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String delete(
            @PathVariable long id,
            @AuthenticationPrincipal AdminUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        if (currentUser != null && currentUser.getId() == id) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nie można usunąć aktualnie zalogowanego użytkownika.");
            return "redirect:/admin/users";
        }
        userService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Usunięto użytkownika.");
        return "redirect:/admin/users";
    }

    private void populateFormModel(Model model, AdminUserForm form, boolean editing, Long userId) {
        model.addAttribute("form", form);
        model.addAttribute("editing", editing);
        model.addAttribute("userId", userId);
    }

    private void validatePassword(AdminUserForm form, BindingResult bindingResult, boolean passwordRequired) {
        boolean passwordEntered = form.getPassword() != null && !form.getPassword().isBlank();
        boolean confirmationEntered = form.getPasswordConfirmation() != null
                && !form.getPasswordConfirmation().isBlank();
        if (passwordRequired && !passwordEntered) {
            bindingResult.rejectValue("password", "required", "Hasło jest wymagane.");
        }
        if ((!passwordEntered && confirmationEntered)
                || (passwordEntered && !Objects.equals(form.getPassword(), form.getPasswordConfirmation()))) {
            bindingResult.rejectValue("passwordConfirmation", "match", "Potwierdzenie hasła musi być takie samo jak hasło.");
        }
    }

    private void validateUsername(AdminUserForm form, BindingResult bindingResult) {
        String username = form.getUsername();
        // The migration intentionally seeds the required "admin" account. It is
        // allowed as a backwards-compatible exception so its name/e-mail can be edited.
        if (username == null || username.strip().length() < 8) {
            if (username == null || !"admin".equalsIgnoreCase(username.strip())) {
                bindingResult.rejectValue("username", "size", "Nazwa użytkownika musi mieć od 8 do 128 znaków.");
            }
        }
    }
}
