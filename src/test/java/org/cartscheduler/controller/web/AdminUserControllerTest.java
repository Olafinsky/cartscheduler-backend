package org.cartscheduler.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cartscheduler.request_filter.JwtAuthFilter;
import org.cartscheduler.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService userService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void rendersNewUserFormWithAddAction() throws Exception {
        mockMvc.perform(get("/admin/users/new")
                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nowy użytkownik")))
                .andExpect(content().string(containsString("autocomplete=\"off\"")))
                .andExpect(content().string(containsString(">Dodaj<")));
    }

    @Test
    void redisplaysInvalidFormWithFieldValidationMessages() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"))
                        .param("username", "short")
                        .param("name", "Admin Test")
                        .param("email", "not-an-email")
                        .param("password", "weak")
                        .param("passwordConfirmation", "other"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nazwa użytkownika musi mieć od 8 do 128 znaków.")))
                .andExpect(content().string(containsString("Podaj poprawny adres e-mail.")))
                .andExpect(content().string(containsString("Hasło musi mieć co najmniej 8 znaków")));
    }
}
