package org.cartscheduler.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.cartscheduler.service.generation.ScheduleGenerationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a human-readable XLSX representation of a persisted schedule.
 *
 * <p>This service intentionally only reads schedule data. It is separate from
 * {@link ScheduleGenerationService}, which is responsible for assigning
 * participants.</p>
 */
@Service
public class ScheduleWorkbookExportService {
    private static final List<IndexedColors> DAY_COLORS = List.of(
            IndexedColors.LIGHT_GREEN,
            IndexedColors.LIGHT_TURQUOISE,
            IndexedColors.ROSE,
            IndexedColors.PALE_BLUE,
            IndexedColors.LIGHT_ORANGE,
            IndexedColors.LAVENDER,
            IndexedColors.TAN,
            IndexedColors.LIGHT_CORNFLOWER_BLUE
    );

    private static final int HOUR_COLUMN_WIDTH = 12 * 256;
    private static final int PARTICIPANT_COLUMN_WIDTH = 22 * 256;

    private final ScheduleRepository scheduleRepository;
    private final ScheduleDayRepository scheduleDayRepository;
    private final Path outputDirectory;

    public ScheduleWorkbookExportService(
            ScheduleRepository scheduleRepository,
            ScheduleDayRepository scheduleDayRepository,
            @Value("${schedule-export.output-directory:build/generated-schedules}") String outputDirectory
    ) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleDayRepository = scheduleDayRepository;
        if (outputDirectory == null || outputDirectory.isBlank()) {
            throw new IllegalArgumentException("schedule-export.output-directory must not be blank");
        }
        this.outputDirectory = Path.of(outputDirectory).toAbsolutePath().normalize();
    }

    /**
     * Exports the current assignments of one schedule to a deterministic file
     * named {@code schedule-{id}.xlsx}.
     *
     * @return absolute path of the saved workbook
     */
    @Transactional(readOnly = true)
    public Path export(long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleWorkbookExportException("Schedule " + scheduleId + " does not exist"));
        List<ExportDay> days = scheduleDayRepository.findForScheduleWithEntries(scheduleId).stream()
                .sorted(scheduleDayComparator())
                .map(day -> new ExportDay(day, entriesByHour(day)))
                .toList();

        Path outputFile = outputDirectory.resolve("schedule-" + scheduleId + ".xlsx");
        try {
            Files.createDirectories(outputDirectory);
            try (XSSFWorkbook workbook = new XSSFWorkbook();
                    OutputStream output = Files.newOutputStream(
                            outputFile,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    )) {
                workbook.getProperties().getCoreProperties().setTitle("Grafik - " + schedule.getName());
                writeWorkbook(workbook, schedule, days);
                workbook.write(output);
            }
        } catch (IOException exception) {
            throw new ScheduleWorkbookExportException(
                    "Could not export schedule " + scheduleId + " to " + outputFile,
                    exception
            );
        }
        return outputFile;
    }

    private void writeWorkbook(XSSFWorkbook workbook, Schedule schedule, List<ExportDay> days) {
        Sheet sheet = workbook.createSheet("Grafik");
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(1, 1);
        sheet.setColumnWidth(0, HOUR_COLUMN_WIDTH);

        if (days.isEmpty()) {
            CellStyle messageStyle = createMessageStyle(workbook);
            Cell message = sheet.createRow(0).createCell(0);
            message.setCellValue("Brak dni w grafiku: " + schedule.getName());
            message.setCellStyle(messageStyle);
            return;
        }

        CellStyle hourHeaderStyle = createHourHeaderStyle(workbook);
        CellStyle hourStyle = createHourStyle(workbook);
        CellStyle emptyParticipantStyle = createEmptyParticipantStyle(workbook);
        List<CellStyle> participantStyles = participantStyles(workbook, days.size());
        RowStyles rowStyles = new RowStyles(hourHeaderStyle, hourStyle, emptyParticipantStyle, participantStyles);

        writeHeader(sheet, days, workbook, rowStyles);
        writeEntries(sheet, days, rowStyles);
    }

    private List<CellStyle> participantStyles(XSSFWorkbook workbook, int dayCount) {
        List<CellStyle> styles = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            styles.add(createParticipantStyle(workbook, colorFor(dayIndex)));
        }
        return List.copyOf(styles);
    }

    private void writeHeader(Sheet sheet, List<ExportDay> days, XSSFWorkbook workbook, RowStyles rowStyles) {
        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        header.setHeightInPoints(26);

        Cell hourHeader = header.createCell(0);
        hourHeader.setCellValue("Godzina");
        hourHeader.setCellStyle(rowStyles.hourHeaderStyle());

        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            int firstDayColumn = 1 + dayIndex * 2;
            CellStyle headerStyle = createDayHeaderStyle(workbook, colorFor(dayIndex));

            Cell headerCell = header.createCell(firstDayColumn);
            headerCell.setCellValue(days.get(dayIndex).day().getName());
            headerCell.setCellStyle(headerStyle);
            header.createCell(firstDayColumn + 1).setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, firstDayColumn, firstDayColumn + 1));
            sheet.setColumnWidth(firstDayColumn, PARTICIPANT_COLUMN_WIDTH);
            sheet.setColumnWidth(firstDayColumn + 1, PARTICIPANT_COLUMN_WIDTH);
        }
    }

    private void writeEntries(Sheet sheet, List<ExportDay> days, RowStyles rowStyles) {
        SortedSet<Short> hours = new TreeSet<>();
        for (ExportDay day : days) {
            hours.addAll(day.entriesByHour().keySet());
        }

        int rowIndex = 1;
        for (short hour : hours) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(21);

            Cell hourCell = row.createCell(0);
            hourCell.setCellValue(hour / 24d);
            hourCell.setCellStyle(rowStyles.hourStyle());

            for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
                int firstDayColumn = 1 + dayIndex * 2;
                ScheduleEntry entry = days.get(dayIndex).entriesByHour().get(hour);
                CellStyle participantStyle = rowStyles.participantStyles().get(dayIndex);
                writeParticipantCell(
                        row.createCell(firstDayColumn),
                        entry == null ? null : entry.getParticipant1(),
                        participantStyle,
                        rowStyles.emptyParticipantStyle()
                );
                writeParticipantCell(
                        row.createCell(firstDayColumn + 1),
                        entry == null ? null : entry.getParticipant2(),
                        participantStyle,
                        rowStyles.emptyParticipantStyle()
                );
            }
        }
    }

    private void writeParticipantCell(
            Cell cell,
            Participant participant,
            CellStyle participantStyle,
            CellStyle emptyParticipantStyle
    ) {
        if (participant == null) {
            cell.setCellStyle(emptyParticipantStyle);
            return;
        }
        cell.setCellValue(participant.getName());
        cell.setCellStyle(participantStyle);
    }

    private Map<Short, ScheduleEntry> entriesByHour(ScheduleDay day) {
        Map<Short, ScheduleEntry> entries = new HashMap<>();
        List<ScheduleEntry> scheduleEntries = day.getScheduleEntries() == null
                ? List.of()
                : day.getScheduleEntries();
        for (ScheduleEntry entry : scheduleEntries) {
            ScheduleEntry previous = entries.putIfAbsent(entry.getHour(), entry);
            if (previous != null) {
                throw new ScheduleWorkbookExportException(
                        "Schedule day " + day.getId() + " contains more than one entry for hour " + entry.getHour()
                );
            }
        }
        return Map.copyOf(entries);
    }

    private Comparator<ScheduleDay> scheduleDayComparator() {
        return Comparator
                .comparing(this::dateFromName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(ScheduleDay::getDayOfWeek)
                .thenComparingLong(ScheduleDay::getId);
    }

    private LocalDate dateFromName(ScheduleDay day) {
        if (day.getScheduleDate() != null) {
            return day.getScheduleDate();
        }
        String name = day.getName();
        if (name == null || name.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(name.substring(0, 10));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private IndexedColors colorFor(int dayIndex) {
        return DAY_COLORS.get(dayIndex % DAY_COLORS.size());
    }

    private CellStyle createHourHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font(workbook, true, 11));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(style);
        return style;
    }

    private CellStyle createHourStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font(workbook, true, 11));
        style.setDataFormat(workbook.createDataFormat().getFormat("hh:mm"));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(style);
        return style;
    }

    private CellStyle createDayHeaderStyle(XSSFWorkbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font(workbook, true, 12));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(style);
        return style;
    }

    private CellStyle createParticipantStyle(XSSFWorkbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font(workbook, false, 10));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(style);
        return style;
    }

    private CellStyle createEmptyParticipantStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font(workbook, false, 10));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(style);
        return style;
    }

    private CellStyle createMessageStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(font(workbook, true, 11));
        return style;
    }

    private Font font(XSSFWorkbook workbook, boolean bold, int fontSize) {
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) fontSize);
        font.setBold(bold);
        return font;
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        short borderColor = IndexedColors.GREY_50_PERCENT.getIndex();
        style.setTopBorderColor(borderColor);
        style.setBottomBorderColor(borderColor);
        style.setLeftBorderColor(borderColor);
        style.setRightBorderColor(borderColor);
    }

    private record ExportDay(ScheduleDay day, Map<Short, ScheduleEntry> entriesByHour) {
    }

    private record RowStyles(
            CellStyle hourHeaderStyle,
            CellStyle hourStyle,
            CellStyle emptyParticipantStyle,
            List<CellStyle> participantStyles
    ) {
    }
}
