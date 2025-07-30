package com.accountselling.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucketBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  @Value("${spring.profiles.active:prod}")
  private String activeProfile;

  // Rate limit configurations for different endpoint patterns
  // IMPORTANT: Order matters! More specific patterns should come first
  private static final Map<String, RateLimitConfig> RATE_LIMIT_CONFIGS = new LinkedHashMap<>();

  static {
    // Most specific patterns first
    RATE_LIMIT_CONFIGS.put("/api/auth/login", new RateLimitConfig(5, Duration.ofMinutes(5)));
    RATE_LIMIT_CONFIGS.put("/api/auth/register", new RateLimitConfig(3, Duration.ofMinutes(10)));

    // Moderately specific patterns
    RATE_LIMIT_CONFIGS.put("/api/auth/**", new RateLimitConfig(10, Duration.ofMinutes(1)));
    RATE_LIMIT_CONFIGS.put("/api/admin/**", new RateLimitConfig(100, Duration.ofMinutes(1)));
    RATE_LIMIT_CONFIGS.put("/api/users/**", new RateLimitConfig(50, Duration.ofMinutes(1)));
    RATE_LIMIT_CONFIGS.put("/api/products/**", new RateLimitConfig(200, Duration.ofMinutes(1)));
    RATE_LIMIT_CONFIGS.put("/api/categories/**", new RateLimitConfig(100, Duration.ofMinutes(1)));
    RATE_LIMIT_CONFIGS.put("/api/payments/**", new RateLimitConfig(20, Duration.ofMinutes(1)));

    // Most general pattern last (fallback)
    RATE_LIMIT_CONFIGS.put("/**", new RateLimitConfig(100, Duration.ofMinutes(1)));
  }

  // Paths that should be excluded from rate limiting
  private static final String[] EXCLUDED_PATHS = {
    "/api-docs/**",
    "/swagger-ui/**",
    "/actuator/**",
    "/api/observability/**",
    "/api/payments/webhook" // Webhook should not be rate limited
  };

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();

    // Skip rate limiting for excluded paths
    if (shouldSkipRateLimit(path)) {
      filterChain.doFilter(request, response);
      return;
    }

    // Skip rate limiting entirely for test profile
    if ("test".equals(activeProfile)) {
      filterChain.doFilter(request, response);
      return;
    }

    // Get client identifier and rate limit configuration
    String clientId = getClientIdentifier(request, path);
    RateLimitConfig config = getRateLimitConfig(path);

    // Create or get the rate limiter for this client
    Bucket bucket = buckets.computeIfAbsent(clientId, k -> createNewBucket(config));

    // Try to consume a token and get probe information
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    // Add rate limit headers
    addRateLimitHeaders(response, probe, config);

    if (probe.isConsumed()) {
      // Allow the request
      log.debug("Rate limit check passed for client: {} on path: {}", clientId, path);
      filterChain.doFilter(request, response);
    } else {
      // Rate limit exceeded
      handleRateLimitExceeded(request, response, clientId, probe);
    }
  }

  private boolean shouldSkipRateLimit(String path) {
    for (String excludedPath : EXCLUDED_PATHS) {
      if (pathMatcher.match(excludedPath, path)) {
        return true;
      }
    }
    return false;
  }

  private String getClientIdentifier(HttpServletRequest request, String path) {
    // Create a more sophisticated client identifier
    String baseId = request.getRemoteAddr();

    // For authenticated requests, try to get user info from JWT
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      // In a production system, you might decode the JWT to get the user ID
      // For now, we'll use a combination of IP and a hash of the token
      baseId = baseId + ":" + Math.abs(authHeader.hashCode() % 1000);
    }

    // Add path-specific identifier for more granular control
    return baseId + ":" + getPathPattern(path);
  }

  private String getPathPattern(String path) {
    // Find the most specific matching pattern
    for (String pattern : RATE_LIMIT_CONFIGS.keySet()) {
      if (pathMatcher.match(pattern, path)) {
        return pattern;
      }
    }
    return "/**"; // Fallback
  }

  private RateLimitConfig getRateLimitConfig(String path) {
    // Find the most specific matching configuration
    for (Map.Entry<String, RateLimitConfig> entry : RATE_LIMIT_CONFIGS.entrySet()) {
      if (pathMatcher.match(entry.getKey(), path)) {
        return entry.getValue();
      }
    }
    return RATE_LIMIT_CONFIGS.get("/**"); // Fallback to default
  }

  private Bucket createNewBucket(RateLimitConfig config) {
    Bandwidth limit;
    if ("test".equals(activeProfile)) {
      // Very permissive limits for testing
      limit = Bandwidth.classic(1000, Refill.greedy(1000, Duration.ofMinutes(1)));
    } else {
      // Use configuration-specific limits
      limit =
          Bandwidth.classic(
              config.getCapacity(), Refill.greedy(config.getCapacity(), config.getRefillPeriod()));
    }

    LocalBucketBuilder builder = new LocalBucketBuilder();
    builder.addLimit(limit);
    return builder.build();
  }

  private void addRateLimitHeaders(
      HttpServletResponse response, ConsumptionProbe probe, RateLimitConfig config) {

    // Add standard rate limit headers
    response.setHeader("X-RateLimit-Limit", String.valueOf(config.getCapacity()));
    response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

    if (!probe.isConsumed()) {
      // Add retry-after header when rate limited
      long nanosUntilRefill = probe.getNanosToWaitForRefill();
      long secondsUntilRefill = (nanosUntilRefill / 1_000_000_000L) + 1;
      response.setHeader(
          "X-RateLimit-Reset",
          String.valueOf(System.currentTimeMillis() / 1000 + secondsUntilRefill));
      response.setHeader("Retry-After", String.valueOf(secondsUntilRefill));
    }

    // Add custom headers for monitoring
    response.setHeader("X-RateLimit-Policy", config.toString());
  }

  private void handleRateLimitExceeded(
      HttpServletRequest request,
      HttpServletResponse response,
      String clientId,
      ConsumptionProbe probe)
      throws IOException {

    log.warn(
        "Rate limit exceeded for client: {} on path: {} (remaining: {})",
        clientId,
        request.getRequestURI(),
        probe.getRemainingTokens());

    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");

    Map<String, Object> errorDetails = new HashMap<>();
    errorDetails.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
    errorDetails.put("error", "Too Many Requests");
    errorDetails.put("message", "Rate limit exceeded. Please try again later.");
    errorDetails.put("path", request.getRequestURI());

    // Use Thailand timezone (UTC+7)
    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Bangkok"));
    errorDetails.put("timestamp", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    // Add rate limit specific information
    long nanosUntilRefill = probe.getNanosToWaitForRefill();
    long secondsUntilRefill = (nanosUntilRefill / 1_000_000_000L) + 1;
    errorDetails.put("retryAfterSeconds", secondsUntilRefill);

    objectMapper.writeValue(response.getWriter(), errorDetails);
  }

  /** Configuration class for rate limiting parameters */
  private static class RateLimitConfig {
    private final int capacity;
    private final Duration refillPeriod;

    public RateLimitConfig(int capacity, Duration refillPeriod) {
      this.capacity = capacity;
      this.refillPeriod = refillPeriod;
    }

    public int getCapacity() {
      return capacity;
    }

    public Duration getRefillPeriod() {
      return refillPeriod;
    }

    @Override
    public String toString() {
      return capacity + " requests per " + refillPeriod.toMinutes() + " minutes";
    }
  }
}
