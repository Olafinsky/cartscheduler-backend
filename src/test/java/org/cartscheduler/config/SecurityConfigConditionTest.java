package org.cartscheduler.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SecurityConfigConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SecurityConfig.class);

    @Test
    void doesNotRegisterHttpSecurityChainsForNonWebConsoleApplication() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(SecurityConfig.class);
            assertThat(context).doesNotHaveBean("apiSecurityFilterChain");
            assertThat(context).doesNotHaveBean("adminSecurityFilterChain");
        });
    }
}
