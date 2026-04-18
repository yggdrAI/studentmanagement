package com.sms.service;

import com.sms.model.Attendance;
import com.sms.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing attendance records
 * Handles validation, duplicate checking, and reporting
 */
@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AnalyticsRealtimeNotifier analyticsRealtimeNotifier;

    @Autowired
    private AnalyticsCacheService analyticsCacheService;

    /**
     * Mark attendance for a student
     * Includes duplicate checking and validation
     */
    @Transactional
    public Attendance markAttendance(String studentId, Long subjectId, Long teacherId,
                                    String status, String markingType, String deviceInfo,
                                    String ipAddress, String tokenHash) throws Exception {
        return markAttendance(studentId, subjectId, teacherId, status, markingType, deviceInfo, ipAddress,
            null, null, false, null, null, tokenHash);
        }

        /**
         * Mark attendance for a student with geolocation audit data
         */
        @Transactional
        public Attendance markAttendance(String studentId, Long subjectId, Long teacherId,
                        String status, String markingType, String deviceInfo,
                        String ipAddress, Double studentLatitude, Double studentLongitude,
                        Boolean locationVerified, String deviceId, Long campusLocationId,
                        String tokenHash) throws Exception {
        
        LocalDate today = LocalDate.now();

        // ✅ Check 1: Duplicate attendance check
        if (attendanceRepository.existsByStudentAndSubjectAndDate(studentId, subjectId, today)) {
            throw new RuntimeException("Attendance already marked for today");
        }

        // ✅ Check 2: Prevent reuse of same token on same day
        if (tokenHash != null) {
            if (attendanceRepository.existsByTokenHashAndStudentAndDate(tokenHash, studentId, today)) {
                throw new RuntimeException("QR token already used");
            }
        }

        // Create new attendance record
        Attendance attendance = new Attendance(
            studentId,
            subjectId,
            teacherId,
            today,
            status,
            markingType
        );
        
        attendance.setDeviceInfo(deviceInfo);
        attendance.setIpAddress(ipAddress);
        attendance.setStudentLatitude(studentLatitude);
        attendance.setStudentLongitude(studentLongitude);
        attendance.setLocationVerified(Boolean.TRUE.equals(locationVerified));
        attendance.setDeviceId(deviceId);
        attendance.setCampusLocationId(campusLocationId);
        attendance.setQrTokenUsed(tokenHash);

        Attendance saved = attendanceRepository.save(attendance);
        analyticsRealtimeNotifier.notifyAttendanceEvent(studentId, status);
        analyticsCacheService.evictAnalyticsCaches();
        return saved;
    }

    /**
     * Mark manual attendance for multiple students
     */
    @Transactional
    public void markManualAttendance(Long subjectId, Long teacherId, LocalDate date,
                                    List<ManualAttendanceRecord> records) throws Exception {
        
        for (ManualAttendanceRecord record : records) {
            try {
                // Check if already marked
                if (!attendanceRepository.existsByStudentAndSubjectAndDate(
                    record.getStudentId(), subjectId, date)) {
                    
                    Attendance attendance = new Attendance(
                        record.getStudentId(),
                        subjectId,
                        teacherId,
                        date,
                        record.getStatus(),
                        "MANUAL"
                    );
                    
                    Attendance saved = attendanceRepository.save(attendance);
                    analyticsRealtimeNotifier.notifyAttendanceEvent(saved.getStudentId(), saved.getStatus());
                    analyticsCacheService.evictAnalyticsCaches();
                }
            } catch (Exception e) {
                System.err.println("Failed to mark attendance for " + record.getStudentId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Get student attendance for a subject
     */
    public List<Attendance> getStudentAttendance(String studentId, Long subjectId) {
        return attendanceRepository.findByStudentIdAndSubjectIdOrderByAttendanceDateDesc(studentId, subjectId);
    }

    /**
     * Get attendance for a specific date
     */
    public List<Attendance> getAttendanceForDate(Long subjectId, LocalDate date) {
        return attendanceRepository.findBySubjectAndDate(subjectId, date);
    }

    /**
     * Calculate attendance percentage for student
     */
    public Double calculateAttendancePercentage(String studentId, Long subjectId) {
        List<Attendance> records = attendanceRepository
            .findByStudentIdAndSubjectIdOrderByAttendanceDateDesc(studentId, subjectId);
        
        if (records.isEmpty()) {
            return 0.0;
        }
        
        long presentCount = records.stream()
            .filter(a -> "PRESENT".equals(a.getStatus()))
            .count();
        
        return (presentCount * 100.0) / records.size();
    }

    /**
     * Get attendance count for a date
     */
    public AttendanceStats getAttendanceStats(Long subjectId, LocalDate date) {
        List<Attendance> records = attendanceRepository.findBySubjectAndDate(subjectId, date);
        
        long present = records.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long absent = records.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        long late = records.stream().filter(a -> "LATE".equals(a.getStatus())).count();
        
        return new AttendanceStats(present, absent, late, records.size());
    }

    public WeightedAttendanceMetrics getWeightedAttendanceMetrics(String studentId, Long subjectId) {
        List<Attendance> records = attendanceRepository
                .findByStudentIdAndSubjectIdOrderByAttendanceDateDesc(studentId, subjectId);

        if (records.isEmpty()) {
            return new WeightedAttendanceMetrics(0.0, 0, 0, 0, 0, true);
        }

        int presentCount = 0;
        int absentCount = 0;
        int lateCount = 0;
        int suspiciousCount = 0;
        double earnedWeight = 0.0;

        for (Attendance record : records) {
            String status = record.getStatus() != null ? record.getStatus().trim().toUpperCase() : "ABSENT";
            switch (status) {
                case "PRESENT" -> {
                    presentCount++;
                    earnedWeight += 1.0;
                }
                case "LATE" -> {
                    lateCount++;
                    earnedWeight += 0.5;
                }
                default -> absentCount++;
            }

            if (Boolean.FALSE.equals(record.getLocationVerified())) {
                suspiciousCount++;
            }
        }

        double weightedPercentage = (earnedWeight * 100.0) / records.size();
        return new WeightedAttendanceMetrics(
                Math.round(weightedPercentage * 100.0) / 100.0,
                presentCount,
                lateCount,
                absentCount,
                suspiciousCount,
                true
        );
    }

    /**
     * Update attendance status (for admin/teacher corrections)
     */
    @Transactional
    public Attendance updateAttendanceStatus(Long attendanceId, String newStatus) throws Exception {
        Optional<Attendance> record = attendanceRepository.findById(attendanceId);
        
        if (record.isEmpty()) {
            throw new RuntimeException("Attendance record not found");
        }
        
        Attendance attendance = record.get();
        attendance.setStatus(newStatus);
        
        Attendance saved = attendanceRepository.save(attendance);
        analyticsRealtimeNotifier.notifyAttendanceEvent(saved.getStudentId(), saved.getStatus());
        analyticsCacheService.evictAnalyticsCaches();
        return saved;
    }

    /**
     * DTO for attendance statistics
     */
    public static class AttendanceStats {
        private long present;
        private long absent;
        private long late;
        private long total;

        public AttendanceStats(long present, long absent, long late, long total) {
            this.present = present;
            this.absent = absent;
            this.late = late;
            this.total = total;
        }

        public long getPresent() { return present; }
        public long getAbsent() { return absent; }
        public long getLate() { return late; }
        public long getTotal() { return total; }
        public double getPercentage() { return (present * 100.0) / Math.max(total, 1); }
    }

    /**
     * DTO for manual attendance records
     */
    public static class ManualAttendanceRecord {
        private String studentId;
        private String status;

        public ManualAttendanceRecord(String studentId, String status) {
            this.studentId = studentId;
            this.status = status;
        }

        public String getStudentId() { return studentId; }
        public String getStatus() { return status; }
    }

    public static class WeightedAttendanceMetrics {
        private final double weightedPercentage;
        private final int presentCount;
        private final int lateCount;
        private final int absentCount;
        private final int suspiciousCount;
        private final boolean faceVerificationEnforced;

        public WeightedAttendanceMetrics(double weightedPercentage,
                                         int presentCount,
                                         int lateCount,
                                         int absentCount,
                                         int suspiciousCount,
                                         boolean faceVerificationEnforced) {
            this.weightedPercentage = weightedPercentage;
            this.presentCount = presentCount;
            this.lateCount = lateCount;
            this.absentCount = absentCount;
            this.suspiciousCount = suspiciousCount;
            this.faceVerificationEnforced = faceVerificationEnforced;
        }

        public double getWeightedPercentage() { return weightedPercentage; }
        public int getPresentCount() { return presentCount; }
        public int getLateCount() { return lateCount; }
        public int getAbsentCount() { return absentCount; }
        public int getSuspiciousCount() { return suspiciousCount; }
        public boolean isFaceVerificationEnforced() { return faceVerificationEnforced; }
    }
}
