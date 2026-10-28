package org.cartscheduler.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.cartscheduler.entity.ConfigCandidateAvailabilityScore;
import org.cartscheduler.entity.ConfigGeneration;
import org.cartscheduler.entity.ConfigPairCompatibilityMode;
import org.cartscheduler.entity.ConfigPairRule;
import org.cartscheduler.entity.ConfigParticipantRule;
import org.cartscheduler.entity.ConfigScheduleDay;
import org.cartscheduler.entity.ConfigScheduleRestrictedDate;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.request_filter.JwtAuthFilter;
import org.cartscheduler.service.AdminCandidateAvailabilityScoreConfigService;
import org.cartscheduler.service.AdminGenerationConfigService;
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
        AdminCandidateAvailabilityScoreConfigController.class,
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
    private AdminCandidateAvailabilityScoreConfigService candidateAvailabilityScoreConfigService;
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
    private ConfigCandidateAvailabilityScore availabilityScore;
    private ConfigScheduleDay scheduleDay;
    private ConfigScheduleRestrictedDate restrictedDate;

    @BeforeEach
    void setUp() {
        Participant anna = participant(2L, "Anna Kowalska");
        Participant jan = participant(3L, "Jan Nowak");
        given(participantService.findAll()).willReturn(List.of(anna, jan));

        generation = new ConfigGeneration();
        generation.setId((short) 1);
        generation.setUniquePairPoints(0);
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

        availabilityScore = new ConfigCandidateAvailabilityScore();
        availabilityScore.setId(6L);
        availabilityScore.setAppearanceCountFrom(0);
        availabilityScore.setAppearanceCountTo(2);
        availabilityScore.setPoints(0);
        given(candidateAvailabilityScoreConfigService.findAll()).willReturn(List.of(availabilityScore));
        given(candidateAvailabilityScoreConfigService.getRequired(6L)).willReturn(availabilityScore);

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
        mockMvc.perform(getWithCsrf("/admin/config/generation"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Konfiguracja generowania")))
                .andExpect(content().string(containsString("Różnorodność par")));
        mockMvc.perform(getWithCsrf("/admin/config/generation/1/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edycja konfiguracji generowania")));

        mockMvc.perform(getWithCsrf("/admin/config/participant-rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reguły uczestników")));
        mockMvc.perform(getWithCsrf("/admin/config/participant-rules/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nowa reguła uczestnika")));

        mockMvc.perform(getWithCsrf("/admin/config/pair-rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reguły par")));
        mockMvc.perform(getWithCsrf("/admin/config/pair-rules/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nowa reguła pary")));

        mockMvc.perform(getWithCsrf("/admin/config/candidate-availability-scores"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Punktacja dostępności")));
        mockMvc.perform(getWithCsrf("/admin/config/candidate-availability-scores/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nowa punktacja dostępności")));

        mockMvc.perform(getWithCsrf("/admin/config/schedule-days"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dni grafiku")));
        mockMvc.perform(getWithCsrf("/admin/config/schedule-days/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nowy dzień grafiku")));

        mockMvc.perform(getWithCsrf("/admin/config/restricted-dates"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Daty zastrzeżone")));
    }

    @Test
    void rendersAllConfigurationDetailViews() throws Exception {
        mockMvc.perform(getWithCsrf("/admin/config/generation/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Punkty za pusty wiersz")));
        mockMvc.perform(getWithCsrf("/admin/config/participant-rules/4"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Anna Kowalska")));
        mockMvc.perform(getWithCsrf("/admin/config/pair-rules/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dowolna")));
        mockMvc.perform(getWithCsrf("/admin/config/candidate-availability-scores/6"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Od liczby wystąpień")));
        mockMvc.perform(getWithCsrf("/admin/config/schedule-days/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Poniedziałek")));
        mockMvc.perform(getWithCsrf("/admin/config/restricted-dates/8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("25.12.2026")));
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
