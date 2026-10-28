package org.cartscheduler.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleForm {

    @NotBlank(message = "Nazwa grafiku jest wymagana.")
    @Size(max = 256, message = "Nazwa grafiku może mieć maksymalnie 256 znaków.")
    private String name;

    /** IDs of participants granted direct access to the schedule. */
    private Set<Long> participantIds = new LinkedHashSet<>();
}
