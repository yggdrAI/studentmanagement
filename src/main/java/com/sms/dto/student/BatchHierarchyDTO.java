package com.sms.dto.student;

import java.util.List;

public class BatchHierarchyDTO {
    private Long classId;
    private String className;
    private List<BatchDTO> batches;

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public List<BatchDTO> getBatches() { return batches; }
    public void setBatches(List<BatchDTO> batches) { this.batches = batches; }

    public static class BatchDTO {
        private Long batchId;
        private String batchName;
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
        public String getBatchName() { return batchName; }
        public void setBatchName(String batchName) { this.batchName = batchName; }
    }
}
