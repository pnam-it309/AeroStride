package com.example.be.infrastructure.config.ratelimit;

import com.example.be.infrastructure.exceptions.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final StringRedisTemplate redisTemplate;

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(RateLimit rateLimit) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        
        // Handle IP behind proxy/load balancer
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            // In case of multiple proxies, take the first IP
            ip = ip.split(",")[0].trim();
        }
        
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        // Key format: rate_limit:IP:METHOD:URI
        String key = String.format("rate_limit:%s:%s:%s", ip, method, uri);

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount != null && currentCount == 1) {
                // Set TTL on the first request of the window
                redisTemplate.expire(key, rateLimit.windowSeconds(), TimeUnit.SECONDS);
            }

            if (currentCount != null && currentCount > rateLimit.limit()) {
                log.warn("Rate limit exceeded! IP: {}, Method: {}, URI: {}, Limit: {}/{}s", 
                        ip, method, uri, rateLimit.limit(), rateLimit.windowSeconds());
                throw new RateLimitException("Bạn đang thực hiện yêu cầu quá nhanh. Vui lòng thử lại sau vài phút.");
            }
        } catch (RateLimitException be) {
            throw be;
        } catch (Exception e) {
            // Fail-safe: if Redis is down, we let the request pass but log the error
            log.error("Rate limiting check failed due to Redis error: {}. Allowing request as fallback.", e.getMessage());
        }
    }
}
