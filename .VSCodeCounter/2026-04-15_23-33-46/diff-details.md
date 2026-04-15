# Diff Details

Date : 2026-04-15 23:33:46

Directory f:\\Coding\\studentmanagement

Total : 75 files,  1973 codes, 6 comments, 305 blanks, all 2284 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [pom.xml](/pom.xml) | XML | 38 | 0 | 3 | 41 |
| [src/main/java/com/sms/StudentManagementApplication.java](/src/main/java/com/sms/StudentManagementApplication.java) | Java | 2 | 0 | 0 | 2 |
| [src/main/java/com/sms/config/DemoDataLoader.java](/src/main/java/com/sms/config/DemoDataLoader.java) | Java | 132 | 0 | 21 | 153 |
| [src/main/java/com/sms/config/JwtAuthenticationFilter.java](/src/main/java/com/sms/config/JwtAuthenticationFilter.java) | Java | 48 | 0 | 15 | 63 |
| [src/main/java/com/sms/config/SecurityConfig.java](/src/main/java/com/sms/config/SecurityConfig.java) | Java | 8 | -2 | 1 | 7 |
| [src/main/java/com/sms/config/WebSocketConfig.java](/src/main/java/com/sms/config/WebSocketConfig.java) | Java | 19 | 0 | 5 | 24 |
| [src/main/java/com/sms/controller/ApiAuthController.java](/src/main/java/com/sms/controller/ApiAuthController.java) | Java | 33 | 0 | 10 | 43 |
| [src/main/java/com/sms/controller/DashboardController.java](/src/main/java/com/sms/controller/DashboardController.java) | Java | 45 | 0 | 10 | 55 |
| [src/main/java/com/sms/dto/auth/LoginRequest.java](/src/main/java/com/sms/dto/auth/LoginRequest.java) | Java | 20 | 0 | 9 | 29 |
| [src/main/java/com/sms/dto/auth/LoginResponse.java](/src/main/java/com/sms/dto/auth/LoginResponse.java) | Java | 10 | 0 | 5 | 15 |
| [src/main/java/com/sms/dto/dashboard/CourseProgressDto.java](/src/main/java/com/sms/dto/dashboard/CourseProgressDto.java) | Java | 45 | 0 | 15 | 60 |
| [src/main/java/com/sms/dto/dashboard/DashboardResponse.java](/src/main/java/com/sms/dto/dashboard/DashboardResponse.java) | Java | 60 | 0 | 20 | 80 |
| [src/main/java/com/sms/dto/dashboard/TaskDto.java](/src/main/java/com/sms/dto/dashboard/TaskDto.java) | Java | 40 | 0 | 15 | 55 |
| [src/main/java/com/sms/dto/dashboard/UpcomingClassDto.java](/src/main/java/com/sms/dto/dashboard/UpcomingClassDto.java) | Java | 53 | 0 | 18 | 71 |
| [src/main/java/com/sms/model/Admin.java](/src/main/java/com/sms/model/Admin.java) | Java | 1 | 0 | 2 | 3 |
| [src/main/java/com/sms/model/ClassSession.java](/src/main/java/com/sms/model/ClassSession.java) | Java | 68 | 0 | 22 | 90 |
| [src/main/java/com/sms/model/Course.java](/src/main/java/com/sms/model/Course.java) | Java | 13 | 0 | 1 | 14 |
| [src/main/java/com/sms/model/Enrollment.java](/src/main/java/com/sms/model/Enrollment.java) | Java | 10 | 0 | 1 | 11 |
| [src/main/java/com/sms/model/Student.java](/src/main/java/com/sms/model/Student.java) | Java | 14 | 0 | 4 | 18 |
| [src/main/java/com/sms/model/TaskItem.java](/src/main/java/com/sms/model/TaskItem.java) | Java | 71 | 0 | 23 | 94 |
| [src/main/java/com/sms/model/TaskStatus.java](/src/main/java/com/sms/model/TaskStatus.java) | Java | 5 | 0 | 2 | 7 |
| [src/main/java/com/sms/model/User.java](/src/main/java/com/sms/model/User.java) | Java | 8 | 0 | 1 | 9 |
| [src/main/java/com/sms/repository/ClassSessionRepository.java](/src/main/java/com/sms/repository/ClassSessionRepository.java) | Java | 10 | 0 | 5 | 15 |
| [src/main/java/com/sms/repository/CourseRepository.java](/src/main/java/com/sms/repository/CourseRepository.java) | Java | 2 | 0 | 2 | 4 |
| [src/main/java/com/sms/repository/EnrollmentRepository.java](/src/main/java/com/sms/repository/EnrollmentRepository.java) | Java | 9 | 0 | 5 | 14 |
| [src/main/java/com/sms/repository/StudentRepository.java](/src/main/java/com/sms/repository/StudentRepository.java) | Java | 2 | 0 | 2 | 4 |
| [src/main/java/com/sms/repository/TaskItemRepository.java](/src/main/java/com/sms/repository/TaskItemRepository.java) | Java | 11 | 0 | 6 | 17 |
| [src/main/java/com/sms/service/CustomUserDetailsService.java](/src/main/java/com/sms/service/CustomUserDetailsService.java) | Java | 23 | 0 | 8 | 31 |
| [src/main/java/com/sms/service/DashboardService.java](/src/main/java/com/sms/service/DashboardService.java) | Java | 146 | 0 | 25 | 171 |
| [src/main/java/com/sms/service/JwtService.java](/src/main/java/com/sms/service/JwtService.java) | Java | 48 | 0 | 15 | 63 |
| [src/main/resources/application-postgres.properties](/src/main/resources/application-postgres.properties) | Java Properties | 9 | 0 | 3 | 12 |
| [src/main/resources/application.properties](/src/main/resources/application.properties) | Java Properties | 4 | 2 | 2 | 8 |
| [src/test/java/com/sms/controller/DashboardControllerTest.java](/src/test/java/com/sms/controller/DashboardControllerTest.java) | Java | 42 | 0 | 9 | 51 |
| [src/test/java/com/sms/service/DashboardServiceTest.java](/src/test/java/com/sms/service/DashboardServiceTest.java) | Java | 36 | 0 | 11 | 47 |
| [target/classes/application-postgres.properties](/target/classes/application-postgres.properties) | Java Properties | 9 | 0 | 3 | 12 |
| [target/classes/application.properties](/target/classes/application.properties) | Java Properties | 4 | 2 | 2 | 8 |
| [target/classes/com/sms/StudentManagementApplication.class](/target/classes/com/sms/StudentManagementApplication.class) | Java | 1 | 0 | 0 | 1 |
| [target/classes/com/sms/config/DemoDataLoader.class](/target/classes/com/sms/config/DemoDataLoader.class) | Java | 79 | 0 | 0 | 79 |
| [target/classes/com/sms/config/JwtAuthenticationFilter.class](/target/classes/com/sms/config/JwtAuthenticationFilter.class) | Java | 28 | 0 | 0 | 28 |
| [target/classes/com/sms/config/SecurityConfig.class](/target/classes/com/sms/config/SecurityConfig.class) | Java | 3 | 4 | 0 | 7 |
| [target/classes/com/sms/config/WebSocketConfig.class](/target/classes/com/sms/config/WebSocketConfig.class) | Java | 10 | 0 | 0 | 10 |
| [target/classes/com/sms/controller/ApiAuthController.class](/target/classes/com/sms/controller/ApiAuthController.class) | Java | 15 | 0 | 0 | 15 |
| [target/classes/com/sms/controller/DashboardController.class](/target/classes/com/sms/controller/DashboardController.class) | Java | 17 | 0 | 0 | 17 |
| [target/classes/com/sms/dto/auth/LoginRequest.class](/target/classes/com/sms/dto/auth/LoginRequest.class) | Java | 8 | 0 | 0 | 8 |
| [target/classes/com/sms/dto/auth/LoginResponse.class](/target/classes/com/sms/dto/auth/LoginResponse.class) | Java | 8 | 0 | 0 | 8 |
| [target/classes/com/sms/dto/dashboard/CourseProgressDto.class](/target/classes/com/sms/dto/dashboard/CourseProgressDto.class) | Java | 22 | 0 | 0 | 22 |
| [target/classes/com/sms/dto/dashboard/DashboardResponse.class](/target/classes/com/sms/dto/dashboard/DashboardResponse.class) | Java | 16 | 0 | 0 | 16 |
| [target/classes/com/sms/dto/dashboard/TaskDto.class](/target/classes/com/sms/dto/dashboard/TaskDto.class) | Java | 14 | 0 | 0 | 14 |
| [target/classes/com/sms/dto/dashboard/UpcomingClassDto.class](/target/classes/com/sms/dto/dashboard/UpcomingClassDto.class) | Java | 16 | 0 | 0 | 16 |
| [target/classes/com/sms/model/Admin.class](/target/classes/com/sms/model/Admin.class) | Java | 2 | 0 | 0 | 2 |
| [target/classes/com/sms/model/ClassSession.class](/target/classes/com/sms/model/ClassSession.class) | Java | 16 | 0 | 0 | 16 |
| [target/classes/com/sms/model/Course.class](/target/classes/com/sms/model/Course.class) | Java | 5 | 0 | 0 | 5 |
| [target/classes/com/sms/model/Enrollment.class](/target/classes/com/sms/model/Enrollment.class) | Java | 3 | 0 | 0 | 3 |
| [target/classes/com/sms/model/LoginUser.class](/target/classes/com/sms/model/LoginUser.class) | Java | 7 | 0 | 0 | 7 |
| [target/classes/com/sms/model/Student.class](/target/classes/com/sms/model/Student.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/model/TaskItem.class](/target/classes/com/sms/model/TaskItem.class) | Java | 20 | 0 | 0 | 20 |
| [target/classes/com/sms/model/TaskStatus.class](/target/classes/com/sms/model/TaskStatus.class) | Java | 19 | 0 | 0 | 19 |
| [target/classes/com/sms/model/Teacher.class](/target/classes/com/sms/model/Teacher.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/model/User.class](/target/classes/com/sms/model/User.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/model/UserRole.class](/target/classes/com/sms/model/UserRole.class) | Java | 19 | 0 | 0 | 19 |
| [target/classes/com/sms/repository/ClassSessionRepository.class](/target/classes/com/sms/repository/ClassSessionRepository.class) | Java | 4 | 0 | 0 | 4 |
| [target/classes/com/sms/repository/CourseRepository.class](/target/classes/com/sms/repository/CourseRepository.class) | Java | 1 | 0 | 0 | 1 |
| [target/classes/com/sms/repository/EnrollmentRepository.class](/target/classes/com/sms/repository/EnrollmentRepository.class) | Java | 4 | 0 | 0 | 4 |
| [target/classes/com/sms/repository/TaskItemRepository.class](/target/classes/com/sms/repository/TaskItemRepository.class) | Java | 5 | 0 | 0 | 5 |
| [target/classes/com/sms/service/AuthenticationService.class](/target/classes/com/sms/service/AuthenticationService.class) | Java | 18 | 0 | 0 | 18 |
| [target/classes/com/sms/service/CustomUserDetailsService.class](/target/classes/com/sms/service/CustomUserDetailsService.class) | Java | 24 | 0 | 0 | 24 |
| [target/classes/com/sms/service/DashboardService.class](/target/classes/com/sms/service/DashboardService.class) | Java | 143 | 0 | 0 | 143 |
| [target/classes/com/sms/service/JwtService.class](/target/classes/com/sms/service/JwtService.class) | Java | 29 | 0 | 0 | 29 |
| [target/classes/model/LoginUser.class](/target/classes/model/LoginUser.class) | Java | -7 | 0 | 0 | -7 |
| [target/classes/model/UserRole.class](/target/classes/model/UserRole.class) | Java | -19 | 0 | 0 | -19 |
| [target/classes/service/AuthenticationService.class](/target/classes/service/AuthenticationService.class) | Java | -18 | 0 | 0 | -18 |
| [target/surefire-reports/TEST-com.sms.controller.DashboardControllerTest.xml](/target/surefire-reports/TEST-com.sms.controller.DashboardControllerTest.xml) | XML | 187 | 0 | 2 | 189 |
| [target/surefire-reports/TEST-com.sms.service.DashboardServiceTest.xml](/target/surefire-reports/TEST-com.sms.service.DashboardServiceTest.xml) | XML | 127 | 0 | 2 | 129 |
| [target/test-classes/com/sms/controller/DashboardControllerTest.class](/target/test-classes/com/sms/controller/DashboardControllerTest.class) | Java | 23 | 0 | 0 | 23 |
| [target/test-classes/com/sms/service/DashboardServiceTest.class](/target/test-classes/com/sms/service/DashboardServiceTest.class) | Java | 29 | 0 | 0 | 29 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details