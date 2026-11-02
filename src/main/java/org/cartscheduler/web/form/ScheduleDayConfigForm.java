package org.cartscheduler.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleDayConfigForm {

    @NotNull(message = "Wybierz dzień tygodnia.")
    @Min(value = 1, message = "Dzień tygodnia musi mieścić się w zakresie od 1 do 7.")
    @Max(value = 7, message = "Dzień tygodnia musi mieścić się w zakresie od 1 do 7.")
    private Short dayOfWeek;

    @NotNull(message = "Podaj godzinę rozpoczęcia.")
    @Min(value = 0, message = "Godzina musi mieścić się w zakresie od 0 do 23.")
    @Max(value = 23, message = "Godzina musi mieścić się w zakresie od 0 do 23.")
    private Short hourFrom;

    @NotNull(message = "Podaj godzinę zakończenia.")
    @Min(value = 0, message = "Godzina musi mieścić się w zakresie od 0 do 23.")
    @Max(value = 23, message = "Godzina musi mieścić się w zakresie od 0 do 23.")
    private Short hourTo;

    private boolean active = true;
}
