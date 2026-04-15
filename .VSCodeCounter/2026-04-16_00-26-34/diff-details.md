# Diff Details

Date : 2026-04-16 00:26:34

Directory f:\\Coding\\studentmanagement

Total : 53 files,  1265 codes, -1 comments, 241 blanks, all 1505 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [src/main/java/com/sms/config/DemoDataLoader.java](/src/main/java/com/sms/config/DemoDataLoader.java) | Java | 10 | 0 | 2 | 12 |
| [src/main/java/com/sms/config/SecurityConfig.java](/src/main/java/com/sms/config/SecurityConfig.java) | Java | 2 | 0 | 0 | 2 |
| [src/main/java/com/sms/config/WebSocketConfig.java](/src/main/java/com/sms/config/WebSocketConfig.java) | Java | 2 | 0 | 0 | 2 |
| [src/main/java/com/sms/controller/AdminApiController.java](/src/main/java/com/sms/controller/AdminApiController.java) | Java | 49 | 0 | 11 | 60 |
| [src/main/java/com/sms/controller/ApiExceptionHandler.java](/src/main/java/com/sms/controller/ApiExceptionHandler.java) | Java | 74 | 0 | 13 | 87 |
| [src/main/java/com/sms/controller/AuthController.java](/src/main/java/com/sms/controller/AuthController.java) | Java | 4 | 0 | 1 | 5 |
| [src/main/java/com/sms/controller/DashboardController.java](/src/main/java/com/sms/controller/DashboardController.java) | Java | 2 | 0 | -1 | 1 |
| [src/main/java/com/sms/controller/TeacherApiController.java](/src/main/java/com/sms/controller/TeacherApiController.java) | Java | 64 | 0 | 12 | 76 |
| [src/main/java/com/sms/dto/dashboard/AssignTeacherRequest.java](/src/main/java/com/sms/dto/dashboard/AssignTeacherRequest.java) | Java | 20 | 0 | 9 | 29 |
| [src/main/java/com/sms/dto/dashboard/CreateSubjectRequest.java](/src/main/java/com/sms/dto/dashboard/CreateSubjectRequest.java) | Java | 29 | 0 | 12 | 41 |
| [src/main/java/com/sms/dto/dashboard/CreateTaskRequest.java](/src/main/java/com/sms/dto/dashboard/CreateTaskRequest.java) | Java | 37 | 0 | 16 | 53 |
| [src/main/java/com/sms/dto/dashboard/EnrollStudentRequest.java](/src/main/java/com/sms/dto/dashboard/EnrollStudentRequest.java) | Java | 21 | 0 | 9 | 30 |
| [src/main/java/com/sms/dto/dashboard/ScheduleClassRequest.java](/src/main/java/com/sms/dto/dashboard/ScheduleClassRequest.java) | Java | 46 | 0 | 19 | 65 |
| [src/main/java/com/sms/dto/dashboard/StudentProgressViewDto.java](/src/main/java/com/sms/dto/dashboard/StudentProgressViewDto.java) | Java | 24 | 0 | 9 | 33 |
| [src/main/java/com/sms/model/Enrollment.java](/src/main/java/com/sms/model/Enrollment.java) | Java | -5 | 0 | 0 | -5 |
| [src/main/java/com/sms/model/StudentTask.java](/src/main/java/com/sms/model/StudentTask.java) | Java | 51 | 0 | 16 | 67 |
| [src/main/java/com/sms/model/TaskItem.java](/src/main/java/com/sms/model/TaskItem.java) | Java | -10 | 0 | -3 | -13 |
| [src/main/java/com/sms/repository/CourseRepository.java](/src/main/java/com/sms/repository/CourseRepository.java) | Java | 1 | 0 | 1 | 2 |
| [src/main/java/com/sms/repository/EnrollmentRepository.java](/src/main/java/com/sms/repository/EnrollmentRepository.java) | Java | 2 | 0 | 2 | 4 |
| [src/main/java/com/sms/repository/StudentTaskRepository.java](/src/main/java/com/sms/repository/StudentTaskRepository.java) | Java | 14 | 0 | 9 | 23 |
| [src/main/java/com/sms/repository/TaskItemRepository.java](/src/main/java/com/sms/repository/TaskItemRepository.java) | Java | 1 | 0 | 1 | 2 |
| [src/main/java/com/sms/repository/TeacherRepository.java](/src/main/java/com/sms/repository/TeacherRepository.java) | Java | 2 | 0 | 2 | 4 |
| [src/main/java/com/sms/service/DashboardService.java](/src/main/java/com/sms/service/DashboardService.java) | Java | 133 | 0 | 26 | 159 |
| [src/main/resources/templates/student-dashboard.html](/src/main/resources/templates/student-dashboard.html) | HTML | 157 | 0 | 31 | 188 |
| [src/test/java/com/sms/controller/DashboardControllerTest.java](/src/test/java/com/sms/controller/DashboardControllerTest.java) | Java | 51 | 0 | 8 | 59 |
| [src/test/java/com/sms/service/DashboardServiceTest.java](/src/test/java/com/sms/service/DashboardServiceTest.java) | Java | 9 | 0 | 2 | 11 |
| [target/classes/com/sms/config/SecurityConfig.class](/target/classes/com/sms/config/SecurityConfig.class) | Java | 0 | -1 | 0 | -1 |
| [target/classes/com/sms/config/WebSocketConfig.class](/target/classes/com/sms/config/WebSocketConfig.class) | Java | 3 | 0 | 0 | 3 |
| [target/classes/com/sms/controller/AdminApiController.class](/target/classes/com/sms/controller/AdminApiController.class) | Java | 31 | 0 | 0 | 31 |
| [target/classes/com/sms/controller/ApiExceptionHandler.class](/target/classes/com/sms/controller/ApiExceptionHandler.class) | Java | 34 | 0 | 0 | 34 |
| [target/classes/com/sms/controller/DashboardController.class](/target/classes/com/sms/controller/DashboardController.class) | Java | -3 | 0 | 0 | -3 |
| [target/classes/com/sms/controller/TeacherApiController.class](/target/classes/com/sms/controller/TeacherApiController.class) | Java | 33 | 0 | 0 | 33 |
| [target/classes/com/sms/dto/dashboard/AssignTeacherRequest.class](/target/classes/com/sms/dto/dashboard/AssignTeacherRequest.class) | Java | 8 | 0 | 0 | 8 |
| [target/classes/com/sms/dto/dashboard/CreateSubjectRequest.class](/target/classes/com/sms/dto/dashboard/CreateSubjectRequest.class) | Java | 19 | 0 | 0 | 19 |
| [target/classes/com/sms/dto/dashboard/CreateTaskRequest.class](/target/classes/com/sms/dto/dashboard/CreateTaskRequest.class) | Java | 10 | 0 | 0 | 10 |
| [target/classes/com/sms/dto/dashboard/EnrollStudentRequest.class](/target/classes/com/sms/dto/dashboard/EnrollStudentRequest.class) | Java | 8 | 0 | 0 | 8 |
| [target/classes/com/sms/dto/dashboard/ScheduleClassRequest.class](/target/classes/com/sms/dto/dashboard/ScheduleClassRequest.class) | Java | 11 | 0 | 0 | 11 |
| [target/classes/com/sms/dto/dashboard/StudentProgressViewDto.class](/target/classes/com/sms/dto/dashboard/StudentProgressViewDto.class) | Java | 10 | 0 | 0 | 10 |
| [target/classes/com/sms/model/Enrollment.class](/target/classes/com/sms/model/Enrollment.class) | Java | 0 | 0 | 3 | 3 |
| [target/classes/com/sms/model/Student.class](/target/classes/com/sms/model/Student.class) | Java | 2 | 0 | 0 | 2 |
| [target/classes/com/sms/model/StudentTask.class](/target/classes/com/sms/model/StudentTask.class) | Java | 13 | 0 | 0 | 13 |
| [target/classes/com/sms/model/TaskItem.class](/target/classes/com/sms/model/TaskItem.class) | Java | -6 | 0 | 0 | -6 |
| [target/classes/com/sms/model/Teacher.class](/target/classes/com/sms/model/Teacher.class) | Java | 1 | 0 | 0 | 1 |
| [target/classes/com/sms/model/User.class](/target/classes/com/sms/model/User.class) | Java | 1 | 0 | 0 | 1 |
| [target/classes/com/sms/repository/EnrollmentRepository.class](/target/classes/com/sms/repository/EnrollmentRepository.class) | Java | 1 | 0 | 0 | 1 |
| [target/classes/com/sms/repository/StudentTaskRepository.class](/target/classes/com/sms/repository/StudentTaskRepository.class) | Java | 7 | 0 | 0 | 7 |
| [target/classes/com/sms/service/CustomUserDetailsService.class](/target/classes/com/sms/service/CustomUserDetailsService.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/service/DashboardService.class](/target/classes/com/sms/service/DashboardService.class) | Java | 71 | 0 | 0 | 71 |
| [target/classes/templates/student-dashboard.html](/target/classes/templates/student-dashboard.html) | HTML | 157 | 0 | 31 | 188 |
| [target/surefire-reports/TEST-com.sms.controller.DashboardControllerTest.xml](/target/surefire-reports/TEST-com.sms.controller.DashboardControllerTest.xml) | XML | 41 | 0 | 0 | 41 |
| [target/surefire-reports/TEST-com.sms.service.DashboardServiceTest.xml](/target/surefire-reports/TEST-com.sms.service.DashboardServiceTest.xml) | XML | 19 | 0 | 0 | 19 |
| [target/test-classes/com/sms/controller/DashboardControllerTest.class](/target/test-classes/com/sms/controller/DashboardControllerTest.class) | Java | 6 | 0 | 0 | 6 |
| [target/test-classes/com/sms/service/DashboardServiceTest.class](/target/test-classes/com/sms/service/DashboardServiceTest.class) | Java | -1 | 0 | 0 | -1 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details