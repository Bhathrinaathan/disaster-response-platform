package com.disasterrecovery.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "GeneratedSummaries")
public class GeneratedSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SummaryID")
    private Integer summaryId;

    @Column(name = "ReportID")
    private Integer reportId;

    @Column(name = "SummaryType", length = 50)
    private String summaryType; // "AUTHORITY" or "CIVILIAN"

    @Column(name = "AuthorityContent", columnDefinition = "TEXT")
    private String authorityContent;

    @Column(name = "CivilianContent", columnDefinition = "TEXT")
    private String civilianContent;

    @Column(name = "DocumentPath", length = 500)
    private String documentPath;

    @Column(name = "IsPublished")
    private Boolean isPublished;

    @Column(name = "GeneratedAt")
    private LocalDateTime generatedAt;

    @Column(name = "PublishedAt")
    private LocalDateTime publishedAt;

    @Column(name = "GeneratedBy", length = 100)
    private String generatedBy;

    @Column(name = "Status", length = 50)
    private String status; // "DRAFT", "REVIEWED", "PUBLISHED"

    // Constructors
    public GeneratedSummary() {
        this.generatedAt = LocalDateTime.now();
        this.isPublished = false;
        this.status = "DRAFT";
    }

    // Getters and Setters
    public Integer getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(Integer summaryId) {
        this.summaryId = summaryId;
    }

    public Integer getReportId() {
        return reportId;
    }

    public void setReportId(Integer reportId) {
        this.reportId = reportId;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public String getAuthorityContent() {
        return authorityContent;
    }

    public void setAuthorityContent(String authorityContent) {
        this.authorityContent = authorityContent;
    }

    public String getCivilianContent() {
        return civilianContent;
    }

    public void setCivilianContent(String civilianContent) {
        this.civilianContent = civilianContent;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
---------------------------------------------------------------------------
