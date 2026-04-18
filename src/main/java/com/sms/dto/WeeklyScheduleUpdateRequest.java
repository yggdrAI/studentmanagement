package com.sms.dto;

import java.util.List;

public class WeeklyScheduleUpdateRequest {

    private List<ScheduleEntryDTO> entries;
    private boolean replaceExisting = true;

    public List<ScheduleEntryDTO> getEntries() {
        return entries;
    }

    public void setEntries(List<ScheduleEntryDTO> entries) {
        this.entries = entries;
    }

    public boolean isReplaceExisting() {
        return replaceExisting;
    }

    public void setReplaceExisting(boolean replaceExisting) {
        this.replaceExisting = replaceExisting;
    }
}
