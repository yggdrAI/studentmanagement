package com.sms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableCaching
public class RedisAnalyticsConfig {

        private static final String[] CACHE_NAMES = new String[] {
                        "hierarchyCache",
                        "analytics-dashboard",
                        "analytics-student-summary",
                        "analytics-live-snapshot",
                        "analytics-snapshots",
                        "studentProfile"
        };

        @Bean
        @ConditionalOnProperty(prefix = "app.analytics.redis", name = "enabled", havingValue = "true")
        public RedisConnectionFactory redisConnectionFactory(org.springframework.core.env.Environment environment) {
                String host = environment.getProperty("spring.data.redis.host", "localhost");
                Integer port = environment.getProperty("spring.data.redis.port", Integer.class, 6379);
                return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
        }

        @Bean
        @ConditionalOnProperty(prefix = "app.analytics.redis", name = "enabled", havingValue = "true")
        public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
                RedisTemplate<String, String> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);
                template.setKeySerializer(new StringRedisSerializer());
                template.setValueSerializer(new StringRedisSerializer());
                template.setHashKeySerializer(new StringRedisSerializer());
                template.setHashValueSerializer(new StringRedisSerializer());
                return template;
        }

        @Bean
        @ConditionalOnProperty(prefix = "app.analytics.redis", name = "enabled", havingValue = "true")
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                Map<String, RedisCacheConfiguration> cacheConfigurations = new LinkedHashMap<>();
                cacheConfigurations.put("hierarchyCache", RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Objects.requireNonNull(Duration.ofMinutes(10))));
                cacheConfigurations.put("analytics-dashboard", RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Objects.requireNonNull(Duration.ofMinutes(10))));
                cacheConfigurations.put("analytics-student-summary", RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Objects.requireNonNull(Duration.ofMinutes(10))));
                cacheConfigurations.put("analytics-live-snapshot", RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Objects.requireNonNull(Duration.ofSeconds(15))));
                cacheConfigurations.put("analytics-snapshots", RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Objects.requireNonNull(Duration.ofDays(1))));
                cacheConfigurations.put("studentProfile", RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Objects.requireNonNull(Duration.ofMinutes(20))));

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

                return RedisCacheManager.builder(Objects.requireNonNull(connectionFactory))
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .transactionAware()
                                .build();
        }

        @Bean
        @ConditionalOnProperty(prefix = "app.analytics.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
        public CacheManager localCacheManager() {
                return new ConcurrentMapCacheManager(CACHE_NAMES);
        }

        @Bean
        @ConditionalOnProperty(prefix = "app.analytics.redis", name = "enabled", havingValue = "true")
        public ChannelTopic analyticsFeedTopic() {
                return new ChannelTopic("analytics:feed");
        }

        @Bean
        @ConditionalOnProperty(prefix = "app.analytics.redis", name = "enabled", havingValue = "true")
        public ChannelTopic analyticsLiveTopic() {
                return new ChannelTopic("analytics:live");
        }

        @Bean
        @ConditionalOnProperty(prefix = "app.analytics.redis", name = "enabled", havingValue = "true")
        public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                        AnalyticsRedisBridge analyticsRedisBridge,
                        ChannelTopic analyticsFeedTopic,
                        ChannelTopic analyticsLiveTopic) {
                RedisMessageListenerContainer container = new RedisMessageListenerContainer();
                container.setConnectionFactory(Objects.requireNonNull(connectionFactory));
                container.addMessageListener(Objects.requireNonNull(analyticsRedisBridge),
                                Objects.requireNonNull(analyticsFeedTopic));
                container.addMessageListener(Objects.requireNonNull(analyticsRedisBridge),
                                Objects.requireNonNull(analyticsLiveTopic));
                return container;
        }

        @Bean
        @ConditionalOnProperty(prefix = "app.analytics.redis", name = "enabled", havingValue = "true")
        public AnalyticsRedisBridge analyticsRedisBridge(ObjectMapper objectMapper,
                        org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
                return new AnalyticsRedisBridge(objectMapper, messagingTemplate);
        }
}