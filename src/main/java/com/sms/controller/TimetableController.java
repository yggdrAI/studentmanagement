package com.sms.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.AutoScheduleRequest;
import com.sms.dto.AutoScheduleResponse;
import com.sms.dto.MoveScheduleRequest;
import com.sms.dto.MoveScheduleResponse;
import com.sms.dto.ScheduleEntryDTO;
import com.sms.dto.TimetableDTO;
import com.sms.dto.WeeklyScheduleUpdateRequest;
import com.sms.dto.WorkloadBalanceResponse;
import com.sms.model.ScheduleEntry;
import com.sms.model.Timetable;
import com.sms.model.TimetableHoliday;
import com.sms.service.TimetableService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/timetables")
@PreAuthorize("hasRole('ADMIN')")
public class TimetableController {

    private static final Logger log = LoggerFactory.getLogger(TimetableController.class);
    
    private final TimetableService timetableService;
    private static final Long DEFAULT_TENANT_ID = 1L;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }
    
    /**
         * List timetables with optional class filters.
         */
        @GetMapping
        public ResponseEntity<List<TimetableDTO>> listTimetables(
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String academicYear) {
        List<TimetableDTO> dtos = timetableService.listTimetables(courseId, semester, section, academicYear).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
        }

        /**
     * Create a new timetable
     */
    @PostMapping
    public ResponseEntity<TimetableDTO> createTimetable(@RequestBody TimetableDTO dto) {
        log.info("Creating timetable for course: {}", dto.getCourseName());
        
        Timetable timetable = timetableService.createTimetable(dto, DEFAULT_TENANT_ID);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(timetable));
    }
    
    /**
     * Get timetable by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TimetableDTO> getTimetable(@PathVariable Long id) {
        Timetable timetable = timetableService.getTimetableWithDetails(id);
        return ResponseEntity.ok(convertToDTO(timetable));
    }
    
    /**
     * Add schedule entries to timetable
     */
    @PostMapping("/{id}/schedule-entries")
    public ResponseEntity<Void> addScheduleEntries(
            @PathVariable Long id,
            @RequestBody List<ScheduleEntryDTO> entries) {
        log.info("Adding {} schedule entries to timetable: {}", entries.size(), id);
        
        timetableService.addScheduleEntries(id, entries, DEFAULT_TENANT_ID);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Replace or append weekly schedule entries for a class timetable.
     */
    @PutMapping("/{id}/weekly-schedule")
    public ResponseEntity<Void> updateWeeklySchedule(
            @PathVariable Long id,
            @RequestBody WeeklyScheduleUpdateRequest request) {
        List<ScheduleEntryDTO> entries = request == null ? List.of() : request.getEntries();
        timetableService.updateWeeklySchedule(id, entries, DEFAULT_TENANT_ID, request != null && request.isReplaceExisting());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get timetable for a specific day
     */
    @GetMapping("/{id}/day")
    public ResponseEntity<List<ScheduleEntryDTO>> getTimetableForDay(
            @PathVariable Long id,
            @RequestParam LocalDate date) {
        List<ScheduleEntry> entries = timetableService.getTimetableForDay(id, date);
        List<ScheduleEntryDTO> dtos = entries.stream()
                .map(this::convertScheduleEntryToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Detect conflicts in timetable
     */
    @PostMapping("/{id}/detect-conflicts")
    public ResponseEntity<Void> detectConflicts(@PathVariable Long id) {
        log.info("Detecting conflicts for timetable: {}", id);
        timetableService.detectConflicts(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Publish timetable
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishTimetable(
            @PathVariable Long id,
            @RequestParam String publishedBy) {
        log.info("Publishing timetable: {}", id);
        timetableService.publishTimetable(id, publishedBy);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Add holiday
     */
    @PostMapping("/{id}/holidays")
    public ResponseEntity<Void> addHoliday(
            @PathVariable Long id,
            @RequestParam LocalDate date,
            @RequestParam TimetableHoliday.HolidayType type,
            @RequestParam String reason) {
        timetableService.addHoliday(id, date, type, reason, DEFAULT_TENANT_ID);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    /**
     * Get active timetables for a date
     */
    @GetMapping("/active")
    public ResponseEntity<List<TimetableDTO>> getActiveTimetables(
            @RequestParam LocalDate date) {
        List<Timetable> timetables = timetableService.getActiveTimetablesForDate(date);
        List<TimetableDTO> dtos = timetables.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Drag-drop move endpoint with conflict validation.
     */
    @PutMapping("/move")
    public ResponseEntity<?> moveScheduleEntry(@Valid @RequestBody MoveScheduleRequest request) {
        MoveScheduleResponse response = timetableService.moveScheduleEntry(request);
        if (!response.isUpdated()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Auto timetable generation using CSP-style backtracking with heuristics.
     */
    @PostMapping("/{id}/auto-generate")
    public ResponseEntity<AutoScheduleResponse> autoGenerate(
            @PathVariable Long id,
            @RequestBody AutoScheduleRequest request) {
        AutoScheduleResponse response = timetableService.generateSchedule(id, request, DEFAULT_TENANT_ID);
        return ResponseEntity.ok(response);
    }

    /**
     * Faculty workload summary.
     */
    @GetMapping("/{id}/faculty-workload")
    public ResponseEntity<Map<String, Integer>> facultyWorkload(@PathVariable Long id) {
        return ResponseEntity.ok(timetableService.calculateWorkloadMap(id));
    }

    /**
     * Workload rebalance entry point.
     */
    @PostMapping("/{id}/rebalance")
    public ResponseEntity<WorkloadBalanceResponse> rebalance(
            @PathVariable Long id,
            @RequestParam(defaultValue = "8") int maxAllowed) {
        return ResponseEntity.ok(timetableService.rebalanceWorkload(id, maxAllowed));
    }
    
    // Helper methods
    private TimetableDTO convertToDTO(Timetable timetable) {
        TimetableDTO dto = new TimetableDTO();
        dto.setId(timetable.getId());
        dto.setTimetableCode(timetable.getTimetableCode());
        dto.setCourseId(timetable.getCourseId());
        dto.setCourseName(timetable.getCourseName());
        dto.setSemester(timetable.getSemester());
        dto.setSection(timetable.getSection());
        dto.setAcademicYear(timetable.getAcademicYear());
        dto.setEffectiveFrom(timetable.getEffectiveFrom());
        dto.setEffectiveTo(timetable.getEffectiveTo());
        dto.setStatus(timetable.getStatus());
        dto.setCreatedAt(timetable.getCreatedAt());
        dto.setUpdatedAt(timetable.getUpdatedAt());
        dto.setCreatedBy(timetable.getCreatedBy());
        dto.setUpdatedBy(timetable.getUpdatedBy());
        
        if (timetable.getScheduleEntries() != null) {
            dto.setScheduleEntries(timetable.getScheduleEntries().stream()
                    .map(this::convertScheduleEntryToDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private ScheduleEntryDTO convertScheduleEntryToDTO(ScheduleEntry entry) {
        ScheduleEntryDTO dto = new ScheduleEntryDTO();
        dto.setId(entry.getId());
        dto.setClassCode(entry.getClassCode());
        dto.setSubjectId(entry.getSubjectId());
        dto.setSubjectName(entry.getSubjectName());
        dto.setSubjectCode(entry.getSubjectCode());
        dto.setFacultyId(entry.getFacultyId());
        dto.setFacultyName(entry.getFacultyName());
        dto.setRoomId(entry.getRoomId());
        dto.setRoomNumber(entry.getRoomNumber());
        dto.setDayOfWeek(entry.getDayOfWeek());
        dto.setScheduleDate(entry.getScheduleDate());
        dto.setStartTime(entry.getStartTime());
        dto.setEndTime(entry.getEndTime());
        dto.setClassType(entry.getClassType());
        dto.setAttendanceStatus(entry.getAttendanceStatus());
        dto.setIsException(entry.getIsException());
        return dto;
    }
}
