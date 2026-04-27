package com.sms.dto.student;

public class TransferStudentRequest {
    private String studentId;
    private Long targetBatchId;
    private Long targetClassId;

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Long getTargetBatchId() {
        return targetBatchId;
    }

    public void setTargetBatchId(Long targetBatchId) {
        this.targetBatchId = targetBatchId;
    }

    public Long getTargetClassId() {
        return targetClassId;
    }

    public void setTargetClassId(Long targetClassId) {
        this.targetClassId = targetClassId;
    }
}