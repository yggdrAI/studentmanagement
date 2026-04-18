package com.sms.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sms.dto.AutoScheduleRequest;
import com.sms.dto.AutoScheduleResponse;
import com.sms.dto.MoveScheduleRequest;
import com.sms.dto.MoveScheduleResponse;
import com.sms.dto.ScheduleEntryDTO;
import com.sms.dto.TimetableDTO;
import com.sms.dto.WorkloadBalanceResponse;
import com.sms.model.ScheduleEntry;
import com.sms.model.Timetable;
import com.sms.model.TimetableConflict;
import com.sms.model.TimetableHoliday;
import com.sms.model.TimetableVersion;
import com.sms.repository.ScheduleEntryRepository;
import com.sms.repository.TimetableConflictRepository;
import com.sms.repository.TimetableHolidayRepository;
import com.sms.repository.TimetableRepository;
import com.sms.repository.TimetableVersionRepository;

@Service
@Transactional
public class TimetableService {

    private static final Logger log = LoggerFactory.getLogger(TimetableService.class);
    
    private final TimetableRepository timetableRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final TimetableHolidayRepository holidayRepository;
    private final TimetableVersionRepository versionRepository;
    private final TimetableConflictRepository conflictRepository;

    public TimetableService(TimetableRepository timetableRepository,
                            ScheduleEntryRepository scheduleEntryRepository,
                            TimetableHolidayRepository holidayRepository,
                            TimetableVersionRepository versionRepository,
                            TimetableConflictRepository conflictRepository) {
        this.timetableRepository = timetableRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.holidayRepository = holidayRepository;
        this.versionRepository = versionRepository;
        this.conflictRepository = conflictRepository;
    }

    @Transactional(readOnly = true)
    public List<Timetable> listTimetables(String courseId,
                                          Integer semester,
                                          String section,
                                          String academicYear) {
        List<Timetable> timetables;
        if (courseId != null && !courseId.isBlank() && semester != null && academicYear != null && !academicYear.isBlank()) {
            timetables = timetableRepository.findByCourseIdAndSemesterAndAcademicYear(courseId.trim(), semester, academicYear.trim());
        } else {
            timetables = timetableRepository.findAll();
        }

        return timetables.stream()
            .filter(t -> section == null || section.isBlank() || Objects.equals(section.trim(), t.getSection()))
            .sorted(Comparator.comparing(Timetable::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }
    
    /**
     * Create a new timetable
     */
    public Timetable createTimetable(TimetableDTO dto, Long tenantId) {
        log.info("Creating timetable for course: {}, semester: {}", dto.getCourseId(), dto.getSemester());
        
        Timetable timetable = new Timetable();
        timetable.setTimetableCode(generateTimetableCode(dto));
        timetable.setCourseId(dto.getCourseId());
        timetable.setCourseName(dto.getCourseName());
        timetable.setSemester(dto.getSemester());
        timetable.setSection(dto.getSection());
        timetable.setAcademicYear(dto.getAcademicYear());
        timetable.setEffectiveFrom(dto.getEffectiveFrom());
        timetable.setEffectiveTo(dto.getEffectiveTo());
        timetable.setStatus(Timetable.TimetableStatus.DRAFT);
        timetable.setTenantId(tenantId);
        
        return timetableRepository.save(timetable);
    }
    
    /**
     * Add schedule entries to timetable
     */
    public void addScheduleEntries(Long timetableId, List<ScheduleEntryDTO> entries, Long tenantId) {
        Timetable timetable = timetableRepository.findById(timetableId)
            .orElseThrow(() -> new RuntimeException("Timetable not found: " + timetableId));
        
        for (ScheduleEntryDTO entryDto : entries) {
            createScheduleEntry(timetable, entryDto, tenantId);
        }
        
        log.info("Added {} schedule entries to timetable: {}", entries.size(), timetableId);
    }

    public void updateWeeklySchedule(Long timetableId,
                                     List<ScheduleEntryDTO> entries,
                                     Long tenantId,
                                     boolean replaceExisting) {
        Timetable timetable = timetableRepository.findById(timetableId)
            .orElseThrow(() -> new RuntimeException("Timetable not found: " + timetableId));

        List<ScheduleEntryDTO> normalizedEntries = entries == null ? List.of() : entries.stream()
            .filter(Objects::nonNull)
            .filter(entry -> entry.getDayOfWeek() != null)
            .collect(Collectors.toList());

        if (normalizedEntries.isEmpty()) {
            throw new IllegalArgumentException("At least one weekly schedule entry is required");
        }

        if (replaceExisting) {
            EnumSet<DayOfWeek> targetedDays = normalizedEntries.stream()
                .map(ScheduleEntryDTO::getDayOfWeek)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));

            for (DayOfWeek day : targetedDays) {
                List<ScheduleEntry> existing = scheduleEntryRepository.findByTimetableIdAndDayOfWeek(timetableId, day);
                if (!existing.isEmpty()) {
                    scheduleEntryRepository.deleteAll(existing);
                }
            }
        }

        for (ScheduleEntryDTO dto : normalizedEntries) {
            if (dto.getStartTime() == null || dto.getEndTime() == null || !dto.getEndTime().isAfter(dto.getStartTime())) {
                throw new IllegalArgumentException("Invalid time range for " + dto.getDayOfWeek());
            }
            if (dto.getSubjectId() == null || dto.getSubjectId().isBlank() || dto.getSubjectName() == null || dto.getSubjectName().isBlank()) {
                throw new IllegalArgumentException("Subject id and subject name are required for weekly entries");
            }
            createScheduleEntry(timetable, dto, tenantId);
        }

        detectConflicts(timetableId);
        log.info("Weekly schedule updated for timetable {} with {} entries", timetableId, normalizedEntries.size());
    }
    
