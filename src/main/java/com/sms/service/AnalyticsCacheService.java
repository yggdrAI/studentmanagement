package com.sms.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AnalyticsCacheService {

    private final CacheManager cacheManager;

    public AnalyticsCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAnalyticsCaches() {
        clear("analytics-dashboard");
        clear("analytics-student-summary");
        clear("analytics-live-snapshot");
        clear("analytics-snapshots");
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(Objects.requireNonNull(cacheName));
        if (cache != null) {
            cache.clear();
        }
    }
}
