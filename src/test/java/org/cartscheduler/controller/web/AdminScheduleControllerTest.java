package org.cartscheduler.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.request_filter.JwtAuthFilter;
import org.cartscheduler.service.AdminScheduleService;
import org.cartscheduler.web.model.ScheduleDetailsView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminScheduleService scheduleService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void rendersEditFormWithExistingParticipantAccess() throws Exception {
        Schedule schedule = new Schedule();
        schedule.setId(7L);
        schedule.setName("Grafik listopadowy");
        Participant anna = new Participant();
        anna.setId(5L);
        anna.setName("Anna Kowalska");
        anna.setEmail("anna@example.test");
        given(scheduleService.getRequired(7L)).willReturn(schedule);
        given(scheduleService.findAccessibleParticipantIds(7L)).willReturn(Set.of(5L));
        given(scheduleService.findAllParticipants()).willReturn(List.of(anna));

        mockMvc.perform(get("/admin/schedules/7/edit")
                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edycja grafiku")))
                .andExpect(content().string(containsString("Dostęp uczestników")))
                .andExpect(content().string(containsString("autocomplete=\"off\"")))
                .andExpect(content().string(containsString("Anna Kowalska (anna@example.test)")));
    }

    @Test
    void rendersParticipantsAssignedToTheScheduleInDetails() throws Exception {
        Participant anna = new Participant();
        anna.setId(5L);
        anna.setName("Anna Kowalska");
        anna.setEmail("anna@example.test");
        given(scheduleService.getDetails(7L))
                .willReturn(new ScheduleDetailsView(7L, "Grafik listopadowy", List.of()));
        given(scheduleService.findAccessibleParticipants(7L)).willReturn(List.of(anna));

        mockMvc.perform(get("/admin/schedules/7")
                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Przypisani uczestnicy")))
                .andExpect(content().string(containsString("Anna Kowalska (anna@example.test)")));
    }
}
