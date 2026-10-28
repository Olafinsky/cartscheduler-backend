package org.cartscheduler.config;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cartscheduler.controller.web.LoginController;
import org.cartscheduler.entity.AdminUser;
import org.cartscheduler.impl.AdminUserDetails;
import org.cartscheduler.request_filter.JwtAuthFilter;
import org.cartscheduler.service.AdminUserDetailsService;
import org.cartscheduler.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LoginController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, PasswordConfiguration.class})
class AdminSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private AdminUserDetailsService adminUserDetailsService;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    void redirectsUnauthenticatedBrowserRequestsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void authenticatesAdministratorWithSessionFormLoginAndRecordsLastLogin() throws Exception {
        AdminUser user = new AdminUser();
        user.setId(17L);
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("Admin123"));
        given(adminUserDetailsService.loadUserByUsername("admin"))
                .willReturn(new AdminUserDetails(user));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "Admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(adminUserService).recordSuccessfulLogin(17L);
    }
}
