package com.example.activityagent.controller;

import com.example.activityagent.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping("/")
    public Result<Map<String, Object>> index() {
        return Result.success(Map.of(
            "service", "activity-agent-backend",
            "status", "ok"
        ));
    }

    /**
     * Liveness probe: is the JVM alive?
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.success(Map.of("status", "ok"));
    }

    /**
     * Readiness probe: can we reach MySQL and Redis?
     */
    @GetMapping("/health/ready")
    public Result<Map<String, Object>> readiness() {
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean allOk = true;

        // MySQL — don't leak internal details in production
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            checks.put("mysql", valid ? "ok" : "unreachable");
            if (!valid) allOk = false;
        } catch (Exception e) {
            log.warn("MySQL health check failed", e);
            checks.put("mysql", "unreachable");
            allOk = false;
        }

        // Redis — don't leak internal details in production
        try {
            String pong = redisConnectionFactory.getConnection().ping();
            checks.put("redis", "PONG".equals(pong) ? "ok" : "unreachable");
            if (!"PONG".equals(pong)) allOk = false;
        } catch (Exception e) {
            log.warn("Redis health check failed", e);
            checks.put("redis", "unreachable");
            allOk = false;
        }

        checks.put("status", allOk ? "ready" : "degraded");
        return Result.success(checks);
    }
}
