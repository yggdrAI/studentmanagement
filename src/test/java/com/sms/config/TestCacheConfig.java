package com.sms.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestCacheConfig {
    
    @Bean
    @Primary
    public CacheManager testCacheManager() {
        // Create a simple in-memory cache manager with all required caches
        return new ConcurrentMapCacheManager(
            "hierarchyCache",
            "analytics-dashboard", 
            "analytics-student-summary",
            "analytics-live-snapshot",
            "analytics-snapshots",
            "studentProfile"
        );
    }
}