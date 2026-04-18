package com.sms.dto;

import java.util.List;
import java.util.Map;

public class MoveScheduleResponse {

    private boolean updated;
    private String message;
    private ScheduleEntryDTO session;
    private List<String> conflicts;
    private Map<String, Integer> facultyWorkload;

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ScheduleEntryDTO getSession() {
        return session;
    }

    public void setSession(ScheduleEntryDTO session) {
        this.session = session;
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts;
    }

    public Map<String, Integer> getFacultyWorkload() {
        return facultyWorkload;
    }

    public void setFacultyWorkload(Map<String, Integer> facultyWorkload) {
        this.facultyWorkload = facultyWorkload;
    }
}
