package com.example.report_generator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledReportService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledReportService.class);

    @Autowired
    private ReportService reportService;

    @Scheduled(cron = "0 0 * * * ?") // This cron expression schedules the task to run every hour
    public void generateScheduledReport() {
        logger.info("Scheduled report generation started.");

        // Update feedFilePath and referenceFilePath as per your requirement
        // You might want to move these paths to a properties file or configuration class
        String feedFilePath = "path/to/feedfile.csv";
        String referenceFilePath = "path/to/referencefile.csv";
        String feedFileType = "csv"; // Specify the feed file type
        String referenceFileType = "csv"; // Specify the reference file type

        try {
            reportService.generateReport(feedFilePath, referenceFilePath, feedFileType, referenceFileType);
            logger.info("Scheduled report generation completed successfully.");
        } catch (Exception e) {
            logger.error("Error during scheduled report generation: ", e);
        }
    }
}
