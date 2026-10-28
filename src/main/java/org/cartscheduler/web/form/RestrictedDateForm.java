package org.cartscheduler.web.form;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class RestrictedDateForm {
    @NotNull(message = "Wybierz datę zastrzeżoną.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate restrictedDate;
}
