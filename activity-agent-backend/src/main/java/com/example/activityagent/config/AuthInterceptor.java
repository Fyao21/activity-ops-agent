package com.example.activityagent.config;

import com.example.activityagent.common.RequestContext;
import com.example.activityagent.common.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Map;

/**
 * Unified authentication interceptor.
 * - Extracts Bearer token from Authorization header.
 * - Validates token against Redis ("login:token:<token>").
 * - Writes user identity into RequestContext.
 * - Enforces @RequireRole if present on the handler.
 *
 * Unauthenticated requests receive HTTP 401.
 * Unauthorized requests receive HTTP 403.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // Allow non-handler requests (e.g., static resources)
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = extractToken(request);
        if (token == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "{\"code\":401,\"message\":\"未提供有效的认证Token\"}");
            return false;
        }

        // AuthServiceImpl stores token via opsForValue().set() — read back
        // the same way. After Jackson deserialization the value is a Map.
        Object cached = redisTemplate.opsForValue().get("login:token:" + token);
        if (!(cached instanceof Map)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "{\"code\":401,\"message\":\"Token已过期或无效\"}");
            return false;
        }

        Map<String, Object> cacheValue = (Map<String, Object>) cached;
        Long userId = toLong(cacheValue.get("userId"));
        String username = String.valueOf(cacheValue.getOrDefault("username", ""));
        String role = String.valueOf(cacheValue.getOrDefault("role", ""));

        if (userId == null || username.isEmpty()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "{\"code\":401,\"message\":\"Token已过期或无效\"}");
            return false;
        }

        RequestContext.set(userId, username, role);

        // Role check
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null && requireRole.value().length > 0) {
            boolean allowed = Arrays.asList(requireRole.value()).contains(role);
            if (!allowed) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN,
                    "{\"code\":403,\"message\":\"权限不足\"}");
                RequestContext.clear();
                return false;
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        RequestContext.clear();
    }

    private void sendError(HttpServletResponse response, int status, String body) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
