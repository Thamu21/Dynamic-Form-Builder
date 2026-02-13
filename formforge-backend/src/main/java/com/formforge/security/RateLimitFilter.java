package com.formforge.security;

import com.formforge.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Bucket4j.
 * 
 * IN-MEMORY IMPLEMENTATION (MVP):
 * - Uses ConcurrentHashMap for per-IP bucket storage
 * - Pros: Zero dependencies, sub-millisecond checks, simple
 * - Cons: Lost on restart, not shared across instances
 * 
 * PRODUCTION UPGRADE PATH:
 * - Replace ConcurrentHashMap with Redis-backed storage
 * - Use Bucket4j-Redis for distributed limiting
 * - Simply change bucket resolution strategy
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate-limit.public-form.requests-per-hour:20}")
    private int requestsPerHour;

    /**
     * In-memory bucket storage per IP address.
     * 
     * TRADEOFFS:
     * - Memory: ~1KB per unique IP (acceptable for MVP)
     * - Thread-safe via ConcurrentHashMap
     * - Lost on JVM restart (attackers can retry)
     * 
     * TODO for production: Replace with Redis
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Only rate limit public form submissions
        String path = request.getRequestURI();
        if (path.startsWith("/api/public/forms/") && path.endsWith("/submit")
                && "POST".equalsIgnoreCase(request.getMethod())) {

            String clientIp = getClientIp(request);
            Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                throw new RateLimitExceededException(
                        "Too many submissions. Please try again later.");
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createBucket(String ip) {
        // 20 requests per hour, refilled every hour
        Bandwidth limit = Bandwidth.classic(
                requestsPerHour,
                Refill.intervally(requestsPerHour, Duration.ofHours(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        // Check for proxy headers
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
