package org.cartscheduler.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.cartscheduler.request_filter.JwtAuthFilter;
import org.cartscheduler.service.AdminDashboardService;
import org.cartscheduler.web.model.DashboardSummary;
import org.cartscheduler.web.model.ParticipantProposalRow;
import org.cartscheduler.web.model.ProposalCellDisplay;
import org.cartscheduler.web.model.ScheduleListItem;
import org.cartscheduler.web.model.ScheduleSummaryDay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService dashboardService;

    // The slice does not load the REST participant user-details service.
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void rendersProposalMatrixWithIconsAndAbbreviatedValues() throws Exception {
        DashboardSummary summary = new DashboardSummary(
                List.of(new ScheduleListItem(1L, "Grafik listopad", 1)),
                1L,
                "Grafik listopad",
                List.of(new ScheduleSummaryDay(10L, "Poniedziałek", LocalDate.of(2026, 11, 2))),
                List.of(new ParticipantProposalRow(
                        "Anna Kowalska",
                        Map.of(10L, List.of(new ProposalCellDisplay(
                                "7:00-8:00", "1h", null, "Qwert.", "Władysław Szczek."
                        )))
                ))
        );
        given(dashboardService.getSummary(null)).willReturn(summary);

        mockMvc.perform(get("/admin")
                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("7:00-8:00")))
                .andExpect(content().string(containsString("bi-clock")))
                .andExpect(content().string(containsString("Qwert.")))
                .andExpect(content().string(containsString("Władysław Szczek.")))
                .andExpect(content().string(containsString("summary-scroll-top")))
                .andExpect(content().string(containsString("table-striped")))
                .andExpect(content().string(containsString("summaryFontSize")))
                .andExpect(content().string(containsString("Mniejszy")));
    }
}
