package com.sms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Best-effort IP → geo resolver.
 *
 * Notes:
 * - This is NOT used to "prove" location; it is only an anti-VPN signal
 *   when it strongly disagrees with device GPS.
 * - External providers can be rate-limited or blocked; we fail open (returns null).
 */
@Service
public class IpGeolocationService {

    private static final Logger log = LoggerFactory.getLogger(IpGeolocationService.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();

    public IpGeolocationService(
            @Value("${app.attendance.ipgeo.enabled:false}") boolean enabled,
            @Value("${app.attendance.ipgeo.base-url:https://ipapi.co}") String baseUrl,
            RestClient.Builder restClientBuilder) {
        this.enabled = enabled;
        // Use default request factory for compatibility across Spring versions.
        // If you want hard timeouts, wire a ClientHttpRequestFactory bean and inject it.
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns [lat,lng] or null when unavailable.
     *
     * Uses `ipapi.co/<ip>/json/` response keys: latitude, longitude.
     */
    public double[] resolve(String ip) {
        if (!enabled) {
            return null;
        }
        if (ip == null || ip.isBlank() || ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168")) {
            return null;
        }
        return cache.computeIfAbsent(ip, key -> {
            try {
                Map payload = restClient.get()
                        .uri("/{ip}/json/", key)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(Map.class);

                if (payload == null) {
                    return null;
                }
                Object lat = payload.get("latitude");
                Object lon = payload.get("longitude");
                if (!(lat instanceof Number) || !(lon instanceof Number)) {
                    return null;
                }
                return new double[]{((Number) lat).doubleValue(), ((Number) lon).doubleValue()};
            } catch (Exception ex) {
                log.debug("IP geo lookup failed for {}: {}", key, ex.getMessage());
                return null;
            }
        });
    }
}

