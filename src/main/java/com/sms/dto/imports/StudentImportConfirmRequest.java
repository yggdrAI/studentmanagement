package com.sms.dto.imports;

import jakarta.validation.constraints.NotNull;

public class StudentImportConfirmRequest {
    @NotNull
    private Long jobId;

    private String duplicateStrategy = "SKIP";
    private Boolean rollbackOnFailure = Boolean.TRUE;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getDuplicateStrategy() { return duplicateStrategy; }
    public void setDuplicateStrategy(String duplicateStrategy) { this.duplicateStrategy = duplicateStrategy; }
    public Boolean getRollbackOnFailure() { return rollbackOnFailure; }
    public void setRollbackOnFailure(Boolean rollbackOnFailure) { this.rollbackOnFailure = rollbackOnFailure; }
}