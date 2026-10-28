package org.cartscheduler.web.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateAvailabilityScoreConfigForm {

    @NotNull(message = "Podaj początek zakresu.")
    @Min(value = 0, message = "Liczba wystąpień nie może być ujemna.")
    private Integer appearanceCountFrom;

    @NotNull(message = "Podaj koniec zakresu.")
    @Min(value = 0, message = "Liczba wystąpień nie może być ujemna.")
    private Integer appearanceCountTo;

    @NotNull(message = "Podaj liczbę punktów.")
    private Integer points = 0;
}
