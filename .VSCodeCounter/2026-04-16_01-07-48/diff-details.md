# Diff Details

Date : 2026-04-16 01:07:48

Directory f:\\Coding\\studentmanagement

Total : 33 files,  3690 codes, 236 comments, 613 blanks, all 4539 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [src/main/java/com/sms/controller/StudentAttendanceController.java](/src/main/java/com/sms/controller/StudentAttendanceController.java) | Java | 172 | 38 | 29 | 239 |
| [src/main/java/com/sms/controller/StudentViewController.java](/src/main/java/com/sms/controller/StudentViewController.java) | Java | -4 | -4 | -1 | -9 |
| [src/main/java/com/sms/controller/TeacherAttendanceController.java](/src/main/java/com/sms/controller/TeacherAttendanceController.java) | Java | 163 | 38 | 32 | 233 |
| [src/main/java/com/sms/dto/attendance/AttendanceQRResponse.java](/src/main/java/com/sms/dto/attendance/AttendanceQRResponse.java) | Java | 63 | 3 | 18 | 84 |
| [src/main/java/com/sms/dto/attendance/GenerateAttendanceQRRequest.java](/src/main/java/com/sms/dto/attendance/GenerateAttendanceQRRequest.java) | Java | 31 | 3 | 11 | 45 |
| [src/main/java/com/sms/dto/attendance/ManualAttendanceRequest.java](/src/main/java/com/sms/dto/attendance/ManualAttendanceRequest.java) | Java | 53 | 6 | 18 | 77 |
| [src/main/java/com/sms/dto/attendance/MarkAttendanceRequest.java](/src/main/java/com/sms/dto/attendance/MarkAttendanceRequest.java) | Java | 44 | 3 | 14 | 61 |
| [src/main/java/com/sms/dto/attendance/MarkAttendanceResponse.java](/src/main/java/com/sms/dto/attendance/MarkAttendanceResponse.java) | Java | 52 | 3 | 15 | 70 |
| [src/main/java/com/sms/model/Attendance.java](/src/main/java/com/sms/model/Attendance.java) | Java | 125 | 6 | 42 | 173 |
| [src/main/java/com/sms/repository/AttendanceRepository.java](/src/main/java/com/sms/repository/AttendanceRepository.java) | Java | 62 | 31 | 13 | 106 |
| [src/main/java/com/sms/service/AttendanceQRTokenService.java](/src/main/java/com/sms/service/AttendanceQRTokenService.java) | Java | 110 | 31 | 24 | 165 |
| [src/main/java/com/sms/service/AttendanceService.java](/src/main/java/com/sms/service/AttendanceService.java) | Java | 123 | 36 | 35 | 194 |
| [src/main/resources/templates/attendance-scanner.html](/src/main/resources/templates/attendance-scanner.html) | HTML | 475 | 9 | 79 | 563 |
| [src/main/resources/templates/teacher-attendance.html](/src/main/resources/templates/teacher-attendance.html) | HTML | 610 | 12 | 101 | 723 |
| [target/classes/com/sms/controller/StudentAttendanceController.class](/target/classes/com/sms/controller/StudentAttendanceController.class) | Java | 98 | 0 | 1 | 99 |
| [target/classes/com/sms/controller/StudentViewController.class](/target/classes/com/sms/controller/StudentViewController.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/controller/TeacherAttendanceController.class](/target/classes/com/sms/controller/TeacherAttendanceController.class) | Java | 85 | 0 | 1 | 86 |
| [target/classes/com/sms/dto/attendance/AttendanceQRResponse.class](/target/classes/com/sms/dto/attendance/AttendanceQRResponse.class) | Java | 25 | 0 | 0 | 25 |
| [target/classes/com/sms/dto/attendance/GenerateAttendanceQRRequest.class](/target/classes/com/sms/dto/attendance/GenerateAttendanceQRRequest.class) | Java | 26 | 0 | 0 | 26 |
| [target/classes/com/sms/dto/attendance/ManualAttendanceRequest$StudentAttendanceRecord.class](/target/classes/com/sms/dto/attendance/ManualAttendanceRequest$StudentAttendanceRecord.class) | Java | 18 | 0 | 0 | 18 |
| [target/classes/com/sms/dto/attendance/ManualAttendanceRequest.class](/target/classes/com/sms/dto/attendance/ManualAttendanceRequest.class) | Java | 16 | 0 | 0 | 16 |
| [target/classes/com/sms/dto/attendance/MarkAttendanceRequest.class](/target/classes/com/sms/dto/attendance/MarkAttendanceRequest.class) | Java | 20 | 0 | 0 | 20 |
| [target/classes/com/sms/dto/attendance/MarkAttendanceResponse.class](/target/classes/com/sms/dto/attendance/MarkAttendanceResponse.class) | Java | 21 | 0 | 0 | 21 |
| [target/classes/com/sms/model/Attendance.class](/target/classes/com/sms/model/Attendance.class) | Java | 39 | 0 | 0 | 39 |
| [target/classes/com/sms/repository/AttendanceRepository.class](/target/classes/com/sms/repository/AttendanceRepository.class) | Java | 17 | 0 | 0 | 17 |
| [target/classes/com/sms/service/AttendanceQRTokenService$AttendanceTokenClaims.class](/target/classes/com/sms/service/AttendanceQRTokenService$AttendanceTokenClaims.class) | Java | 10 | 0 | 0 | 10 |
| [target/classes/com/sms/service/AttendanceQRTokenService.class](/target/classes/com/sms/service/AttendanceQRTokenService.class) | Java | 66 | 0 | 0 | 66 |
| [target/classes/com/sms/service/AttendanceService$AttendanceStats.class](/target/classes/com/sms/service/AttendanceService$AttendanceStats.class) | Java | 11 | 0 | 0 | 11 |
| [target/classes/com/sms/service/AttendanceService$ManualAttendanceRecord.class](/target/classes/com/sms/service/AttendanceService$ManualAttendanceRecord.class) | Java | 8 | 0 | 0 | 8 |
| [target/classes/com/sms/service/AttendanceService.class](/target/classes/com/sms/service/AttendanceService.class) | Java | 64 | 0 | 0 | 64 |
| [target/classes/templates/attendance-scanner.html](/target/classes/templates/attendance-scanner.html) | HTML | 475 | 9 | 79 | 563 |
| [target/classes/templates/teacher-attendance.html](/target/classes/templates/teacher-attendance.html) | HTML | 610 | 12 | 101 | 723 |
| [target/maven-archiver/pom.properties](/target/maven-archiver/pom.properties) | Java Properties | 3 | 0 | 1 | 4 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details