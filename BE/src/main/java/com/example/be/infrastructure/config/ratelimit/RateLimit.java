package com.example.be.infrastructure.config.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for Rate Limiting.
 * Default is 10 requests per 60 seconds.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 10;      // Maximum number of requests
    int windowSeconds() default 60; // Time window in seconds
}
