package org.cartscheduler.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.cartscheduler.entity.ConfigCandidateAssignmentScore;
import org.cartscheduler.entity.ConfigGeneration;
import org.cartscheduler.entity.ConfigPairAssignmentScore;
import org.cartscheduler.entity.ConfigPairCompatibilityMode;
import org.cartscheduler.entity.ConfigPairRule;
import org.cartscheduler.entity.ConfigParticipantRule;
import org.cartscheduler.entity.ConfigScheduleDay;
import org.cartscheduler.entity.ConfigScheduleRestrictedDate;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.request_filter.JwtAuthFilter;
import org.cartscheduler.service.AdminCandidateAssignmentScoreConfigService;
import org.cartscheduler.service.AdminGenerationConfigService;
import org.cartscheduler.service.AdminPairAssignmentScoreConfigService;
import org.cartscheduler.service.AdminPairRuleConfigService;
import org.cartscheduler.service.AdminParticipantRuleConfigService;
import org.cartscheduler.service.AdminParticipantService;
import org.cartscheduler.service.AdminRestrictedDateService;
import org.cartscheduler.service.AdminScheduleDayConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = {
        AdminGenerationConfigController.class,
        AdminParticipantRuleConfigController.class,
        AdminPairRuleConfigController.class,
        AdminCandidateAssignmentScoreConfigController.class,
        AdminPairAssignmentScoreConfigController.class,
        AdminScheduleDayConfigController.class,
        AdminRestrictedDateController.class
})
@AutoConfigureMockMvc(addFilters = false)
class AdminConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminGenerationConfigService generationConfigService;
    @MockitoBean
    private AdminParticipantRuleConfigService participantRuleConfigService;
    @MockitoBean
    private AdminPairRuleConfigService pairRuleConfigService;
    @MockitoBean
    private AdminCandidateAssignmentScoreConfigService candidateAssignmentScoreConfigService;
    @MockitoBean
    private AdminPairAssignmentScoreConfigService pairAssignmentScoreConfigService;
    @MockitoBean
    private AdminScheduleDayConfigService scheduleDayConfigService;
    @MockitoBean
    private AdminRestrictedDateService restrictedDateService;
    @MockitoBean
    private AdminParticipantService participantService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private ConfigGeneration generation;
    private ConfigParticipantRule participantRule;
    private ConfigPairRule pairRule;
    private ConfigCandidateAssignmentScore candidateAssignmentScore;
    private ConfigPairAssignmentScore pairAssignmentScore;
    private ConfigScheduleDay scheduleDay;
    private ConfigScheduleRestrictedDate restrictedDate;

    @BeforeEach
    void setUp() {
        Participant anna = participant(2L, "Anna Kowalska");
        Participant jan = participant(3L, "Jan Nowak");
        given(participantService.findAll()).willReturn(List.of(anna, jan));

        generation = new ConfigGeneration();
        generation.setId((short) 1);
        given(generationConfigService.findAll()).willReturn(List.of(generation));
        given(generationConfigService.getRequired((short) 1)).willReturn(generation);

        participantRule = new ConfigParticipantRule();
        participantRule.setId(4L);
        participantRule.setParticipant(anna);
        participantRule.setPrivilegePoints(0);
        given(participantRuleConfigService.findAll()).willReturn(List.of(participantRule));
        given(participantRuleConfigService.getRequired(4L)).willReturn(participantRule);

        pairRule = new ConfigPairRule();
        pairRule.setId(5L);
        pairRule.setParticipant1(anna);
        pairRule.setParticipant2(jan);
        pairRule.setCompatibilityMode(ConfigPairCompatibilityMode.ANY);
        given(pairRuleConfigService.findAll()).willReturn(List.of(pairRule));
        given(pairRuleConfigService.getRequired(5L)).willReturn(pairRule);

        candidateAssignmentScore = new ConfigCandidateAssignmentScore();
        candidateAssignmentScore.setId(6L);
        candidateAssignmentScore.setAssignmentCountFrom(1);
        candidateAssignmentScore.setAssignmentCountTo(2);
        candidateAssignmentScore.setPoints(10);
        given(candidateAssignmentScoreConfigService.findAll()).willReturn(List.of(candidateAssignmentScore));
        given(candidateAssignmentScoreConfigService.getRequired(6L)).willReturn(candidateAssignmentScore);

        pairAssignmentScore = new ConfigPairAssignmentScore();
        pairAssignmentScore.setId(9L);
        pairAssignmentScore.setAssignmentCountFrom(1);
        pairAssignmentScore.setAssignmentCountTo(2);
        pairAssignmentScore.setPoints(5);
        given(pairAssignmentScoreConfigService.findAll()).willReturn(List.of(pairAssignmentScore));
        given(pairAssignmentScoreConfigService.getRequired(9L)).willReturn(pairAssignmentScore);

        scheduleDay = new ConfigScheduleDay();
        scheduleDay.setId(7L);
        scheduleDay.setDayOfWeek((short) 1);
        scheduleDay.setHourFrom((short) 8);
        scheduleDay.setHourTo((short) 16);
        scheduleDay.setActive(true);
        given(scheduleDayConfigService.findAll()).willReturn(List.of(scheduleDay));
        given(scheduleDayConfigService.getRequired(7L)).willReturn(scheduleDay);

        restrictedDate = new ConfigScheduleRestrictedDate();
        restrictedDate.setId(8L);
        restrictedDate.setRestrictedDate(LocalDate.of(2026, 12, 25));
        given(restrictedDateService.findAll()).willReturn(List.of(restrictedDate));
        given(restrictedDateService.getRequired(8L)).willReturn(restrictedDate);
    }

    @Test
    void rendersAllConfigurationListsAndForms() throws Exception {
        assertOk("/admin/config/generation");
        assertOk("/admin/config/generation/1/edit");
        assertOk("/admin/config/participant-rules");
        assertOk("/admin/config/participant-rules/new");
        assertOk("/admin/config/pair-rules");
        assertOk("/admin/config/pair-rules/new");
        assertOk("/admin/config/candidate-assignment-scores");
        assertOk("/admin/config/candidate-assignment-scores/new");
        assertOk("/admin/config/pair-assignment-scores");
        assertOk("/admin/config/pair-assignment-scores/new");
        assertOk("/admin/config/schedule-days");
        assertOk("/admin/config/schedule-days/new");
        assertOk("/admin/config/restricted-dates");
    }

    @Test
    void rendersAllConfigurationDetailViews() throws Exception {
        mockMvc.perform(getWithCsrf("/admin/config/generation/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Punkty")));
        mockMvc.perform(getWithCsrf("/admin/config/participant-rules/4"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Anna Kowalska")));
        mockMvc.perform(getWithCsrf("/admin/config/pair-rules/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dowolna")));
        mockMvc.perform(getWithCsrf("/admin/config/candidate-assignment-scores/6"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("10")));
        mockMvc.perform(getWithCsrf("/admin/config/pair-assignment-scores/9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("5")));
        mockMvc.perform(getWithCsrf("/admin/config/schedule-days/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("8")));
        mockMvc.perform(getWithCsrf("/admin/config/restricted-dates/8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("25.12.2026")));
    }

    @Test
    void usesTheNewAssignmentAndPairOccurrenceLabelsInEveryScoreView() throws Exception {
        assertContains("/admin/config/candidate-assignment-scores", "Ilość przydziałów OD");
        assertContains("/admin/config/candidate-assignment-scores", "Ilość przydziałów DO");
        assertContains("/admin/config/candidate-assignment-scores/new", "Ilość przydziałów OD");
        assertContains("/admin/config/candidate-assignment-scores/new", "Ilość przydziałów DO");
        assertContains("/admin/config/candidate-assignment-scores/6", "Ilość przydziałów DO");

        assertContains("/admin/config/pair-assignment-scores", "Konfiguracja premii za częstotliwość występowania par.");
        assertContains("/admin/config/pair-assignment-scores", "Ilość wystąpień pary DO");
        assertContains("/admin/config/pair-assignment-scores/new", "Ilość wystąpień pary OD");
        assertContains("/admin/config/pair-assignment-scores/new", "Punkty za każde wystąpienie pary");
        assertContains("/admin/config/pair-assignment-scores/9", "Ilość wystąpień pary DO");
        assertContains("/admin/config/pair-assignment-scores/9", "Punkty za każde wystąpienie pary");
    }

    @Test
    void doesNotExposeTheRemovedPairDiversityConfiguration() throws Exception {
        assertDoesNotContain("/admin/config/generation", "Różnorodność par");
        assertDoesNotContain("/admin/config/generation/1/edit", "Różnorodność par");
        assertDoesNotContain("/admin/config/generation/1", "Różnorodność par");
    }

    private void assertOk(String url) throws Exception {
        mockMvc.perform(getWithCsrf(url)).andExpect(status().isOk());
    }

    private void assertContains(String url, String expectedContent) throws Exception {
        mockMvc.perform(getWithCsrf(url))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expectedContent)));
    }

    private void assertDoesNotContain(String url, String unexpectedContent) throws Exception {
        mockMvc.perform(getWithCsrf(url))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(unexpectedContent))));
    }

    private MockHttpServletRequestBuilder getWithCsrf(String url) {
        return get(url).requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"));
    }

    private Participant participant(long id, String name) {
        Participant participant = new Participant();
        participant.setId(id);
        participant.setName(name);
        participant.setEmail(name.toLowerCase().replace(' ', '.') + "@example.test");
        return participant;
    }
}
