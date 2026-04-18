package com.sms.dto.imports;

import jakarta.validation.constraints.NotBlank;

public class StudentImportUploadRequest {
    @NotBlank
    private String duplicateStrategy = "SKIP";
    private Boolean rollbackOnFailure = Boolean.TRUE;

    public String getDuplicateStrategy() { return duplicateStrategy; }
    public void setDuplicateStrategy(String duplicateStrategy) { this.duplicateStrategy = duplicateStrategy; }
    public Boolean getRollbackOnFailure() { return rollbackOnFailure; }
    public void setRollbackOnFailure(Boolean rollbackOnFailure) { this.rollbackOnFailure = rollbackOnFailure; }
}