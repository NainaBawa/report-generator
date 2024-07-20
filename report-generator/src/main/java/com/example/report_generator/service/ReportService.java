package com.example.report_generator.service;

import com.example.report_generator.config.ReportRulesProperties;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private ReportRulesProperties reportRulesProperties;

    public void generateReport(String feedFilePath, String referenceFilePath, String feedFileType, String referenceFileType) throws Exception {
        logger.info("Starting report generation...");
        logger.info("Feed file path: {}", feedFilePath);
        logger.info("Reference file path: {}", referenceFilePath);
        logger.info("Feed file type: {}", feedFileType);
        logger.info("Reference file type: {}", referenceFileType);

        // Read the feed file
        List<Map<String, String>> feedData = readFile(feedFilePath, feedFileType);

        // Read the reference file
        List<Map<String, String>> referenceData = readFile(referenceFilePath, referenceFileType);

        // Perform transformation based on the rules
        List<Map<String, String>> transformedData = applyTransformationRules(feedData, referenceData);

        // Output the transformed data to a CSV file
        writeCSVOutput(transformedData);
        logger.info("Report generation completed.");
    }

    private List<Map<String, String>> readFile(String filePath, String fileType) throws IOException {
        List<Map<String, String>> data = new ArrayList<>();

        if ("csv".equalsIgnoreCase(fileType)) {
            Reader in = new FileReader(filePath);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
            for (CSVRecord record : records) {
                Map<String, String> row = new HashMap<>();
                for (String header : record.toMap().keySet()) {
                    row.put(header, record.get(header));
                }
                data.add(row);
            }
        } else if ("xls".equalsIgnoreCase(fileType) || "xlsx".equalsIgnoreCase(fileType)) {
            try (InputStream inp = new FileInputStream(filePath)) {
                Sheet sheet = WorkbookFactory.create(inp).getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    Map<String, String> rowData = new HashMap<>();
                    for (int cn = 0; cn < row.getLastCellNum(); cn++) {
                        rowData.put(headerRow.getCell(cn).getStringCellValue(), row.getCell(cn).getStringCellValue());
                    }
                    data.add(rowData);
                }
            }
        }
        return data;
    }

    private List<Map<String, String>> applyTransformationRules(List<Map<String, String>> feedData, List<Map<String, String>> referenceData) {
        List<Map<String, String>> transformedData = new ArrayList<>();

        for (int i = 0; i < feedData.size(); i++) {
            Map<String, String> transformedRow = new HashMap<>();
            Map<String, String> feedRow = feedData.get(i);
            Map<String, String> referenceRow = referenceData.size() > i ? referenceData.get(i) : new HashMap<>();

            transformedRow.put("outfield1", evaluateExpression(reportRulesProperties.getRules().get("outfield1"), feedRow, referenceRow));
            transformedRow.put("outfield2", evaluateExpression(reportRulesProperties.getRules().get("outfield2"), feedRow, referenceRow));
            transformedRow.put("outfield3", evaluateExpression(reportRulesProperties.getRules().get("outfield3"), feedRow, referenceRow));
            transformedRow.put("outfield4", evaluateExpression(reportRulesProperties.getRules().get("outfield4"), feedRow, referenceRow));
            transformedRow.put("outfield5", evaluateExpression(reportRulesProperties.getRules().get("outfield5"), feedRow, referenceRow));

            transformedData.add(transformedRow);
        }

        return transformedData;
    }

    private String evaluateExpression(String expression, Map<String, String> feedData, Map<String, String> referenceData) {
        try {
            // Replace variables with values from feedData and referenceData
            expression = expression.replaceAll("field1", feedData.getOrDefault("field1", "0"))
                    .replaceAll("field2", feedData.getOrDefault("field2", "0"))
                    .replaceAll("field3", feedData.getOrDefault("field3", "0"))
                    .replaceAll("field5", feedData.getOrDefault("field5", "0"))
                    .replaceAll("refdata1", referenceData.getOrDefault("refdata1", "0"))
                    .replaceAll("refdata2", referenceData.getOrDefault("refdata2", "0"))
                    .replaceAll("refdata3", referenceData.getOrDefault("refdata3", "0"))
                    .replaceAll("refdata4", referenceData.getOrDefault("refdata4", "0"));

            // Handle `max` function
            while (expression.contains("max(")) {
                int start = expression.indexOf("max(");
                int end = expression.indexOf(')', start);
                String maxContent = expression.substring(start + 4, end); // Get content inside `max()`
                String[] parts = maxContent.split(",");
                double maxValue = Math.max(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
                expression = expression.substring(0, start) + maxValue + expression.substring(end + 1);
            }

            // Evaluate the remaining expression
            return String.valueOf(evaluateSimpleExpression(expression));
        } catch (Exception e) {
            logger.error("Error evaluating expression '{}': {}", expression, e.getMessage());
            return "Error";
        }
    }

    private double evaluateSimpleExpression(String expression) {
        // This method assumes expression is in a simple form with single operations
        String[] parts;
        if (expression.contains("+")) {
            parts = expression.split("\\+");
            return Double.parseDouble(parts[0].trim()) + Double.parseDouble(parts[1].trim());
        } else if (expression.contains("-")) {
            parts = expression.split("\\-");
            return Double.parseDouble(parts[0].trim()) - Double.parseDouble(parts[1].trim());
        } else if (expression.contains("*")) {
            parts = expression.split("\\*");
            return Double.parseDouble(parts[0].trim()) * Double.parseDouble(parts[1].trim());
        } else if (expression.contains("/")) {
            parts = expression.split("\\/");
            return Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim());
        } else {
            return Double.parseDouble(expression.trim());
        }
    }


    private void writeCSVOutput(List<Map<String, String>> transformedData) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.csv"));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("outfield1", "outfield2", "outfield3", "outfield4", "outfield5"))) {

            for (Map<String, String> row : transformedData) {
                csvPrinter.printRecord(
                        row.get("outfield1"),
                        row.get("outfield2"),
                        row.get("outfield3"),
                        row.get("outfield4"),
                        row.get("outfield5")
                );
            }
        }
    }
}
