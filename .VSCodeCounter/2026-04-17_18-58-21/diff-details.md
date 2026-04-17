# Diff Details

Date : 2026-04-17 18:58:21

Directory f:\\Coding\\studentmanagement

Total : 42 files,  1276 codes, 8 comments, 175 blanks, all 1459 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [src/main/java/com/sms/controller/CampusLocationWebSocketController.java](/src/main/java/com/sms/controller/CampusLocationWebSocketController.java) | Java | 37 | 0 | 6 | 43 |
| [src/main/java/com/sms/controller/StudentAttendanceController.java](/src/main/java/com/sms/controller/StudentAttendanceController.java) | Java | 63 | 0 | 6 | 69 |
| [src/main/java/com/sms/controller/TeacherController.java](/src/main/java/com/sms/controller/TeacherController.java) | Java | 5 | 0 | 1 | 6 |
| [src/main/java/com/sms/controller/TeacherFraudController.java](/src/main/java/com/sms/controller/TeacherFraudController.java) | Java | 28 | 0 | 6 | 34 |
| [src/main/java/com/sms/dto/attendance/FraudAlertDTO.java](/src/main/java/com/sms/dto/attendance/FraudAlertDTO.java) | Java | 35 | 3 | 3 | 41 |
| [src/main/java/com/sms/dto/attendance/MarkAttendanceRequest.java](/src/main/java/com/sms/dto/attendance/MarkAttendanceRequest.java) | Java | 7 | 0 | 2 | 9 |
| [src/main/java/com/sms/dto/attendance/MarkAttendanceResponse.java](/src/main/java/com/sms/dto/attendance/MarkAttendanceResponse.java) | Java | 21 | 0 | 6 | 27 |
| [src/main/java/com/sms/dto/campus/LocationDTO.java](/src/main/java/com/sms/dto/campus/LocationDTO.java) | Java | 34 | 3 | 3 | 40 |
| [src/main/java/com/sms/model/FraudLog.java](/src/main/java/com/sms/model/FraudLog.java) | Java | 75 | 0 | 17 | 92 |
| [src/main/java/com/sms/repository/FraudLogRepository.java](/src/main/java/com/sms/repository/FraudLogRepository.java) | Java | 19 | 0 | 5 | 24 |
| [src/main/java/com/sms/repository/SecurityAuditRepository.java](/src/main/java/com/sms/repository/SecurityAuditRepository.java) | Java | 4 | 0 | 1 | 5 |
| [src/main/java/com/sms/service/CampusTrackingService.java](/src/main/java/com/sms/service/CampusTrackingService.java) | Java | 1 | 0 | 0 | 1 |
| [src/main/java/com/sms/service/FraudDetectionService.java](/src/main/java/com/sms/service/FraudDetectionService.java) | Java | 245 | 0 | 40 | 285 |
| [src/main/resources/application.properties](/src/main/resources/application.properties) | Java Properties | 1 | 1 | 1 | 3 |
| [src/main/resources/templates/attendance-scanner.html](/src/main/resources/templates/attendance-scanner.html) | HTML | 73 | 0 | 13 | 86 |
| [src/main/resources/templates/teacher-dashboard.html](/src/main/resources/templates/teacher-dashboard.html) | HTML | 133 | 0 | 25 | 158 |
| [target/classes/application.properties](/target/classes/application.properties) | Java Properties | 1 | 1 | 1 | 3 |
| [target/classes/com/sms/controller/CampusLocationWebSocketController.class](/target/classes/com/sms/controller/CampusLocationWebSocketController.class) | Java | 42 | 0 | 0 | 42 |
| [target/classes/com/sms/controller/CampusTrackingController.class](/target/classes/com/sms/controller/CampusTrackingController.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/controller/StudentAttendanceController.class](/target/classes/com/sms/controller/StudentAttendanceController.class) | Java | 28 | 0 | 0 | 28 |
| [target/classes/com/sms/controller/TeacherController.class](/target/classes/com/sms/controller/TeacherController.class) | Java | 2 | 0 | 0 | 2 |
| [target/classes/com/sms/controller/TeacherFraudController.class](/target/classes/com/sms/controller/TeacherFraudController.class) | Java | 20 | 0 | 0 | 20 |
| [target/classes/com/sms/dto/attendance/FaceRegistrationRequest.class](/target/classes/com/sms/dto/attendance/FaceRegistrationRequest.class) | Java | -2 | 0 | 0 | -2 |
| [target/classes/com/sms/dto/attendance/FraudAlertDTO.class](/target/classes/com/sms/dto/attendance/FraudAlertDTO.class) | Java | 18 | 0 | 0 | 18 |
| [target/classes/com/sms/dto/attendance/MarkAttendanceRequest.class](/target/classes/com/sms/dto/attendance/MarkAttendanceRequest.class) | Java | -10 | 0 | 0 | -10 |
| [target/classes/com/sms/dto/attendance/MarkAttendanceResponse.class](/target/classes/com/sms/dto/attendance/MarkAttendanceResponse.class) | Java | 2 | 0 | 0 | 2 |
| [target/classes/com/sms/dto/campus/CampusLocationUpdateDTO.class](/target/classes/com/sms/dto/campus/CampusLocationUpdateDTO.class) | Java | -10 | 0 | 0 | -10 |
| [target/classes/com/sms/dto/campus/LocationDTO.class](/target/classes/com/sms/dto/campus/LocationDTO.class) | Java | 10 | 0 | 0 | 10 |
| [target/classes/com/sms/model/FaceData.class](/target/classes/com/sms/model/FaceData.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/model/FraudLog.class](/target/classes/com/sms/model/FraudLog.class) | Java | 18 | 0 | 0 | 18 |
| [target/classes/com/sms/model/StudentLocation.class](/target/classes/com/sms/model/StudentLocation.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/repository/FaceDataRepository.class](/target/classes/com/sms/repository/FaceDataRepository.class) | Java | 1 | 0 | 0 | 1 |
| [target/classes/com/sms/repository/FraudLogRepository.class](/target/classes/com/sms/repository/FraudLogRepository.class) | Java | 7 | 0 | 0 | 7 |
| [target/classes/com/sms/repository/SecurityAuditRepository.class](/target/classes/com/sms/repository/SecurityAuditRepository.class) | Java | 3 | 0 | 0 | 3 |
| [target/classes/com/sms/repository/StudentLocationRepository.class](/target/classes/com/sms/repository/StudentLocationRepository.class) | Java | 1 | 0 | 0 | 1 |
| [target/classes/com/sms/service/CampusTrackingService.class](/target/classes/com/sms/service/CampusTrackingService.class) | Java | 5 | 0 | 0 | 5 |
| [target/classes/com/sms/service/FaceVerificationService$FaceVerificationResult.class](/target/classes/com/sms/service/FaceVerificationService$FaceVerificationResult.class) | Java | -2 | 0 | 0 | -2 |
| [target/classes/com/sms/service/FraudDetectionService$FraudAssessment.class](/target/classes/com/sms/service/FraudDetectionService$FraudAssessment.class) | Java | 20 | 0 | 0 | 20 |
| [target/classes/com/sms/service/FraudDetectionService$FraudSummary.class](/target/classes/com/sms/service/FraudDetectionService$FraudSummary.class) | Java | 10 | 0 | 0 | 10 |
| [target/classes/com/sms/service/FraudDetectionService.class](/target/classes/com/sms/service/FraudDetectionService.class) | Java | 128 | 0 | 1 | 129 |
| [target/classes/templates/attendance-scanner.html](/target/classes/templates/attendance-scanner.html) | HTML | 73 | 0 | 13 | 86 |
| [target/classes/templates/teacher-dashboard.html](/target/classes/templates/teacher-dashboard.html) | HTML | 133 | 0 | 25 | 158 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details