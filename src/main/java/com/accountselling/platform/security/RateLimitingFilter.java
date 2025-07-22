package com.accountselling.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucketBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting for certain paths
        String path = request.getRequestURI();
        if (path.contains("/api-docs") || path.contains("/swagger-ui") || path.contains("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get client IP or use a token from header if available
        String clientId = getClientIdentifier(request);

        // Create or get the rate limiter for this client
        Bucket bucket = buckets.computeIfAbsent(clientId, this::createNewBucket);

        // Try to consume a token
        if (bucket.tryConsume(1)) {
            // Allow the request
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for client: {}", clientId);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            errorDetails.put("error", "Too Many Requests");
            errorDetails.put("message", "Rate limit exceeded. Please try again later.");
            errorDetails.put("path", request.getRequestURI());

            objectMapper.writeValue(response.getWriter(), errorDetails);
        }
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // In a real application, you might want to use a more sophisticated approach
        // such as using the authenticated user's ID or a combination of IP and user agent
        return request.getRemoteAddr();
    }

    private Bucket createNewBucket(String clientId) {
        // Allow 30 requests per minute
        Bandwidth limit = Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1)));

        // ใช้ LocalBucketBuilder แทน Bucket4j.builder() ที่ถูก deprecated
        LocalBucketBuilder builder = new LocalBucketBuilder();
        builder.addLimit(limit);
        return builder.build();
    }
}
