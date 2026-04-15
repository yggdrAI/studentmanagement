package com.sms.dto.attendance;

import java.util.List;

/**
 * Request for manual attendance marking by teacher
 */
public class ManualAttendanceRequest {
    private Long subjectId;
    private String attendanceDate;
    private List<StudentAttendanceRecord> attendanceRecords;

    public ManualAttendanceRequest() {}

    public ManualAttendanceRequest(Long subjectId, String attendanceDate, 
                                  List<StudentAttendanceRecord> attendanceRecords) {
        this.subjectId = subjectId;
        this.attendanceDate = attendanceDate;
        this.attendanceRecords = attendanceRecords;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(String attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public List<StudentAttendanceRecord> getAttendanceRecords() {
        return attendanceRecords;
    }

    public void setAttendanceRecords(List<StudentAttendanceRecord> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    /**
     * Individual student attendance record
     */
    public static class StudentAttendanceRecord {
        private String studentId;
        private String status; // PRESENT, ABSENT, LATE

        public StudentAttendanceRecord() {}

        public StudentAttendanceRecord(String studentId, String status) {
            this.studentId = studentId;
            this.status = status;
        }

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
