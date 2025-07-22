package com.accountselling.platform.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor to add request context information to MDC for logging
 */
@Slf4j
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Generate a unique request ID
        String requestId = UUID.randomUUID().toString();
        
        // Add HTTP request information to MDC
        MDC.put("http.request.method", request.getMethod());
        MDC.put("http.request.path", request.getRequestURI());
        MDC.put("http.request.remote_ip", getClientIp(request));
        MDC.put("request.id", requestId);
        
        // Log that interceptor is being applied
        log.debug("RequestContextInterceptor applied for URI: {}, Method: {}, RequestId: {}", 
            request.getRequestURI(), request.getMethod(), requestId);
        
        // Add session information if available
        if (request.getSession(false) != null) {
            MDC.put("session.id", request.getSession().getId());
        }
        
        // Add user information if authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            MDC.put("user.id", auth.getName());
            MDC.put("user.name", auth.getName());
        }
        
        // Add request ID to response header for correlation
        response.addHeader("X-Request-ID", requestId);
        
        // Store start time in request attributes for calculating execution time
        request.setAttribute("startTime", System.currentTimeMillis());
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Add response information to MDC
        MDC.put("http.response.status_code", String.valueOf(response.getStatus()));
        
        // Calculate and add execution time
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long executionTime = System.currentTimeMillis() - startTime;
            MDC.put("execution.time_ms", String.valueOf(executionTime));
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Clean up MDC to prevent memory leaks
        MDC.remove("http.request.method");
        MDC.remove("http.request.path");
        MDC.remove("http.request.remote_ip");
        MDC.remove("http.response.status_code");
        MDC.remove("request.id");
        MDC.remove("session.id");
        MDC.remove("user.id");
        MDC.remove("user.name");
        MDC.remove("execution.time_ms");
        
        // Add error information if exception occurred
        if (ex != null) {
            MDC.put("error.type", ex.getClass().getName());
            MDC.put("error.message", ex.getMessage());
            log.error("Request processing failed", ex);
            MDC.remove("error.type");
            MDC.remove("error.message");
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}