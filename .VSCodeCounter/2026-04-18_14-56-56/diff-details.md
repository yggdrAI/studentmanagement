# Diff Details

Date : 2026-04-18 14:56:56

Directory f:\\Coding\\studentmanagement

Total : 35 files,  2273 codes, -10 comments, 357 blanks, all 2620 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [src/main/java/com/sms/controller/DietPlannerController.java](/src/main/java/com/sms/controller/DietPlannerController.java) | Java | 49 | 0 | 9 | 58 |
| [src/main/java/com/sms/dto/diet/DailyCaloriePoint.java](/src/main/java/com/sms/dto/diet/DailyCaloriePoint.java) | Java | 6 | 0 | 2 | 8 |
| [src/main/java/com/sms/dto/diet/DietLogBatchRequest.java](/src/main/java/com/sms/dto/diet/DietLogBatchRequest.java) | Java | 48 | 0 | 17 | 65 |
| [src/main/java/com/sms/dto/diet/DietSuggestionResponse.java](/src/main/java/com/sms/dto/diet/DietSuggestionResponse.java) | Java | 15 | 0 | 3 | 18 |
| [src/main/java/com/sms/model/DietLog.java](/src/main/java/com/sms/model/DietLog.java) | Java | 68 | 0 | 23 | 91 |
| [src/main/java/com/sms/repository/DietLogRepository.java](/src/main/java/com/sms/repository/DietLogRepository.java) | Java | 11 | 0 | 6 | 17 |
| [src/main/java/com/sms/service/DietAIService.java](/src/main/java/com/sms/service/DietAIService.java) | Java | 42 | 0 | 7 | 49 |
| [src/main/java/com/sms/service/DietLogService.java](/src/main/java/com/sms/service/DietLogService.java) | Java | 138 | 0 | 30 | 168 |
| [src/main/java/com/sms/service/DietMLService.java](/src/main/java/com/sms/service/DietMLService.java) | Java | 64 | 0 | 14 | 78 |
| [src/main/resources/static/css/cafeteria.css](/src/main/resources/static/css/cafeteria.css) | PostCSS | 413 | 0 | 65 | 478 |
| [src/main/resources/static/js/cafeteria.js](/src/main/resources/static/js/cafeteria.js) | JavaScript | 327 | 0 | 55 | 382 |
| [src/main/resources/templates/cafeteria.html](/src/main/resources/templates/cafeteria.html) | HTML | 68 | 0 | 6 | 74 |
| [src/main/resources/templates/student-id-card.html](/src/main/resources/templates/student-id-card.html) | HTML | -10 | -5 | -3 | -18 |
| [target/classes/com/sms/config/RedisAnalyticsConfig.class](/target/classes/com/sms/config/RedisAnalyticsConfig.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/controller/DietPlannerController.class](/target/classes/com/sms/controller/DietPlannerController.class) | Java | 31 | 0 | 0 | 31 |
| [target/classes/com/sms/controller/StudentAttendanceController.class](/target/classes/com/sms/controller/StudentAttendanceController.class) | Java | -10 | 0 | 0 | -10 |
| [target/classes/com/sms/controller/TeacherAttendanceController.class](/target/classes/com/sms/controller/TeacherAttendanceController.class) | Java | -7 | 0 | -1 | -8 |
| [target/classes/com/sms/dto/diet/DailyCaloriePoint.class](/target/classes/com/sms/dto/diet/DailyCaloriePoint.class) | Java | 17 | 0 | 0 | 17 |
| [target/classes/com/sms/dto/diet/DietLogBatchRequest$MealEntry.class](/target/classes/com/sms/dto/diet/DietLogBatchRequest$MealEntry.class) | Java | 10 | 0 | 0 | 10 |
| [target/classes/com/sms/dto/diet/DietLogBatchRequest.class](/target/classes/com/sms/dto/diet/DietLogBatchRequest.class) | Java | 11 | 0 | 0 | 11 |
| [target/classes/com/sms/dto/diet/DietSuggestionResponse.class](/target/classes/com/sms/dto/diet/DietSuggestionResponse.class) | Java | 24 | 0 | 0 | 24 |
| [target/classes/com/sms/model/DietLog.class](/target/classes/com/sms/model/DietLog.class) | Java | 15 | 0 | 0 | 15 |
| [target/classes/com/sms/repository/DietLogRepository.class](/target/classes/com/sms/repository/DietLogRepository.class) | Java | 6 | 0 | 0 | 6 |
| [target/classes/com/sms/service/AnalyticsRealtimeNotifier.class](/target/classes/com/sms/service/AnalyticsRealtimeNotifier.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/service/AttendanceService$ManualAttendanceRecord.class](/target/classes/com/sms/service/AttendanceService$ManualAttendanceRecord.class) | Java | -1 | 0 | 0 | -1 |
| [target/classes/com/sms/service/AttendanceService.class](/target/classes/com/sms/service/AttendanceService.class) | Java | 6 | 0 | -1 | 5 |
| [target/classes/com/sms/service/DietAIService.class](/target/classes/com/sms/service/DietAIService.class) | Java | 18 | 0 | 0 | 18 |
| [target/classes/com/sms/service/DietLogService.class](/target/classes/com/sms/service/DietLogService.class) | Java | 84 | 0 | 0 | 84 |
| [target/classes/com/sms/service/DietMLService$DietMLResult.class](/target/classes/com/sms/service/DietMLService$DietMLResult.class) | Java | 8 | 0 | 0 | 8 |
| [target/classes/com/sms/service/DietMLService.class](/target/classes/com/sms/service/DietMLService.class) | Java | 27 | 0 | 0 | 27 |
| [target/classes/com/sms/service/StudentService.class](/target/classes/com/sms/service/StudentService.class) | Java | -1 | 0 | 2 | 1 |
| [target/classes/static/css/cafeteria.css](/target/classes/static/css/cafeteria.css) | PostCSS | 413 | 0 | 65 | 478 |
| [target/classes/static/js/cafeteria.js](/target/classes/static/js/cafeteria.js) | JavaScript | 327 | 0 | 55 | 382 |
| [target/classes/templates/cafeteria.html](/target/classes/templates/cafeteria.html) | HTML | 68 | 0 | 6 | 74 |
| [target/classes/templates/student-id-card.html](/target/classes/templates/student-id-card.html) | HTML | -10 | -5 | -3 | -18 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details