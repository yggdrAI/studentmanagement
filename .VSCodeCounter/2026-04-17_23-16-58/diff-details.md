# Diff Details

Date : 2026-04-17 23:16:58

Directory f:\\Coding\\studentmanagement

Total : 43 files,  964 codes, 17 comments, 118 blanks, all 1099 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [src/main/java/com/sms/config/DemoDataLoader.java](/src/main/java/com/sms/config/DemoDataLoader.java) | Java | 1 | 0 | 0 | 1 |
| [src/main/java/com/sms/controller/AdminController.java](/src/main/java/com/sms/controller/AdminController.java) | Java | 4 | 0 | 0 | 4 |
| [src/main/java/com/sms/controller/AdminProfileController.java](/src/main/java/com/sms/controller/AdminProfileController.java) | Java | 20 | 0 | 4 | 24 |
| [src/main/java/com/sms/controller/TeacherAttendanceController.java](/src/main/java/com/sms/controller/TeacherAttendanceController.java) | Java | 57 | -1 | 6 | 62 |
| [src/main/java/com/sms/dto/profile/AdminUpdateStudentProfileRequest.java](/src/main/java/com/sms/dto/profile/AdminUpdateStudentProfileRequest.java) | Java | 3 | 0 | 0 | 3 |
| [src/main/java/com/sms/dto/profile/StudentProfileResponseDTO.java](/src/main/java/com/sms/dto/profile/StudentProfileResponseDTO.java) | Java | 3 | 0 | 0 | 3 |
| [src/main/java/com/sms/model/StudentProfile.java](/src/main/java/com/sms/model/StudentProfile.java) | Java | 4 | 0 | 1 | 5 |
| [src/main/java/com/sms/service/StudentProfileService.java](/src/main/java/com/sms/service/StudentProfileService.java) | Java | 3 | 0 | 0 | 3 |
| [src/main/java/com/sms/service/StudentService.java](/src/main/java/com/sms/service/StudentService.java) | Java | 21 | 0 | 1 | 22 |
| [src/main/resources/static/css/dashboard.css](/src/main/resources/static/css/dashboard.css) | PostCSS | 85 | 0 | 17 | 102 |
| [src/main/resources/templates/admin-dashboard.html](/src/main/resources/templates/admin-dashboard.html) | HTML | -80 | -6 | -6 | -92 |
| [src/main/resources/templates/admin-profile.html](/src/main/resources/templates/admin-profile.html) | HTML | 118 | 0 | 14 | 132 |
| [src/main/resources/templates/admin-students.html](/src/main/resources/templates/admin-students.html) | HTML | 85 | 0 | 7 | 92 |
| [src/main/resources/templates/attendance-scanner.html](/src/main/resources/templates/attendance-scanner.html) | HTML | 65 | 0 | 1 | 66 |
| [src/main/resources/templates/fragments/sidebar.html](/src/main/resources/templates/fragments/sidebar.html) | HTML | 1 | 0 | 0 | 1 |
| [src/main/resources/templates/student-profile.html](/src/main/resources/templates/student-profile.html) | HTML | 72 | 0 | 8 | 80 |
| [src/main/resources/templates/teacher-attendance.html](/src/main/resources/templates/teacher-attendance.html) | HTML | 51 | 0 | 11 | 62 |
| [target/classes/com/sms/config/DemoDataLoader.class](/target/classes/com/sms/config/DemoDataLoader.class) | Java | -22 | 30 | 0 | 8 |
| [target/classes/com/sms/controller/AdminController.class](/target/classes/com/sms/controller/AdminController.class) | Java | 3 | 0 | 0 | 3 |
| [target/classes/com/sms/controller/AdminProfileController.class](/target/classes/com/sms/controller/AdminProfileController.class) | Java | 9 | 0 | 0 | 9 |
| [target/classes/com/sms/controller/AdminStudentProfileApiController.class](/target/classes/com/sms/controller/AdminStudentProfileApiController.class) | Java | 2 | 0 | 0 | 2 |
| [target/classes/com/sms/controller/DashboardController.class](/target/classes/com/sms/controller/DashboardController.class) | Java | 1 | 0 | 0 | 1 |
| [target/classes/com/sms/controller/DigitalIDApi.class](/target/classes/com/sms/controller/DigitalIDApi.class) | Java | 2 | 0 | 1 | 3 |
| [target/classes/com/sms/controller/PortalController.class](/target/classes/com/sms/controller/PortalController.class) | Java | -9 | 0 | 0 | -9 |
| [target/classes/com/sms/controller/StudentController.class](/target/classes/com/sms/controller/StudentController.class) | Java | -6 | 0 | 0 | -6 |
| [target/classes/com/sms/controller/StudentProfileApiController.class](/target/classes/com/sms/controller/StudentProfileApiController.class) | Java | 1 | 0 | 0 | 1 |
| [target/classes/com/sms/controller/TeacherAttendanceController.class](/target/classes/com/sms/controller/TeacherAttendanceController.class) | Java | 40 | 0 | 1 | 41 |
| [target/classes/com/sms/dto/dashboard/DashboardResponse.class](/target/classes/com/sms/dto/dashboard/DashboardResponse.class) | Java | -6 | 0 | 0 | -6 |
| [target/classes/com/sms/dto/dashboard/UpcomingClassDto.class](/target/classes/com/sms/dto/dashboard/UpcomingClassDto.class) | Java | -5 | 0 | 0 | -5 |
| [target/classes/com/sms/dto/profile/AdminUpdateStudentProfileRequest.class](/target/classes/com/sms/dto/profile/AdminUpdateStudentProfileRequest.class) | Java | 5 | 0 | 0 | 5 |
| [target/classes/com/sms/dto/profile/StudentProfileResponseDTO.class](/target/classes/com/sms/dto/profile/StudentProfileResponseDTO.class) | Java | 4 | 0 | 0 | 4 |
| [target/classes/com/sms/model/StudentProfile.class](/target/classes/com/sms/model/StudentProfile.class) | Java | 25 | 0 | 0 | 25 |
| [target/classes/com/sms/service/DashboardService.class](/target/classes/com/sms/service/DashboardService.class) | Java | -4 | 0 | 0 | -4 |
| [target/classes/com/sms/service/StudentProfileService.class](/target/classes/com/sms/service/StudentProfileService.class) | Java | 3 | 0 | 0 | 3 |
| [target/classes/com/sms/service/StudentService.class](/target/classes/com/sms/service/StudentService.class) | Java | 11 | 0 | 0 | 11 |
| [target/classes/static/css/dashboard.css](/target/classes/static/css/dashboard.css) | PostCSS | 85 | 0 | 17 | 102 |
| [target/classes/templates/admin-dashboard.html](/target/classes/templates/admin-dashboard.html) | HTML | -80 | -6 | -6 | -92 |
| [target/classes/templates/admin-profile.html](/target/classes/templates/admin-profile.html) | HTML | 118 | 0 | 14 | 132 |
| [target/classes/templates/admin-students.html](/target/classes/templates/admin-students.html) | HTML | 85 | 0 | 7 | 92 |
| [target/classes/templates/attendance-scanner.html](/target/classes/templates/attendance-scanner.html) | HTML | 65 | 0 | 1 | 66 |
| [target/classes/templates/fragments/sidebar.html](/target/classes/templates/fragments/sidebar.html) | HTML | 1 | 0 | 0 | 1 |
| [target/classes/templates/student-profile.html](/target/classes/templates/student-profile.html) | HTML | 72 | 0 | 8 | 80 |
| [target/classes/templates/teacher-attendance.html](/target/classes/templates/teacher-attendance.html) | HTML | 51 | 0 | 11 | 62 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details