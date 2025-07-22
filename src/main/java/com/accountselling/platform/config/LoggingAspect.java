package com.accountselling.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging method execution details
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.accountselling.platform.controller..*(..)) || " +
            "execution(* com.accountselling.platform.service..*(..))")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        
        long startTime = System.currentTimeMillis();
        
        try {
            MDC.put("method", methodName);
            log.debug("Executing method: {}", methodName);
            
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            MDC.put("executionTime", String.valueOf(executionTime));
            log.debug("Method {} executed in {} ms", methodName, executionTime);
            
            return result;
        } catch (Throwable ex) {
            MDC.put("errorType", ex.getClass().getName());
            MDC.put("errorMessage", ex.getMessage());
            log.error("Error executing method {}: {}", methodName, ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove("method");
            MDC.remove("executionTime");
            MDC.remove("errorType");
            MDC.remove("errorMessage");
        }
    }
}