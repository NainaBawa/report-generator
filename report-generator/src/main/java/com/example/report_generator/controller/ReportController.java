package com.example.report_generator.controller;

import com.example.report_generator.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateReport(
            @RequestParam String feedFilePath,
            @RequestParam String referenceFilePath,
            @RequestParam String feedFileType,
            @RequestParam String referenceFileType) {
        try {
            reportService.generateReport(feedFilePath, referenceFilePath, feedFileType, referenceFileType);
            return ResponseEntity.ok("Report generation triggered successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating report: " + e.getMessage());
        }
    }

    @GetMapping("/describe")
    public Map<String, Object> describeApi() {
        return Map.of(
                "api", "Report Generator API",
                "description", "Welcome to the Report Generator API!",
                "endpoints", Map.of(
                        "GET /api/reports/generate", "Trigger report generation with specific parameters.",
                        "POST /api/reports/generate", "Trigger report generation with specific parameters (if implemented)."
                ),
                "usage", "For the GET request, no parameters are required. For POST, provide the necessary parameters in the request body or URL."
        );
    }
}
