package com.example.activityagent.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare required role(s) for a controller method.
 * Checked by AuthInterceptor after token validation.
 * If no @RequireRole annotation is present, any authenticated user may access.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * Allowed role(s). Values: ADMIN, OPERATOR
     */
    String[] value() default {};
}
