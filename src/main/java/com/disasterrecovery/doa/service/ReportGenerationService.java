package com.disasterrecovery.doa.service;

import com.disasterrecovery.doa.controller.dto.LLMResponseDTO;
import com.disasterrecovery.repository.GeneratedSummaryRepository;
import com.disasterrecovery.entities.GeneratedSummary;
import com.disasterrecovery.exception.ReportGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service to generate HTML reports from LLM response and save to database
 * HTML files can be opened directly in Microsoft Word
 * NO EXTERNAL DEPENDENCIES REQUIRED
 */
@Service
public class ReportGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationService.class);

    @Autowired
    private GeneratedSummaryRepository summaryRepository;

    @Value("${report.output.directory:./reports}")
    private String outputDirectory;

    /**
     * Generate and save disaster summary report
     */
    public GeneratedSummary generateAndSaveReport(
            Integer reportId,
            LLMResponseDTO llmResponse,
            String generatedBy) {

        logger.info("Generating report for Report ID: {}", reportId);

        try {
            // Create output directory if it doesn't exist
            ensureOutputDirectoryExists();

            // Generate authority report
            String authorityDocPath = generateHtmlDocument(
                    reportId,
                    llmResponse.getAuthoritySummary(),
                    "AUTHORITY"
            );

            // Generate civilian report
            String civilianDocPath = generateHtmlDocument(
                    reportId,
                    llmResponse.getCivilianSummary(),
                    "CIVILIAN"
            );

            // Save summary to database
            GeneratedSummary summary = new GeneratedSummary();
            summary.setReportId(reportId);
            summary.setAuthorityContent(llmResponse.getAuthoritySummary());
            summary.setCivilianContent(llmResponse.getCivilianSummary());
            summary.setDocumentPath(authorityDocPath + "," + civilianDocPath);
            summary.setGeneratedBy(generatedBy);
            summary.setStatus("DRAFT");
            summary.setIsPublished(false);

            GeneratedSummary savedSummary = summaryRepository.save(summary);
            logger.info("Report saved successfully with ID: {}", savedSummary.getSummaryId());

            return savedSummary;

        } catch (Exception e) {
            logger.error("Failed to generate report for Report ID: {}", reportId, e);
            throw new ReportGenerationException("Failed to generate and save report", e);
        }
    }

    /**
     * Generate HTML document from summary text
     * HTML files can be opened in Microsoft Word
     */
    private String generateHtmlDocument(Integer reportId, String content, String reportType) {
        try {
            String fileName = String.format("Disaster_Report_%d_%s_%s.html",
                    reportId,
                    reportType,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            Path filePath = Paths.get(outputDirectory, fileName);

            // Build HTML content
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>Disaster Summary Report - ").append(reportType).append("</title>\n");
            html.append("    <style>\n");
            html.append(getHtmlStyles(reportType));
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");

            // Header
            html.append("    <div class=\"header\">\n");
            html.append("        <h1>Disaster Summary Report</h1>\n");
            html.append("        <h2>").append(reportType).append(" REPORT</h2>\n");
            html.append("    </div>\n");

            // Metadata
            html.append("    <div class=\"metadata\">\n");
            html.append("        <p><strong>Report ID:</strong> ").append(reportId).append("</p>\n");
            html.append("        <p><strong>Generated:</strong> ").append(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
            ).append("</p>\n");
            html.append("        <p><strong>Report Type:</strong> ").append(reportType).append("</p>\n");
            html.append("    </div>\n");

            html.append("    <hr class=\"divider\">\n");

            // Content
            html.append("    <div class=\"content\">\n");
            html.append(formatContentToHtml(content));
            html.append("    </div>\n");

            // Footer
            html.append("    <hr class=\"divider\">\n");
            html.append("    <div class=\"footer\">\n");
            if (reportType.equals("AUTHORITY")) {
                html.append("        <p class=\"confidential\">⚠️ CONFIDENTIAL - FOR OFFICIAL USE ONLY</p>\n");
            } else {
                html.append("        <p class=\"public-info\">ℹ️ This report is for public information</p>\n");
            }
            html.append("        <p class=\"generated-info\">Generated by Disaster Recovery System</p>\n");
            html.append("    </div>\n");

            html.append("</body>\n");
            html.append("</html>");

            // Write to file
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write(html.toString());
                logger.info("HTML document generated: {}", filePath.toString());
            }

            return filePath.toString();

        } catch (IOException e) {
            logger.error("Error generating HTML document for Report ID: {}, Type: {}", reportId, reportType, e);
            throw new ReportGenerationException("Failed to generate HTML document", e);
        }
    }

    /**
     * Get CSS styles for the HTML document
     */
    private String getHtmlStyles(String reportType) {
        String accentColor = reportType.equals("AUTHORITY") ? "#c41e3a" : "#1e88e5";

        StringBuilder css = new StringBuilder();
        css.append("body {\n");
        css.append("    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n");
        css.append("    line-height: 1.6;\n");
        css.append("    color: #333;\n");
        css.append("    max-width: 900px;\n");
        css.append("    margin: 0 auto;\n");
        css.append("    padding: 20px;\n");
        css.append("    background-color: #f5f5f5;\n");
        css.append("}\n");
        css.append(".header {\n");
        css.append("    text-align: center;\n");
        css.append("    background: linear-gradient(135deg, ").append(accentColor).append(" 0%, #555 100%);\n");
        css.append("    color: white;\n");
        css.append("    padding: 30px;\n");
        css.append("    border-radius: 10px 10px 0 0;\n");
        css.append("    margin-bottom: 0;\n");
        css.append("}\n");
        css.append(".header h1 {\n");
        css.append("    margin: 0;\n");
        css.append("    font-size: 32px;\n");
        css.append("    font-weight: 600;\n");
        css.append("}\n");
        css.append(".header h2 {\n");
        css.append("    margin: 10px 0 0 0;\n");
        css.append("    font-size: 18px;\n");
        css.append("    font-weight: 400;\n");
        css.append("    opacity: 0.9;\n");
        css.append("}\n");
        css.append(".metadata {\n");
        css.append("    background-color: #fff;\n");
        css.append("    padding: 20px;\n");
        css.append("    border-left: 4px solid ").append(accentColor).append(";\n");
        css.append("    margin-bottom: 20px;\n");
        css.append("}\n");
        css.append(".metadata p {\n");
        css.append("    margin: 8px 0;\n");
        css.append("}\n");
        css.append(".divider {\n");
        css.append("    border: none;\n");
        css.append("    border-top: 2px solid ").append(accentColor).append(";\n");
        css.append("    margin: 30px 0;\n");
        css.append("}\n");
        css.append(".content {\n");
        css.append("    background-color: #fff;\n");
        css.append("    padding: 30px;\n");
        css.append("    border-radius: 5px;\n");
        css.append("    box-shadow: 0 2px 5px rgba(0,0,0,0.1);\n");
        css.append("}\n");
        css.append(".content h3 {\n");
        css.append("    color: ").append(accentColor).append(";\n");
        css.append("    border-bottom: 2px solid ").append(accentColor).append(";\n");
        css.append("    padding-bottom: 10px;\n");
        css.append("    margin-top: 30px;\n");
        css.append("    margin-bottom: 15px;\n");
        css.append("}\n");
        css.append(".content h4 {\n");
        css.append("    color: #555;\n");
        css.append("    margin-top: 20px;\n");
        css.append("    margin-bottom: 10px;\n");
        css.append("}\n");
        css.append(".content p {\n");
        css.append("    margin: 10px 0;\n");
        css.append("    text-align: justify;\n");
        css.append("}\n");
        css.append(".content ul, .content ol {\n");
        css.append("    margin: 10px 0;\n");
        css.append("    padding-left: 30px;\n");
        css.append("}\n");
        css.append(".content li {\n");
        css.append("    margin: 5px 0;\n");
        css.append("}\n");
        css.append(".footer {\n");
        css.append("    text-align: center;\n");
        css.append("    padding: 20px;\n");
        css.append("    background-color: #fff;\n");
        css.append("    border-radius: 0 0 10px 10px;\n");
        css.append("    margin-top: 0;\n");
        css.append("}\n");
        css.append(".confidential {\n");
        css.append("    color: #c41e3a;\n");
        css.append("    font-weight: bold;\n");
        css.append("    font-size: 14px;\n");
        css.append("    margin: 10px 0;\n");
        css.append("}\n");
        css.append(".public-info {\n");
        css.append("    color: #1e88e5;\n");
        css.append("    font-weight: bold;\n");
        css.append("    font-size: 14px;\n");
        css.append("    margin: 10px 0;\n");
        css.append("}\n");
        css.append(".generated-info {\n");
        css.append("    color: #777;\n");
        css.append("    font-size: 12px;\n");
        css.append("    font-style: italic;\n");
        css.append("    margin: 5px 0;\n");
        css.append("}\n");
        css.append("@media print {\n");
        css.append("    body {\n");
        css.append("        background-color: white;\n");
        css.append("    }\n");
        css.append("    .header, .content, .footer, .metadata {\n");
        css.append("        box-shadow: none;\n");
        css.append("    }\n");
        css.append("}\n");

        return css.toString();
    }

    /**
     * Format content text to HTML with proper structure
     */
    private String formatContentToHtml(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "<p>No content available.</p>";
        }

        StringBuilder html = new StringBuilder();
        String[] paragraphs = content.split("\n\n");

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            // Check if it's a main heading (surrounded by **)
            if (para.startsWith("**") && para.endsWith("**")) {
                String heading = para.substring(2, para.length() - 2);
                html.append("        <h3>").append(escapeHtml(heading)).append("</h3>\n");
            }
            // Check if it's a sub-heading (starts with **)
            else if (para.startsWith("**") && para.contains(":**")) {
                String heading = para.substring(2, para.indexOf(":**"));
                String content_part = para.substring(para.indexOf(":**") + 3);
                html.append("        <h4>").append(escapeHtml(heading)).append("</h4>\n");
                html.append("        <p>").append(escapeHtml(content_part)).append("</p>\n");
            }
            // Check if it's a bulleted list (lines starting with -, *, or •)
            else if (para.contains("\n-") || para.contains("\n*") || para.contains("\n•")) {
                html.append("        <ul>\n");
                String[] lines = para.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
                        String listItem = line.substring(1).trim();
                        html.append("            <li>").append(escapeHtml(listItem)).append("</li>\n");
                    } else if (!line.isEmpty()) {
                        html.append("        <p>").append(escapeHtml(line)).append("</p>\n");
                    }
                }
                html.append("        </ul>\n");
            }
            // Check if it's a numbered list
            else if (para.matches(".*\\n\\d+\\..*")) {
                html.append("        <ol>\n");
                String[] lines = para.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.matches("^\\d+\\..*")) {
                        String listItem = line.substring(line.indexOf(".") + 1).trim();
                        html.append("            <li>").append(escapeHtml(listItem)).append("</li>\n");
                    } else if (!line.isEmpty()) {
                        html.append("        <p>").append(escapeHtml(line)).append("</p>\n");
                    }
                }
                html.append("        </ol>\n");
            }
            // Regular paragraph
            else {
                // Handle inline bold text
                String formatted = para.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
                html.append("        <p>").append(escapeHtml(formatted)).append("</p>\n");
            }
        }

        return html.toString();
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Publish report to make it available to civilians
     */
    public GeneratedSummary publishReport(Integer summaryId) {
        try {
            GeneratedSummary summary = summaryRepository.findById(summaryId)
                    .orElseThrow(() -> new ReportGenerationException("Summary not found with ID: " + summaryId));

            summary.setIsPublished(true);
            summary.setPublishedAt(LocalDateTime.now());
            summary.setStatus("PUBLISHED");

            GeneratedSummary published = summaryRepository.save(summary);
            logger.info("Report published successfully: {}", summaryId);

            return published;

        } catch (Exception e) {
            logger.error("Failed to publish report: {}", summaryId, e);
            throw new ReportGenerationException("Failed to publish report", e);
        }
    }

    /**
     * Update report with authority amendments
     */
    public GeneratedSummary updateReportWithAmendments(
            Integer summaryId,
            String updatedAuthorityContent,
            String updatedCivilianContent) {

        try {
            GeneratedSummary summary = summaryRepository.findById(summaryId)
                    .orElseThrow(() -> new ReportGenerationException("Summary not found with ID: " + summaryId));

            if (updatedAuthorityContent != null) {
                summary.setAuthorityContent(updatedAuthorityContent);
            }

            if (updatedCivilianContent != null) {
                summary.setCivilianContent(updatedCivilianContent);
            }

            summary.setStatus("REVIEWED");

            // Regenerate documents if content was updated
            if (updatedAuthorityContent != null || updatedCivilianContent != null) {
                String authorityDocPath = generateHtmlDocument(
                        summary.getReportId(),
                        summary.getAuthorityContent(),
                        "AUTHORITY"
                );

                String civilianDocPath = generateHtmlDocument(
                        summary.getReportId(),
                        summary.getCivilianContent(),
                        "CIVILIAN"
                );

                summary.setDocumentPath(authorityDocPath + "," + civilianDocPath);
            }

            GeneratedSummary updated = summaryRepository.save(summary);
            logger.info("Report updated successfully: {}", summaryId);

            return updated;

        } catch (Exception e) {
            logger.error("Failed to update report: {}", summaryId, e);
            throw new ReportGenerationException("Failed to update report", e);
        }
    }

    /**
     * Ensure output directory exists
     */
    private void ensureOutputDirectoryExists() throws IOException {
        Path path = Paths.get(outputDirectory);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("Created output directory: {}", outputDirectory);
        }
    }
}
---------------------------------------------------------------------------
