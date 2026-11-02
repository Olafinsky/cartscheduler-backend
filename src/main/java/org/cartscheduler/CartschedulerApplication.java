package org.cartscheduler;

import org.cartscheduler.service.generation.ScheduleCalendarGenerationResult;
import org.cartscheduler.service.generation.ScheduleCalendarGenerationService;
import org.cartscheduler.service.generation.ScheduleGenerationPreview;
import org.cartscheduler.service.generation.ScheduleGenerationResult;
import org.cartscheduler.service.generation.ScheduleGenerationService;
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
		boolean calendarGenerationRequested = context.getEnvironment()
				.getProperty("schedule-calendar-generation.enabled", Boolean.class, false);
		if (!calendarGenerationRequested && (scheduleIdArgument == null || scheduleIdArgument.isBlank())) {
			return;
		}

		try {
			if (calendarGenerationRequested) {
				ScheduleCalendarGenerationResult result = context
						.getBean(ScheduleCalendarGenerationService.class)
						.generate();
				if (result.created()) {
					LOGGER.info(
							"Created calendar schedule {} for {} through {} with {} days and {} entries",
							result.scheduleName(),
							result.startDate(),
							result.endDate(),
							result.createdScheduleDays(),
							result.createdScheduleEntries()
					);
				} else {
					LOGGER.info(
							"Calendar schedule {} already exists for {} through {}; no changes were made",
							result.scheduleName(),
							result.startDate(),
							result.endDate()
					);
				}
			}

			if (scheduleIdArgument != null && !scheduleIdArgument.isBlank()) {
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
				} else {
					ScheduleGenerationResult result = context.getBean(ScheduleGenerationService.class).generate(scheduleId);
					var exportedWorkbook = context.getBean(ScheduleWorkbookExportService.class).export(scheduleId);
					LOGGER.info(
							"Generated schedule {} and assigned {} participant cells; exported XLSX to {}",
							result.scheduleId(),
							result.assignedParticipantCells(),
							exportedWorkbook
					);
				}
			}
		} finally {
			SpringApplication.exit(context);
		}
	}

}
