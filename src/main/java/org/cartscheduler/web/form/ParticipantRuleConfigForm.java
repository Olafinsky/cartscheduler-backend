package org.cartscheduler.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantRuleConfigForm {

    @NotNull(message = "Wybierz uczestnika.")
    private Long participantId;

    @Min(value = 0, message = "Długość służby nie może być ujemna.")
    @Max(value = 127, message = "Długość służby może wynosić maksymalnie 127.")
    private Short serviceLengthMin;

    @Min(value = 0, message = "Długość służby nie może być ujemna.")
    @Max(value = 127, message = "Długość służby może wynosić maksymalnie 127.")
    private Short serviceLengthMax;

    @Min(value = 0, message = "Długość przerwy nie może być ujemna.")
    @Max(value = 127, message = "Długość przerwy może wynosić maksymalnie 127.")
    private Short breakLengthMin;

    @Min(value = 0, message = "Długość przerwy nie może być ujemna.")
    @Max(value = 127, message = "Długość przerwy może wynosić maksymalnie 127.")
    private Short breakLengthMax;

    @NotNull(message = "Podaj liczbę punktów uprzywilejowania.")
    private Integer privilegePoints = 0;
}