    /**
     * Detect conflicts in timetable
     */
    public void detectConflicts(Long timetableId) {
        log.info("Detecting conflicts for timetable: {}", timetableId);
        
        Timetable timetable = timetableRepository.findById(timetableId)
            .orElseThrow(() -> new RuntimeException("Timetable not found: " + timetableId));
        
        List<ScheduleEntry> entries = scheduleEntryRepository.findByTimetableId(timetableId);
        
        // Clear existing conflicts
        List<TimetableConflict> existingConflicts = conflictRepository.findByTimetableId(timetableId);
        conflictRepository.deleteAll(existingConflicts);
        
        // Check for faculty clashes
        checkFacultyConflicts(timetable, entries);
        
        // Check for room clashes
        checkRoomConflicts(timetable, entries);
    }
    
    private void checkFacultyConflicts(Timetable timetable, List<ScheduleEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                ScheduleEntry entry1 = entries.get(i);
                ScheduleEntry entry2 = entries.get(j);
                
                if (entry1.getFacultyId().equals(entry2.getFacultyId()) &&
                    entry1.getDayOfWeek() == entry2.getDayOfWeek() &&
                    hasTimeOverlap(entry1, entry2)) {
                    
                    createConflict(timetable, entry1, entry2, 
                        TimetableConflict.ConflictType.FACULTY_CLASH,
                        "Faculty " + entry1.getFacultyName() + " has overlapping classes");
                }
            }
        }
    }
    
    private void checkRoomConflicts(Timetable timetable, List<ScheduleEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                ScheduleEntry entry1 = entries.get(i);
                ScheduleEntry entry2 = entries.get(j);
                
                if (entry1.getRoomId().equals(entry2.getRoomId()) &&
                    entry1.getDayOfWeek() == entry2.getDayOfWeek() &&
                    hasTimeOverlap(entry1, entry2)) {
                    
                    createConflict(timetable, entry1, entry2,
                        TimetableConflict.ConflictType.ROOM_CLASH,
                        "Room " + entry1.getRoomNumber() + " is double-booked");
                }
            }
        }
    }
    
    private boolean hasTimeOverlap(ScheduleEntry entry1, ScheduleEntry entry2) {
        return !entry1.getEndTime().isBefore(entry2.getStartTime()) &&
               !entry2.getEndTime().isBefore(entry1.getStartTime());
    }
    
    private void createConflict(Timetable timetable, ScheduleEntry entry1, ScheduleEntry entry2,
                               TimetableConflict.ConflictType type, String description) {
        TimetableConflict conflict = new TimetableConflict();
        conflict.setTimetable(timetable);
        conflict.setScheduleEntryId1(entry1.getId());
        conflict.setScheduleEntryId2(entry2.getId());
        conflict.setConflictType(type);
        conflict.setDescription(description);
        conflict.setResource1(entry1.getFacultyId());
        conflict.setResource2(entry2.getFacultyId());
        conflict.setSeverity(TimetableConflict.Severity.HIGH);
        conflict.setStatus(TimetableConflict.ConflictStatus.PENDING);
        conflict.setTenantId(timetable.getTenantId());
        
        conflictRepository.save(conflict);
        log.warn("Conflict detected: {}", description);
    }
    
    /**
     * Get timetable with all details
     */
    @Transactional(readOnly = true)
    public Timetable getTimetableWithDetails(Long timetableId) {
        return timetableRepository.findById(timetableId)
            .orElseThrow(() -> new RuntimeException("Timetable not found: " + timetableId));
    }
    
    /**
     * Get timetable for a specific day
     */
    @Transactional(readOnly = true)
    public List<ScheduleEntry> getTimetableForDay(Long timetableId, LocalDate date) {
        return scheduleEntryRepository.findByTimetableAndDate(timetableId, date);
    }
    
    /**
     * Publish timetable
     */
    public void publishTimetable(Long timetableId, String publishedBy) {
        Timetable timetable = timetableRepository.findById(timetableId)
            .orElseThrow(() -> new RuntimeException("Timetable not found: " + timetableId));
        
        // Check for unresolved critical conflicts
        long criticalConflicts = conflictRepository.countByTimetableIdAndStatus(timetableId, 
            TimetableConflict.ConflictStatus.PENDING);
        
        if (criticalConflicts > 0) {
            throw new RuntimeException("Cannot publish timetable with unresolved conflicts");
        }
        
        timetable.setStatus(Timetable.TimetableStatus.PUBLISHED);
        timetable.setUpdatedBy(publishedBy);
        timetableRepository.save(timetable);
        
        // Create version
        createVersion(timetable, TimetableVersion.ChangeType.PUBLISHED, "Timetable published", publishedBy);
        
        log.info("Timetable {} published by {}", timetableId, publishedBy);
    }
    
    /**
     * Create a new version of the timetable
     */
    private void createVersion(Timetable timetable, TimetableVersion.ChangeType changeType, 
                              String description, String createdBy) {
        TimetableVersion version = new TimetableVersion();
        version.setTimetable(timetable);
        version.setVersionNumber(getNextVersionNumber(timetable.getId()));
        version.setChangeType(changeType);
        version.setChangeDescription(description);
        version.setCreatedBy(createdBy);
        version.setTenantId(timetable.getTenantId());
        // In real implementation, convert timetable to JSON for snapshot
        version.setSnapshotJson("{}");
        
        versionRepository.save(version);
    }
    
    private int getNextVersionNumber(Long timetableId) {
        Integer maxVersion = versionRepository.findMaxVersionNumber(timetableId);
        return (maxVersion == null) ? 1 : maxVersion + 1;
    }
    
    /**
     * Add holiday
     */
    public void addHoliday(Long timetableId, LocalDate date, TimetableHoliday.HolidayType type, 
                          String reason, Long tenantId) {
        Timetable timetable = timetableRepository.findById(timetableId)
            .orElseThrow(() -> new RuntimeException("Timetable not found: " + timetableId));
        
        TimetableHoliday holiday = new TimetableHoliday();
        holiday.setTimetable(timetable);
        holiday.setHolidayDate(date);
        holiday.setHolidayType(type);
        holiday.setReason(reason);
        holiday.setTenantId(tenantId);
        
        holidayRepository.save(holiday);
        log.info("Holiday added: {} - {}", date, reason);
    }
    
    /**
     * Get active timetable for a date
     */
    @Transactional(readOnly = true)
    public List<Timetable> getActiveTimetablesForDate(LocalDate date) {
        return timetableRepository.findActiveTimetablesForDate(date);
    }

    public MoveScheduleResponse moveScheduleEntry(MoveScheduleRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("Invalid time range");
        }

        ScheduleEntry session = scheduleEntryRepository.findById(request.getSessionId())
            .orElseThrow(() -> new RuntimeException("Schedule entry not found: " + request.getSessionId()));

        LocalDate targetDate = request.getScheduleDate() != null ? request.getScheduleDate() : session.getScheduleDate();
        DayOfWeek targetDay = request.getDayOfWeek() != null ? request.getDayOfWeek() : session.getDayOfWeek();
        String targetFacultyId = normalizeOrFallback(request.getFacultyId(), session.getFacultyId());
        String targetRoomId = normalizeOrFallback(request.getRoomId(), session.getRoomId());

        if (targetDate == null && targetDay == null) {
            throw new IllegalArgumentException("Either scheduleDate or dayOfWeek is required");
        }

        List<String> conflicts = findConflicts(
            session,
            targetDate,
            targetDay,
            request.getStartTime(),
            request.getEndTime(),
            targetFacultyId,
            targetRoomId
        );

        if (!conflicts.isEmpty()) {
            MoveScheduleResponse denied = new MoveScheduleResponse();
            denied.setUpdated(false);
            denied.setMessage("Conflict detected");
            denied.setConflicts(conflicts);
            denied.setSession(toScheduleEntryDTO(session));
            denied.setFacultyWorkload(calculateWorkloadMap(session.getTimetable().getId()));
            return denied;
        }

        session.setScheduleDate(targetDate);
        session.setDayOfWeek(targetDay);
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setFacultyId(targetFacultyId);
        session.setFacultyName(normalizeOrFallback(request.getFacultyName(), session.getFacultyName()));
        session.setRoomId(targetRoomId);
        session.setRoomNumber(normalizeOrFallback(request.getRoomNumber(), session.getRoomNumber()));
        if (request.getClassType() != null) {
            session.setClassType(request.getClassType());
        }

        ScheduleEntry saved = scheduleEntryRepository.save(session);
        detectConflicts(saved.getTimetable().getId());

        MoveScheduleResponse ok = new MoveScheduleResponse();
        ok.setUpdated(true);
        ok.setMessage("Updated");
        ok.setConflicts(Collections.emptyList());
        ok.setSession(toScheduleEntryDTO(saved));
        ok.setFacultyWorkload(calculateWorkloadMap(saved.getTimetable().getId()));
        return ok;
    }

    public AutoScheduleResponse generateSchedule(Long timetableId, AutoScheduleRequest request, Long tenantId) {
        Timetable timetable = timetableRepository.findById(timetableId)
            .orElseThrow(() -> new RuntimeException("Timetable not found: " + timetableId));

        if (request.getSubjects() == null || request.getSubjects().isEmpty()) {
            throw new IllegalArgumentException("At least one subject is required for auto generation");
        }

        LocalDate targetDate = request.getScheduleDate();
        DayOfWeek targetDay = request.getDayOfWeek() != null
            ? request.getDayOfWeek()
            : (targetDate != null ? targetDate.getDayOfWeek() : null);

        if (targetDate == null && targetDay == null) {
            throw new IllegalArgumentException("scheduleDate or dayOfWeek is required");
        }

        List<Slot> slots = buildSlots(request.getSlots());
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("No valid time slots available for scheduling");
        }

        List<ScheduleEntry> existing = getEntriesForTarget(timetableId, targetDate, targetDay);
        if (!request.isReplaceExisting() && !existing.isEmpty()) {
            throw new IllegalStateException("Target window already has sessions. Use replaceExisting=true to regenerate.");
        }

        if (request.isReplaceExisting() && !existing.isEmpty()) {
            scheduleEntryRepository.deleteAll(existing);
        }

        int practicalBlockSize = request.getPracticalBlockSize() == null ? 2 : request.getPracticalBlockSize().intValue();
        List<UnitTask> tasks = explodeToTasks(request.getSubjects(), Math.max(2, practicalBlockSize));
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("No schedulable subject tasks generated from input");
        }

        // Hardest-first heuristic: practical blocks and high-hours subjects first.
        tasks.sort(Comparator
            .comparingInt((UnitTask t) -> t.blockSize).reversed()
            .thenComparingInt((UnitTask t) -> t.hoursPerWeek).reversed()
            .thenComparing(t -> t.subjectCode));

        List<Assignment> assignments = new ArrayList<>();
        Map<String, Integer> facultyLoad = calculateWorkloadMap(timetableId);

        boolean solved = backtrackSchedule(tasks, slots, 0, assignments, facultyLoad);

        AutoScheduleResponse response = new AutoScheduleResponse();
        response.setSolved(solved);

        if (!solved) {
            response.setMessage("Unable to generate a conflict-free schedule with provided constraints");
            response.setGeneratedCount(0);
            response.setGeneratedEntries(Collections.emptyList());
            response.setFacultyWorkload(facultyLoad);
            return response;
        }

        List<ScheduleEntry> generatedEntries = new ArrayList<>();
        for (Assignment assignment : assignments) {
            UnitTask task = assignment.task;
            for (int i = 0; i < task.blockSize; i++) {
                Slot slot = slots.get(assignment.slotIndex + i);
                ScheduleEntry entry = new ScheduleEntry();
                entry.setTimetable(timetable);
                entry.setClassCode(generateAutoClassCode(timetable.getId(), task.subjectCode, slot.startTime));
                entry.setSubjectId(task.subjectId);
                entry.setSubjectName(task.subjectName);
                entry.setSubjectCode(task.subjectCode);
                entry.setFacultyId(task.facultyId);
                entry.setFacultyName(task.facultyName);
                entry.setRoomId(task.roomId);
                entry.setRoomNumber(task.roomNumber);
                entry.setDayOfWeek(targetDay);
                entry.setScheduleDate(targetDate);
                entry.setStartTime(slot.startTime);
                entry.setEndTime(slot.endTime);
                entry.setClassType(task.classType);
                entry.setTenantId(tenantId);
                generatedEntries.add(scheduleEntryRepository.save(entry));
            }
        }

        detectConflicts(timetableId);
        Map<String, Integer> updatedLoad = calculateWorkloadMap(timetableId);

        response.setMessage("Schedule generated");
        response.setGeneratedCount(generatedEntries.size());
        response.setGeneratedEntries(generatedEntries.stream().map(this::toScheduleEntryDTO).collect(Collectors.toList()));
        response.setFacultyWorkload(updatedLoad);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> calculateWorkloadMap(Long timetableId) {
        List<ScheduleEntry> sessions = scheduleEntryRepository.findByTimetableId(timetableId);
        Map<String, Integer> load = new HashMap<>();

        for (ScheduleEntry session : sessions) {
            String faculty = normalizeOrFallback(session.getFacultyName(), session.getFacultyId());
            load.put(faculty, load.getOrDefault(faculty, 0) + 1);
        }

        return load;
    }

    public WorkloadBalanceResponse rebalanceWorkload(Long timetableId, int maxAllowed) {
        if (maxAllowed < 1) {
            throw new IllegalArgumentException("maxAllowed must be >= 1");
        }

        List<ScheduleEntry> entries = scheduleEntryRepository.findByTimetableId(timetableId);
        Map<String, Integer> load = calculateWorkloadMap(timetableId);

        List<String> overloaded = load.entrySet().stream()
            .filter(e -> e.getValue() > maxAllowed)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        int rebalanced = 0;

        if (!overloaded.isEmpty()) {
            Map<String, Integer> idLoad = new HashMap<>();
            for (ScheduleEntry entry : entries) {
                idLoad.put(entry.getFacultyId(), idLoad.getOrDefault(entry.getFacultyId(), 0) + 1);
            }

            List<String> facultyIds = new ArrayList<>(idLoad.keySet());
            for (ScheduleEntry entry : entries) {
                Integer current = idLoad.get(entry.getFacultyId());
                if (current == null || current <= maxAllowed) {
                    continue;
                }

                Optional<String> candidate = facultyIds.stream()
                    .filter(fid -> !fid.equals(entry.getFacultyId()))
                    .filter(fid -> idLoad.getOrDefault(fid, 0) < maxAllowed)
                    .findFirst();

                if (candidate.isPresent()) {
                    String previousFacultyId = entry.getFacultyId();
                    String nextFacultyId = candidate.get();
                    entry.setFacultyId(nextFacultyId);
                    entry.setFacultyName(nextFacultyId);
                    scheduleEntryRepository.save(entry);
                    idLoad.put(previousFacultyId, Math.max(0, idLoad.getOrDefault(previousFacultyId, 0) - 1));
                    idLoad.put(nextFacultyId, idLoad.getOrDefault(nextFacultyId, 0) + 1);
                    rebalanced++;
                }
            }
        }

        detectConflicts(timetableId);

        WorkloadBalanceResponse response = new WorkloadBalanceResponse();
        response.setWorkload(calculateWorkloadMap(timetableId));
        response.setMaxAllowed(maxAllowed);
        response.setOverloadedFaculties(response.getWorkload().entrySet().stream()
            .filter(e -> e.getValue() > maxAllowed)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList()));
        response.setRebalancedEntries(rebalanced);
        return response;
    }
    
    private String generateTimetableCode(TimetableDTO dto) {
        return String.format("TT-%s-SEM%d-%s", 
            dto.getCourseId().toUpperCase().replaceAll(" ", "-"),
            dto.getSemester(),
            dto.getAcademicYear().replaceAll("-", ""));
    }

    private void createScheduleEntry(Timetable timetable, ScheduleEntryDTO entryDto, Long tenantId) {
        ScheduleEntry entry = new ScheduleEntry();
        entry.setTimetable(timetable);
        entry.setClassCode(generateClassCode(timetable.getId(), entryDto));
        entry.setSubjectId(entryDto.getSubjectId());
        entry.setSubjectName(entryDto.getSubjectName());
        entry.setSubjectCode(entryDto.getSubjectCode());
        entry.setFacultyId(entryDto.getFacultyId());
        entry.setFacultyName(entryDto.getFacultyName());
        entry.setRoomId(entryDto.getRoomId());
        entry.setRoomNumber(entryDto.getRoomNumber());
        entry.setDayOfWeek(entryDto.getDayOfWeek());
        entry.setScheduleDate(entryDto.getScheduleDate());
        entry.setStartTime(entryDto.getStartTime());
        entry.setEndTime(entryDto.getEndTime());
        entry.setClassType(entryDto.getClassType() == null ? ScheduleEntry.ClassType.LECTURE : entryDto.getClassType());
        entry.setTenantId(tenantId);
        scheduleEntryRepository.save(entry);
    }
    
    private String generateClassCode(Long timetableId, ScheduleEntryDTO dto) {
        return String.format("CLASS-%d-%s-%s", 
            timetableId,
            dto.getDayOfWeek().name().substring(0, 3),
            System.nanoTime());
    }

    private String generateAutoClassCode(Long timetableId, String subjectCode, LocalTime startTime) {
        String safeSubject = (subjectCode == null || subjectCode.isBlank()) ? "SUB" : subjectCode;
        return String.format("AUTO-%d-%s-%s", timetableId, safeSubject, startTime.toString().replace(":", ""));
    }

    private ScheduleEntryDTO toScheduleEntryDTO(ScheduleEntry entry) {
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

    private List<ScheduleEntry> getEntriesForTarget(Long timetableId, LocalDate scheduleDate, DayOfWeek dayOfWeek) {
        if (scheduleDate != null) {
            return scheduleEntryRepository.findByTimetableIdAndScheduleDate(timetableId, scheduleDate);
        }
        return scheduleEntryRepository.findByTimetableIdAndDayOfWeek(timetableId, dayOfWeek);
    }

    private List<String> findConflicts(ScheduleEntry source,
                                       LocalDate targetDate,
                                       DayOfWeek targetDay,
                                       LocalTime start,
                                       LocalTime end,
                                       String targetFacultyId,
                                       String targetRoomId) {
        List<ScheduleEntry> scoped = getEntriesForTarget(source.getTimetable().getId(), targetDate, targetDay);
        List<String> conflicts = new ArrayList<>();

        for (ScheduleEntry candidate : scoped) {
            if (Objects.equals(candidate.getId(), source.getId())) {
                continue;
            }
            if (!hasTimeOverlap(start, end, candidate.getStartTime(), candidate.getEndTime())) {
                continue;
            }
            if (Objects.equals(targetFacultyId, candidate.getFacultyId())) {
                conflicts.add("Faculty clash with class " + candidate.getClassCode());
            }
            if (Objects.equals(targetRoomId, candidate.getRoomId())) {
                conflicts.add("Room clash with class " + candidate.getClassCode());
            }
        }

        return conflicts;
    }

    private boolean hasTimeOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private String normalizeOrFallback(String candidate, String fallback) {
        return (candidate == null || candidate.isBlank()) ? fallback : candidate;
    }

    private List<Slot> buildSlots(List<AutoScheduleRequest.SlotDTO> slotDtos) {
        List<Slot> slots = new ArrayList<>();

        if (slotDtos != null && !slotDtos.isEmpty()) {
            for (AutoScheduleRequest.SlotDTO dto : slotDtos) {
                if (dto.getStartTime() == null || dto.getEndTime() == null || !dto.getEndTime().isAfter(dto.getStartTime())) {
                    continue;
                }
                slots.add(new Slot(dto.getStartTime(), dto.getEndTime()));
            }
        } else {
            LocalTime cursor = LocalTime.of(9, 0);
            while (cursor.isBefore(LocalTime.of(17, 0))) {
                slots.add(new Slot(cursor, cursor.plusHours(1)));
                cursor = cursor.plusHours(1);
            }
        }

        slots.sort(Comparator.comparing(s -> s.startTime));
        return slots;
    }

    private List<UnitTask> explodeToTasks(List<AutoScheduleRequest.SubjectRequirementDTO> subjects, int practicalBlockSize) {
        List<UnitTask> tasks = new ArrayList<>();
        for (AutoScheduleRequest.SubjectRequirementDTO subject : subjects) {
            int hours = Math.max(0, subject.getHoursPerWeek());
            if (hours == 0) {
                continue;
            }

            int block = subject.isPractical() ? practicalBlockSize : 1;
            int remaining = hours;
            while (remaining > 0) {
                int chunk = Math.min(block, remaining);
                tasks.add(new UnitTask(
                    subject.getSubjectId(),
                    subject.getSubjectName(),
                    subject.getSubjectCode(),
                    subject.getFacultyId(),
                    subject.getFacultyName(),
                    subject.getRoomId(),
                    subject.getRoomNumber(),
                    subject.getClassType(),
                    subject.getHoursPerWeek(),
                    chunk
                ));
                remaining -= chunk;
            }
        }
        return tasks;
    }

    private boolean backtrackSchedule(List<UnitTask> tasks,
                                      List<Slot> slots,
                                      int taskIndex,
                                      List<Assignment> assignments,
                                      Map<String, Integer> facultyLoad) {
        if (taskIndex >= tasks.size()) {
            return true;
        }

        UnitTask task = tasks.get(taskIndex);

        List<Integer> candidateStarts = new ArrayList<>();
        for (int i = 0; i <= slots.size() - task.blockSize; i++) {
            if (isConsecutive(slots, i, task.blockSize)) {
                candidateStarts.add(i);
            }
        }

        // Heuristic: prefer earlier slots and less-loaded faculty pressure.
        candidateStarts.sort(Comparator.comparingInt(i -> i));

        for (Integer startIndex : candidateStarts) {
            if (!isValidAssignment(task, startIndex, slots, assignments)) {
                continue;
            }

            Assignment assignment = new Assignment(task, startIndex);
            assignments.add(assignment);
            facultyLoad.put(task.facultyId, facultyLoad.getOrDefault(task.facultyId, 0) + task.blockSize);

            if (backtrackSchedule(tasks, slots, taskIndex + 1, assignments, facultyLoad)) {
                return true;
            }

            assignments.remove(assignments.size() - 1);
            facultyLoad.put(task.facultyId, Math.max(0, facultyLoad.getOrDefault(task.facultyId, 0) - task.blockSize));
        }

        return false;
    }

    private boolean isConsecutive(List<Slot> slots, int startIndex, int blockSize) {
        for (int i = 0; i < blockSize - 1; i++) {
            Slot current = slots.get(startIndex + i);
            Slot next = slots.get(startIndex + i + 1);
            if (!current.endTime.equals(next.startTime)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidAssignment(UnitTask task,
                                      int startIndex,
                                      List<Slot> slots,
                                      List<Assignment> assignments) {
        for (Assignment assigned : assignments) {
            for (int i = 0; i < task.blockSize; i++) {
                Slot current = slots.get(startIndex + i);
                for (int j = 0; j < assigned.task.blockSize; j++) {
                    Slot existing = slots.get(assigned.slotIndex + j);
                    if (!hasTimeOverlap(current.startTime, current.endTime, existing.startTime, existing.endTime)) {
                        continue;
                    }

                    if (Objects.equals(task.facultyId, assigned.task.facultyId)) {
                        return false;
                    }
                    if (Objects.equals(task.roomId, assigned.task.roomId)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static final class Slot {
        private final LocalTime startTime;
        private final LocalTime endTime;

        private Slot(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    private static final class UnitTask {
        private final String subjectId;
        private final String subjectName;
        private final String subjectCode;
        private final String facultyId;
        private final String facultyName;
        private final String roomId;
        private final String roomNumber;
        private final ScheduleEntry.ClassType classType;
        private final int hoursPerWeek;
        private final int blockSize;

        private UnitTask(String subjectId,
                         String subjectName,
                         String subjectCode,
                         String facultyId,
                         String facultyName,
                         String roomId,
                         String roomNumber,
                         ScheduleEntry.ClassType classType,
                         int hoursPerWeek,
                         int blockSize) {
            this.subjectId = subjectId;
            this.subjectName = subjectName;
            this.subjectCode = subjectCode;
            this.facultyId = facultyId;
            this.facultyName = facultyName;
            this.roomId = roomId;
            this.roomNumber = roomNumber;
            this.classType = classType;
            this.hoursPerWeek = hoursPerWeek;
            this.blockSize = blockSize;
        }
    }

    private static final class Assignment {
        private final UnitTask task;
        private final int slotIndex;

        private Assignment(UnitTask task, int slotIndex) {
            this.task = task;
            this.slotIndex = slotIndex;
        }
    }
}
