package com.sms.service;

import com.sms.model.ClassSession;
import com.sms.repository.ClassSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TimetableNotificationScheduler {

    private final ClassSessionRepository classSessionRepository;
    private final NotificationService notificationService;
    private final Set<String> dispatchedKeys = ConcurrentHashMap.newKeySet();

    public TimetableNotificationScheduler(ClassSessionRepository classSessionRepository,
                                         NotificationService notificationService) {
        this.classSessionRepository = classSessionRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional(readOnly = true)
    public void dispatchUpcomingClassReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusMinutes(9);
        LocalDateTime to = now.plusMinutes(10);

        for (ClassSession session : classSessionRepository.findByStartsAtBetween(from, to)) {
            if (session == null || session.getId() == null || session.getStartsAt() == null) {
                continue;
            }

            String dedupeKey = session.getId() + "#" + session.getStartsAt();
            if (!dispatchedKeys.add(dedupeKey)) {
                continue;
            }

            String title = session.getTitle() != null ? session.getTitle() : "Class";
            String message = title + " starts in 10 minutes";
            String courseCode = session.getCourse() != null ? session.getCourse().getCode() : null;

            Set<String> targetUsernames = new HashSet<>();
            if (session.getStudent() != null && session.getStudent().getUser() != null) {
                String username = session.getStudent().getUser().getUsername();
                if (username != null && !username.isBlank()) {
                    targetUsernames.add(username);
                }
            }

            if (targetUsernames.isEmpty()) {
                notificationService.notifyUpcomingClass(
                        message,
                        session.getId(),
                        courseCode,
                        session.getStartsAt().toString()
                );
                continue;
            }

            for (String username : targetUsernames) {
                notificationService.notifyUpcomingClassToUser(
                        username,
                        message,
                        session.getId(),
                        courseCode,
                        session.getStartsAt().toString()
                );
            }
        }

        if (dispatchedKeys.size() > 5000) {
            dispatchedKeys.clear();
        }
    }
}
