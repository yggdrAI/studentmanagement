package com.sms.dto;

import com.sms.model.ScheduleEntry;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public class ScheduleEntryDTO {
    
    private Long id;
    private String classCode;
    private String subjectId;
    private String subjectName;
    private String subjectCode;
    private String facultyId;
    private String facultyName;
    private String roomId;
    private String roomNumber;
    private DayOfWeek dayOfWeek;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ScheduleEntry.ClassType classType;
    private ScheduleEntry.AttendanceStatus attendanceStatus;
    private Boolean isException;
    private String duration;

    public ScheduleEntryDTO() {
    }

    public ScheduleEntryDTO(Long id, String classCode, String subjectId, String subjectName, String subjectCode,
                            String facultyId, String facultyName, String roomId, String roomNumber,
                            DayOfWeek dayOfWeek, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime,
                            ScheduleEntry.ClassType classType, ScheduleEntry.AttendanceStatus attendanceStatus,
                            Boolean isException, String duration) {
        this.id = id;
        this.classCode = classCode;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.facultyId = facultyId;
        this.facultyName = facultyName;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.dayOfWeek = dayOfWeek;
        this.scheduleDate = scheduleDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.classType = classType;
        this.attendanceStatus = attendanceStatus;
        this.isException = isException;
        this.duration = duration;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public ScheduleEntry.ClassType getClassType() {
        return classType;
    }

    public void setClassType(ScheduleEntry.ClassType classType) {
        this.classType = classType;
    }

    public ScheduleEntry.AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(ScheduleEntry.AttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public Boolean getIsException() {
        return isException;
    }

    public void setIsException(Boolean exception) {
        isException = exception;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
    
    public String getDuration() {
        if (startTime != null && endTime != null) {
            return startTime + " - " + endTime;
        }
        return duration;
    }
}
