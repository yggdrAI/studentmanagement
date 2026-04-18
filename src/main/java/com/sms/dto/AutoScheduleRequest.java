package com.sms.dto;

import com.sms.model.ScheduleEntry;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AutoScheduleRequest {

    private LocalDate scheduleDate;
    private DayOfWeek dayOfWeek;
    private boolean replaceExisting = false;
    private Integer practicalBlockSize = 2;
    private List<SlotDTO> slots = new ArrayList<>();
    private List<SubjectRequirementDTO> subjects = new ArrayList<>();

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public boolean isReplaceExisting() {
        return replaceExisting;
    }

    public void setReplaceExisting(boolean replaceExisting) {
        this.replaceExisting = replaceExisting;
    }

    public Integer getPracticalBlockSize() {
        return practicalBlockSize;
    }

    public void setPracticalBlockSize(Integer practicalBlockSize) {
        this.practicalBlockSize = practicalBlockSize;
    }

    public List<SlotDTO> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotDTO> slots) {
        this.slots = slots;
    }

    public List<SubjectRequirementDTO> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectRequirementDTO> subjects) {
        this.subjects = subjects;
    }

    public static class SlotDTO {
        private LocalTime startTime;
        private LocalTime endTime;

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
    }

    public static class SubjectRequirementDTO {
        private String subjectId;
        private String subjectName;
        private String subjectCode;
        private String facultyId;
        private String facultyName;
        private String roomId;
        private String roomNumber;
        private int hoursPerWeek;
        private ScheduleEntry.ClassType classType = ScheduleEntry.ClassType.LECTURE;
        private boolean practical;

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

        public int getHoursPerWeek() {
            return hoursPerWeek;
        }

        public void setHoursPerWeek(int hoursPerWeek) {
            this.hoursPerWeek = hoursPerWeek;
        }

        public ScheduleEntry.ClassType getClassType() {
            return classType;
        }

        public void setClassType(ScheduleEntry.ClassType classType) {
            this.classType = classType;
        }

        public boolean isPractical() {
            return practical;
        }

        public void setPractical(boolean practical) {
            this.practical = practical;
        }
    }
}
