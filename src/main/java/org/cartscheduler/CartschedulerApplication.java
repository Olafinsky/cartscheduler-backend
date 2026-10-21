package org.cartscheduler;

import org.cartscheduler.service.ScheduleGenerationResult;
import org.cartscheduler.service.ScheduleGenerationPreview;
import org.cartscheduler.service.ScheduleGenerationService;
import org.cartscheduler.service.ScheduleWorkbookExportService;
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
			boolean dryRun = context.getEnvironment()
					.getProperty("schedule-generation.dry-run", Boolean.class, false);
			if (dryRun) {
				ScheduleGenerationPreview preview = context.getBean(ScheduleGenerationService.class).preview(scheduleId);
				LOGGER.info(
						"Dry-run generated schedule {} with {} assigned participant cells and score {}; no changes were saved",
						preview.scheduleId(),
						preview.assignedParticipantCells(),
						preview.score()
				);
				return;
			}
			ScheduleGenerationResult result = context.getBean(ScheduleGenerationService.class).generate(scheduleId);
			var exportedWorkbook = context.getBean(ScheduleWorkbookExportService.class).export(scheduleId);
			LOGGER.info(
					"Generated schedule {} and assigned {} participant cells; exported XLSX to {}",
					result.scheduleId(),
					result.assignedParticipantCells(),
					exportedWorkbook
			);
		} finally {
			SpringApplication.exit(context);
		}
	}

}
