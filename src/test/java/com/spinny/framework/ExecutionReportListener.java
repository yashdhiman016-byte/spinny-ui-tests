package com.spinny.framework;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Captures a screenshot for every outcome and refreshes the execution results sheet per suite run. */
public class ExecutionReportListener implements ITestListener, ISuiteListener {
    private static final DateTimeFormatter RUN_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final String[] COLUMNS = {
            "Run date/time", "Test", "Scenario", "Status", "Selected city", "City listing data shown",
            "Filter applied", "Filter/listing details shown", "Opened car", "Detail/listing URL", "Error", "Screenshot evidence"
    };
    private static final List<ReportRow> ROWS = new CopyOnWriteArrayList<>();
    private static volatile String runTimestamp;

    @Override public void onStart(ISuite suite) {
        ROWS.clear();
        runTimestamp = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).format(RUN_FORMAT);
        ScenarioReportData.clear();
    }

    @Override public void onTestStart(ITestResult result) { ScenarioReportData.clear(); }
    @Override public void onTestSuccess(ITestResult result) { finish(result, "PASS"); }
    @Override public void onTestFailure(ITestResult result) { finish(result, "FAIL"); }
    @Override public void onTestSkipped(ITestResult result) { finish(result, "SKIP"); }

    private void finish(ITestResult result, String status) {
        ScenarioReportData.put("Scenario", result.getMethod().getDescription());
        String screenshotName = result.getMethod().getMethodName() + "-" + status + ".png";
        Map<String, String> data = ScenarioReportData.snapshot();
        Throwable error = result.getThrowable();
        ROWS.add(new ReportRow(runTimestamp, result.getMethod().getMethodName(), data.getOrDefault("Scenario", ""),
                status, data.getOrDefault("Selected city", ""), data.getOrDefault("City listing data shown", ""),
                data.getOrDefault("Filter applied", ""), data.getOrDefault("Filter/listing details shown", ""),
                data.getOrDefault("Opened car", ""), data.getOrDefault("Detail/listing URL", ""),
                error == null ? "" : error.toString(),
                data.getOrDefault("Screenshot evidence", "Not captured (browser unavailable)")));
    }

    @Override public void onFinish(ISuite suite) {
        Path report = Path.of(System.getProperty("excel.report", "test-results/Spinny_Execution_Results.xlsx"));
        try { writeWorkbook(report); }
        catch (IOException | RuntimeException e) {
            System.err.println("Could not write Excel execution report at " + report + ": " + e.getMessage());
        }
    }

    private void writeWorkbook(Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        try (Workbook workbook = Files.exists(path)
                ? WorkbookFactory.create(path.toFile())
                : new XSSFWorkbook()) {
            int oldSheet = workbook.getSheetIndex("Execution Results");
            if (oldSheet >= 0) workbook.removeSheetAt(oldSheet);
            Sheet sheet = workbook.createSheet("Execution Results");
            sheet.createFreezePane(0, 1);
            CellStyle header = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            header.setFont(font);
            header.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row headings = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell cell = headings.createCell(i);
                cell.setCellValue(COLUMNS[i]);
                cell.setCellStyle(header);
            }
            List<ReportRow> rows = new ArrayList<>(ROWS);
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                String[] values = rows.get(r).values();
                for (int c = 0; c < values.length; c++) row.createCell(c).setCellValue(values[c]);
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, rows.size(), 0, COLUMNS.length - 1));
            for (int c = 0; c < COLUMNS.length; c++) sheet.setColumnWidth(c, Math.min(14000, Math.max(3500, COLUMNS[c].length() * 280)));
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                Files.write(path, out.toByteArray());
            }
        }
        System.out.println("Excel execution report written: " + path.toAbsolutePath());
    }

    private record ReportRow(String timestamp, String test, String scenario, String status, String city,
                             String cityData, String filter, String details, String car, String url,
                             String error, String screenshot) {
        String[] values() { return new String[] {timestamp, test, scenario, status, city, cityData, filter, details, car, url, error, screenshot}; }
    }
}
