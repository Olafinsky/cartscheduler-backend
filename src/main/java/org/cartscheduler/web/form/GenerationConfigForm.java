package org.cartscheduler.web.form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerationConfigForm {

    @NotNull(message = "Podaj liczbę punktów za pusty wiersz.")
    private Integer emptyRowPoints = 0;

    @NotNull(message = "Podaj liczbę punktów za pełny wiersz obok niepełnego.")
    private Integer fullRowAdjacentToIncompletePoints = 0;

    @NotNull(message = "Podaj liczbę punktów za pełny wiersz.")
    private Integer fullRowNotAdjacentToIncompletePoints = 0;

}
