package org.cartscheduler;

import org.cartscheduler.service.ScheduleGenerationResult;
import org.cartscheduler.service.ScheduleGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CartschedulerApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(CartschedulerApplication.class);

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(CartschedulerApplication.class, args);
		String scheduleIdArgument = context.getEnvironment().getProperty("schedule-generation.schedule-id");
		if (scheduleIdArgument == null || scheduleIdArgument.isBlank()) {
			return;
		}

		try {
			long scheduleId = Long.parseLong(scheduleIdArgument);
			ScheduleGenerationResult result = context.getBean(ScheduleGenerationService.class).generate(scheduleId);
			LOGGER.info(
					"Generated schedule {} and assigned {} participant cells",
					result.scheduleId(),
					result.assignedParticipantCells()
			);
		} finally {
			SpringApplication.exit(context);
		}
	}

}
