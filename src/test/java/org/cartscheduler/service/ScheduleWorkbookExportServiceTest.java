package org.cartscheduler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkbookExportServiceTest {
    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleDayRepository scheduleDayRepository;

    @TempDir
    private Path outputDirectory;

    @Test
    void exportsDaysChronologicallyWithBothParticipantColumnsAndEmptySlots() throws Exception {
        Schedule schedule = schedule(9L, "October 2026");
        ScheduleDay laterDay = day(12L, "2026-10-12 (Monday)", (short) 1);
        ScheduleDay earlierDay = day(10L, "2026-10-05 (Monday)", (short) 1);
        laterDay.setScheduleEntries(List.of(
                entry((short) 7, participant(1L, "Jan Kowalski"), participant(2L, "Anna Nowak"))
        ));
        earlierDay.setScheduleEntries(List.of(
                entry((short) 7, participant(3L, "Ewa Wista"), null),
                entry((short) 8, null, participant(4L, "Piotr Bula"))
        ));
        given(scheduleRepository.findById(9L)).willReturn(Optional.of(schedule));
        given(scheduleDayRepository.findForScheduleWithEntries(9L)).willReturn(List.of(laterDay, earlierDay));
        ScheduleWorkbookExportService service = new ScheduleWorkbookExportService(
                scheduleRepository,
                scheduleDayRepository,
                outputDirectory.toString()
        );

        Path exportedFile = service.export(9L);

        assertThat(exportedFile).isEqualTo(outputDirectory.resolve("schedule-9.xlsx").toAbsolutePath().normalize());
        assertThat(exportedFile).exists();
        try (InputStream input = Files.newInputStream(exportedFile); Workbook workbook = new XSSFWorkbook(input)) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet("Grafik");
            DataFormatter formatter = new DataFormatter(Locale.ROOT);

            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Godzina");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("2026-10-05 (Monday)");
            assertThat(sheet.getRow(0).getCell(3).getStringCellValue()).isEqualTo("2026-10-12 (Monday)");
            assertThat(sheet.getNumMergedRegions()).isEqualTo(2);
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(0))).isEqualTo("07:00");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Ewa Wista");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEmpty();
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Jan Kowalski");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("Anna Nowak");
            assertThat(formatter.formatCellValue(sheet.getRow(2).getCell(0))).isEqualTo("08:00");
            assertThat(sheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("Piotr Bula");
        }
    }

    @Test
    void reportsAMissingScheduleBeforeCreatingAFile() {
        given(scheduleRepository.findById(99L)).willReturn(Optional.empty());
        ScheduleWorkbookExportService service = new ScheduleWorkbookExportService(
                scheduleRepository,
                scheduleDayRepository,
                outputDirectory.toString()
        );

        assertThatThrownBy(() -> service.export(99L))
                .isInstanceOf(ScheduleWorkbookExportException.class)
                .hasMessage("Schedule 99 does not exist");
        assertThat(Files.exists(outputDirectory.resolve("schedule-99.xlsx"))).isFalse();
    }

    private Schedule schedule(long id, String name) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setName(name);
        return schedule;
    }

    private ScheduleDay day(long id, String name, short dayOfWeek) {
        ScheduleDay day = new ScheduleDay();
        day.setId(id);
        day.setName(name);
        day.setDayOfWeek(dayOfWeek);
        return day;
    }

    private ScheduleEntry entry(short hour, Participant participant1, Participant participant2) {
        ScheduleEntry entry = new ScheduleEntry();
        entry.setName(hour + ":00");
        entry.setHour(hour);
        entry.setParticipant1(participant1);
        entry.setParticipant2(participant2);
        return entry;
    }

    private Participant participant(long id, String name) {
        Participant participant = new Participant();
        participant.setId(id);
        participant.setName(name);
        return participant;
    }
}
