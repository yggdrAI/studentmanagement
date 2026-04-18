package com.sms.dto;

import com.sms.model.TimetableConflict;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableConflictDTO {
    
    private Long id;
    private Long scheduleEntryId1;
    private Long scheduleEntryId2;
    private TimetableConflict.ConflictType conflictType;
    private String description;
    private String resource1;
    private String resource2;
    private TimetableConflict.Severity severity;
    private TimetableConflict.ConflictStatus status;
    private String resolutionSuggestion;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
