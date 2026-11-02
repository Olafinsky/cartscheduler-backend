package org.cartscheduler.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.request_filter.JwtAuthFilter;
import org.cartscheduler.service.AdminParticipantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminParticipantController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminParticipantService participantService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void rendersAssignedParticipantCheckboxListInNewParticipantForm() throws Exception {
        Participant existingParticipant = new Participant();
        existingParticipant.setId(5L);
        existingParticipant.setName("Anna Kowalska");
        existingParticipant.setEmail("anna@example.test");
        given(participantService.findAll()).willReturn(List.of(existingParticipant));

        mockMvc.perform(get("/admin/participants/new")
                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Przypisani uczestnicy")))
                .andExpect(content().string(containsString("autocomplete=\"off\"")))
                .andExpect(content().string(containsString("Anna Kowalska (anna@example.test)")));
    }
}
