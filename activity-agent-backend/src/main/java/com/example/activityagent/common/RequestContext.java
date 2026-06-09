package com.example.activityagent.common;

import lombok.Data;

/**
 * Thread-local context holding the authenticated user for the current request.
 * Populated by AuthInterceptor after token validation.
 */
public final class RequestContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(Long userId, String username, String role) {
        Context ctx = new Context();
        ctx.userId = userId;
        ctx.username = username;
        ctx.role = role;
        HOLDER.set(ctx);
    }

    public static Long getUserId() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.userId : null;
    }

    public static String getUsername() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.username : null;
    }

    public static String getRole() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.role : null;
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Data
    private static class Context {
        private Long userId;
        private String username;
        private String role;
    }
}
