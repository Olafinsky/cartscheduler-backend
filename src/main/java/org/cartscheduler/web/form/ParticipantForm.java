package org.cartscheduler.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantForm {

    @NotBlank(message = "Nazwa uczestnika jest wymagana.")
    @Size(min = 8, max = 128, message = "Nazwa uczestnika musi mieć od 8 do 128 znaków.")
    private String name;

    @NotBlank(message = "Adres e-mail jest wymagany.")
    @Email(message = "Podaj poprawny adres e-mail.")
    @Size(min = 8, max = 128, message = "Adres e-mail musi mieć od 8 do 128 znaków.")
    private String email;

    private boolean enabled = true;
    private boolean newScheduleAutoassignmentEnabled;
    private boolean newScheduleAutoinvitementEnabled;
    private boolean allowPairSelection;
    private boolean allowAlternativeGroupSelection;

    /** IDs of participants for whom this participant may submit proposals. */
    private Set<Long> assignedParticipantIds = new LinkedHashSet<>();
}
