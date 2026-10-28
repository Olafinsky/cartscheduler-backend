package org.cartscheduler.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserForm {

    @NotBlank(message = "Nazwa użytkownika jest wymagana.")
    @Size(max = 128, message = "Nazwa użytkownika może mieć maksymalnie 128 znaków.")
    private String username;

    @NotBlank(message = "Imię i nazwisko jest wymagane.")
    @Size(max = 128, message = "Imię i nazwisko może mieć maksymalnie 128 znaków.")
    private String name;

    /* An empty password is permitted only during an edit, where it means "do not change". */
    @Pattern(
            regexp = "^$|^(?=.*[a-z])(?=.*[A-Z]).{8,}$",
            message = "Hasło musi mieć co najmniej 8 znaków oraz małą i wielką literę."
    )
    private String password = "";

    private String passwordConfirmation = "";

    @NotBlank(message = "Adres e-mail jest wymagany.")
    @Email(message = "Podaj poprawny adres e-mail.")
    @Size(min = 8, max = 128, message = "Adres e-mail musi mieć od 8 do 128 znaków.")
    private String email;
}
