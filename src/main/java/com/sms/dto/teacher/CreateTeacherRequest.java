package com.sms.dto.teacher;

import java.util.List;

public class CreateTeacherRequest {
    public TeacherDTO teacher;
    public TeacherCredentialsDTO credentials;
    public List<TeacherAssignmentDTO> assignments;

    public static class TeacherDTO {
        public String firstName;
        public String lastName;
        public String fullName;
        public String email;
        public String phone;
        public String employeeId;
        public String department;
        public String designation;
        public String qualification;
        public Integer experienceYears;
        public String specialization;
        public String status;
        public String dateOfJoining;
    }

    public static class TeacherCredentialsDTO {
        public String username;
        public String password;
        public Boolean passwordResetRequired;
    }

    public static class TeacherAssignmentDTO {
        public Long classId;
        public Long batchId;
        public String subject;
        public Boolean isClassTeacher;
    }
}
