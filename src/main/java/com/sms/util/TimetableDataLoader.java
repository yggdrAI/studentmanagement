package com.sms.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.*;
import com.sms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to load sample timetable data from JSON file
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TimetableDataLoader {
    
    private final TimetableRepository timetableRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final TimetableHolidayRepository holidayRepository;
    private final TimetableVersionRepository versionRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Load timetable data from JSON file
     */
    @Transactional
    public Timetable loadFromJsonFile(String filePath, Long tenantId) {
        log.info("Loading timetable data from file: {}", filePath);
        
        try {
            JsonNode root = objectMapper.readTree(new File(filePath));
            return parseAndSaveTimetable(root, tenantId);
        } catch (Exception e) {
            log.error("Error loading timetable data from file: {}", filePath, e);
            throw new RuntimeException("Failed to load timetable data", e);
        }
    }
    
    /**
     * Parse JSON and save timetable with all entities
     */
    private Timetable parseAndSaveTimetable(JsonNode root, Long tenantId) {
        // Create and save timetable
        Timetable timetable = new Timetable();
        timetable.setTimetableCode(root.get("timetableId").asText());
        timetable.setCourseName(root.get("course").asText());
        timetable.setSemester(root.get("semester").asInt());
        timetable.setAcademicYear(root.get("academicYear").asText());
        timetable.setSection(root.get("section").asText());
        timetable.setEffectiveFrom(LocalDate.parse(root.get("effectiveFrom").asText()));
        timetable.setStatus(Timetable.TimetableStatus.DRAFT);
        timetable.setTenantId(tenantId);
        
        timetable = timetableRepository.save(timetable);
        log.info("Timetable created with ID: {}", timetable.getId());
        
        // Parse and save schedule entries
        parseScheduleEntries(root, timetable, tenantId);
        
        // Parse and save holidays
        parseHolidays(root, timetable, tenantId);
        
        // Create initial version
        createInitialVersion(timetable, tenantId);
        
        return timetable;
    }
    
    /**
     * Parse schedule entries from JSON
     */
    private void parseScheduleEntries(JsonNode root, Timetable timetable, Long tenantId) {
        JsonNode weeklySchedule = root.get("weeklySchedule");
        int entryCount = 0;
        
        for (DayOfWeek day : DayOfWeek.values()) {
            String dayName = day.name().toLowerCase();
            JsonNode daySchedule = weeklySchedule.get(dayName);
            
            if (daySchedule != null && daySchedule.isArray()) {
                for (JsonNode entryNode : daySchedule) {
                    ScheduleEntry entry = new ScheduleEntry();
                    entry.setTimetable(timetable);
                    entry.setClassCode(entryNode.get("classId").asText());
                    entry.setSubjectId(extractSubjectId(entryNode.get("subject").asText()));
                    entry.setSubjectName(entryNode.get("subject").asText());
                    entry.setSubjectCode(entryNode.get("code").asText());
                    entry.setFacultyId(extractFacultyId(entryNode.get("faculty").asText()));
                    entry.setFacultyName(entryNode.get("faculty").asText());
                    entry.setRoomId(extractRoomId(entryNode.get("room").asText()));
                    entry.setRoomNumber(entryNode.get("room").asText());
                    entry.setDayOfWeek(day);
                    entry.setStartTime(LocalTime.parse(entryNode.get("startTime").asText()));
                    entry.setEndTime(LocalTime.parse(entryNode.get("endTime").asText()));
                    
                    String classType = entryNode.get("type").asText();
                    entry.setClassType(ScheduleEntry.ClassType.valueOf(classType.toUpperCase()));
                    entry.setAttendanceStatus(ScheduleEntry.AttendanceStatus.PENDING);
                    entry.setIsException(false);
                    entry.setTenantId(tenantId);
                    
                    scheduleEntryRepository.save(entry);
                    entryCount++;
                }
            }
        }
        
        log.info("Loaded {} schedule entries for timetable: {}", entryCount, timetable.getId());
    }
    
    /**
     * Parse holidays from JSON
     */
    private void parseHolidays(JsonNode root, Timetable timetable, Long tenantId) {
        // Since JSON doesn't have holidays, we can add them separately
        // This is a placeholder for future enhancement
        log.debug("No holidays to parse from JSON");
    }
    
    /**
     * Create initial version record
     */
    private void createInitialVersion(Timetable timetable, Long tenantId) {
        TimetableVersion version = new TimetableVersion();
        version.setTimetable(timetable);
        version.setVersionNumber(1);
        version.setChangeType(TimetableVersion.ChangeType.CREATED);
        version.setChangeDescription("Initial timetable created from JSON data");
        version.setSnapshotJson("{}");
        version.setCreatedBy("SYSTEM");
        version.setTenantId(tenantId);
        
        versionRepository.save(version);
        log.info("Initial version created for timetable: {}", timetable.getId());
    }
    
    /**
     * Extract subject ID from subject name
     */
    private String extractSubjectId(String subjectName) {
        // Simple extraction logic - can be enhanced
        return "SUBJ-" + Math.abs(subjectName.hashCode() % 1000);
    }
    
    /**
     * Extract faculty ID from faculty name
     */
    private String extractFacultyId(String facultyName) {
        // Simple extraction logic - can be enhanced
        return "FAC-" + Math.abs(facultyName.hashCode() % 1000);
    }
    
    /**
     * Extract room ID from room number
     */
    private String extractRoomId(String roomNumber) {
        // Simple extraction logic - can be enhanced
        return "ROOM-" + Math.abs(roomNumber.hashCode() % 1000);
    }
}
