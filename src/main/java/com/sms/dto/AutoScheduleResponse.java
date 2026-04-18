package com.sms.dto;

import java.util.List;
import java.util.Map;

public class AutoScheduleResponse {

    private boolean solved;
    private String message;
    private int generatedCount;
    private List<ScheduleEntryDTO> generatedEntries;
    private Map<String, Integer> facultyWorkload;

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(int generatedCount) {
        this.generatedCount = generatedCount;
    }

    public List<ScheduleEntryDTO> getGeneratedEntries() {
        return generatedEntries;
    }

    public void setGeneratedEntries(List<ScheduleEntryDTO> generatedEntries) {
        this.generatedEntries = generatedEntries;
    }

    public Map<String, Integer> getFacultyWorkload() {
        return facultyWorkload;
    }

    public void setFacultyWorkload(Map<String, Integer> facultyWorkload) {
        this.facultyWorkload = facultyWorkload;
    }
}
