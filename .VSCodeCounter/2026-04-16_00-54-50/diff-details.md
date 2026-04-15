# Diff Details

Date : 2026-04-16 00:54:50

Directory f:\\Coding\\studentmanagement

Total : 38 files,  2141 codes, 84 comments, 397 blanks, all 2622 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [pom.xml](/pom.xml) | XML | 24 | 2 | 2 | 28 |
| [src/main/java/com/sms/config/DemoDataLoader.java](/src/main/java/com/sms/config/DemoDataLoader.java) | Java | 12 | 0 | 0 | 12 |
| [src/main/java/com/sms/controller/DigitalIDApi.java](/src/main/java/com/sms/controller/DigitalIDApi.java) | Java | 156 | 29 | 22 | 207 |
| [src/main/java/com/sms/controller/StudentController.java](/src/main/java/com/sms/controller/StudentController.java) | Java | 4 | 0 | 1 | 5 |
| [src/main/java/com/sms/controller/StudentProfileApiController.java](/src/main/java/com/sms/controller/StudentProfileApiController.java) | Java | 23 | 0 | 7 | 30 |
| [src/main/java/com/sms/controller/StudentViewController.java](/src/main/java/com/sms/controller/StudentViewController.java) | Java | 16 | 11 | 5 | 32 |
| [src/main/java/com/sms/dto/identity/DigitalIDCardDTO.java](/src/main/java/com/sms/dto/identity/DigitalIDCardDTO.java) | Java | 19 | 4 | 3 | 26 |
| [src/main/java/com/sms/dto/identity/VerificationResponseDTO.java](/src/main/java/com/sms/dto/identity/VerificationResponseDTO.java) | Java | 14 | 4 | 3 | 21 |
| [src/main/java/com/sms/dto/student/StudentProfileDTO.java](/src/main/java/com/sms/dto/student/StudentProfileDTO.java) | Java | 101 | 0 | 34 | 135 |
| [src/main/java/com/sms/model/Student.java](/src/main/java/com/sms/model/Student.java) | Java | 78 | 0 | 26 | 104 |
| [src/main/java/com/sms/service/QRService.java](/src/main/java/com/sms/service/QRService.java) | Java | 79 | 18 | 17 | 114 |
| [src/main/java/com/sms/service/StudentService.java](/src/main/java/com/sms/service/StudentService.java) | Java | 33 | 0 | 8 | 41 |
| [src/main/resources/static/css/dashboard.css](/src/main/resources/static/css/dashboard.css) | PostCSS | 161 | 1 | 29 | 191 |
| [src/main/resources/templates/fragments/sidebar.html](/src/main/resources/templates/fragments/sidebar.html) | HTML | 1 | 0 | 0 | 1 |
| [src/main/resources/templates/student-id-card.html](/src/main/resources/templates/student-id-card.html) | HTML | 522 | 5 | 79 | 606 |
| [src/main/resources/templates/student-profile.html](/src/main/resources/templates/student-profile.html) | HTML | 144 | 0 | 28 | 172 |
| [src/test/java/com/sms/controller/DashboardControllerTest.java](/src/test/java/com/sms/controller/DashboardControllerTest.java) | Java | 15 | 0 | 2 | 17 |
| [target/classes/com/sms/config/DemoDataLoader.class](/target/classes/com/sms/config/DemoDataLoader.class) | Java | 18 | 0 | 0 | 18 |
| [target/classes/com/sms/controller/DigitalIDApi.class](/target/classes/com/sms/controller/DigitalIDApi.class) | Java | 72 | 0 | 0 | 72 |
| [target/classes/com/sms/controller/StudentController.class](/target/classes/com/sms/controller/StudentController.class) | Java | 1 | 0 | 0 | 1 |
| [target/classes/com/sms/controller/StudentProfileApiController.class](/target/classes/com/sms/controller/StudentProfileApiController.class) | Java | 11 | 0 | 0 | 11 |
| [target/classes/com/sms/controller/StudentViewController.class](/target/classes/com/sms/controller/StudentViewController.class) | Java | 8 | 0 | 0 | 8 |
| [target/classes/com/sms/dto/identity/DigitalIDCardDTO.class](/target/classes/com/sms/dto/identity/DigitalIDCardDTO.class) | Java | 12 | 4 | 0 | 16 |
| [target/classes/com/sms/dto/identity/VerificationResponseDTO.class](/target/classes/com/sms/dto/identity/VerificationResponseDTO.class) | Java | 16 | 0 | 0 | 16 |
| [target/classes/com/sms/dto/student/StudentProfileDTO.class](/target/classes/com/sms/dto/student/StudentProfileDTO.class) | Java | 28 | 0 | 0 | 28 |
| [target/classes/com/sms/model/Student.class](/target/classes/com/sms/model/Student.class) | Java | 20 | 0 | 0 | 20 |
| [target/classes/com/sms/service/DashboardService.class](/target/classes/com/sms/service/DashboardService.class) | Java | -5 | 0 | 0 | -5 |
| [target/classes/com/sms/service/QRService$IDTokenClaims.class](/target/classes/com/sms/service/QRService$IDTokenClaims.class) | Java | 13 | 0 | 0 | 13 |
| [target/classes/com/sms/service/QRService.class](/target/classes/com/sms/service/QRService.class) | Java | 40 | 0 | 0 | 40 |
| [target/classes/com/sms/service/StudentService.class](/target/classes/com/sms/service/StudentService.class) | Java | 52 | 0 | 0 | 52 |
| [target/classes/static/css/dashboard.css](/target/classes/static/css/dashboard.css) | PostCSS | 161 | 1 | 29 | 191 |
| [target/classes/templates/fragments/sidebar.html](/target/classes/templates/fragments/sidebar.html) | HTML | 1 | 0 | 0 | 1 |
| [target/classes/templates/student-id-card.html](/target/classes/templates/student-id-card.html) | HTML | 522 | 5 | 79 | 606 |
| [target/classes/templates/student-profile.html](/target/classes/templates/student-profile.html) | HTML | 144 | 0 | 28 | 172 |
| [target/maven-archiver/pom.properties](/target/maven-archiver/pom.properties) | Java Properties | -3 | 0 | -1 | -4 |
| [target/surefire-reports/TEST-com.sms.controller.DashboardControllerTest.xml](/target/surefire-reports/TEST-com.sms.controller.DashboardControllerTest.xml) | XML | -224 | 0 | -2 | -226 |
| [target/surefire-reports/TEST-com.sms.service.DashboardServiceTest.xml](/target/surefire-reports/TEST-com.sms.service.DashboardServiceTest.xml) | XML | -146 | 0 | -2 | -148 |
| [target/test-classes/com/sms/controller/DashboardControllerTest.class](/target/test-classes/com/sms/controller/DashboardControllerTest.class) | Java | -2 | 0 | 0 | -2 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details