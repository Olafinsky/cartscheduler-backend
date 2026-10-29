package org.cartscheduler.web.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Form shared by participant- and pair-assignment score bands. */
@Getter
@Setter
public class AssignmentScoreConfigForm {

    @NotNull(message = "Podaj początek zakresu.")
    @Min(value = 1, message = "Numer wystąpienia musi wynosić co najmniej 1.")
    private Integer assignmentCountFrom;

    @NotNull(message = "Podaj koniec zakresu.")
    @Min(value = 1, message = "Numer wystąpienia musi wynosić co najmniej 1.")
    private Integer assignmentCountTo;

    @NotNull(message = "Podaj liczbę punktów.")
    private Integer points = 0;
}
