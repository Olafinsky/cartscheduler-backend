package org.cartscheduler.web.form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.cartscheduler.entity.ConfigPairCompatibilityMode;

@Getter
@Setter
public class PairRuleConfigForm {

    @NotNull(message = "Wybierz pierwszego uczestnika.")
    private Long participant1Id;

    @NotNull(message = "Wybierz drugiego uczestnika.")
    private Long participant2Id;

    @NotNull(message = "Wybierz tryb zgodności pary.")
    private ConfigPairCompatibilityMode compatibilityMode = ConfigPairCompatibilityMode.ANY;

    @NotNull(message = "Podaj liczbę punktów dla pary.")
    private Integer scorePoints = 0;
}
