package com.sms.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_import_job")
public class StudentImportJob {

    public enum Status {
        UPLOADED,
        PREVIEW_READY,
        CONFIRMED,
        ROLLED_BACK,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "uploaded_by", nullable = false, length = 128)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "invalid_rows", nullable = false)
    private int invalidRows;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "roll_back_last_import", nullable = false)
    private boolean rollbackOnFailure = true;

    @Column(name = "duplicate_strategy", nullable = false, length = 32)
    private String duplicateStrategy = "SKIP";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status = Status.UPLOADED;

    @Column(name = "last_error_report_name", length = 255)
    private String lastErrorReportName;

    @Column(name = "last_error_report_path", length = 500)
    private String lastErrorReportPath;

    @Column(name = "source_file_count", nullable = false)
    private int sourceFileCount;

    @Lob
    @Column(name = "source_files_json")
    private String sourceFilesJson;

    @Lob
    @Column(name = "merge_log_json")
    private String mergeLogJson;

    @Column(name = "fused_student_count", nullable = false)
    private int fusedStudentCount;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }
    public int getInvalidRows() { return invalidRows; }
    public void setInvalidRows(int invalidRows) { this.invalidRows = invalidRows; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    public boolean isRollbackOnFailure() { return rollbackOnFailure; }
    public void setRollbackOnFailure(boolean rollbackOnFailure) { this.rollbackOnFailure = rollbackOnFailure; }
    public String getDuplicateStrategy() { return duplicateStrategy; }
    public void setDuplicateStrategy(String duplicateStrategy) { this.duplicateStrategy = duplicateStrategy; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getLastErrorReportName() { return lastErrorReportName; }
    public void setLastErrorReportName(String lastErrorReportName) { this.lastErrorReportName = lastErrorReportName; }
    public String getLastErrorReportPath() { return lastErrorReportPath; }
    public void setLastErrorReportPath(String lastErrorReportPath) { this.lastErrorReportPath = lastErrorReportPath; }
    public int getSourceFileCount() { return sourceFileCount; }
    public void setSourceFileCount(int sourceFileCount) { this.sourceFileCount = sourceFileCount; }
    public String getSourceFilesJson() { return sourceFilesJson; }
    public void setSourceFilesJson(String sourceFilesJson) { this.sourceFilesJson = sourceFilesJson; }
    public String getMergeLogJson() { return mergeLogJson; }
    public void setMergeLogJson(String mergeLogJson) { this.mergeLogJson = mergeLogJson; }
    public int getFusedStudentCount() { return fusedStudentCount; }
    public void setFusedStudentCount(int fusedStudentCount) { this.fusedStudentCount = fusedStudentCount; }
}
