package com.sms.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AnalyticsSnapshotScheduler {

    private final AiAnalyticsService aiAnalyticsService;
    private final AnalyticsSnapshotService analyticsSnapshotService;

    public AnalyticsSnapshotScheduler(AiAnalyticsService aiAnalyticsService,
                                      AnalyticsSnapshotService analyticsSnapshotService) {
        this.aiAnalyticsService = aiAnalyticsService;
        this.analyticsSnapshotService = analyticsSnapshotService;
    }

    @Scheduled(cron = "${app.analytics.snapshot.cron:0 0 1 * * *}")
    public void persistDailySnapshot() {
        Map<String, Object> dashboard = aiAnalyticsService.buildDashboard("ADMIN", "system", null, null, null, null, null);
        analyticsSnapshotService.saveSnapshot("ADMIN", "GLOBAL", "DAILY_ADMIN_DASHBOARD", dashboard);
    }
}
