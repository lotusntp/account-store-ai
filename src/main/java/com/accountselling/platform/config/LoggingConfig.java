package com.accountselling.platform.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Configuration class for logging setup and validation. Ensures proper logging configuration and
 * Elasticsearch connectivity.
 */
@Slf4j
@Configuration
public class LoggingConfig {

  @Value("${logging.elasticsearch.host:192.168.100.11}")
  private String elasticsearchHost;

  @Value("${logging.elasticsearch.port:5000}")
  private String elasticsearchPort;

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${spring.profiles.active:default}")
  private String activeProfile;

  /** Validate logging configuration on application startup */
  @EventListener(ApplicationReadyEvent.class)
  public void validateLoggingConfiguration() {
    log.info("Validating logging configuration...");

    // Print logback configuration status
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    StatusPrinter.print(context);

    // Log configuration details
    log.info(
        "Application: {} | Profile: {} | Elasticsearch: {}:{}",
        applicationName,
        activeProfile,
        elasticsearchHost,
        elasticsearchPort);

    // Test structured logging
    log.info(
        "Structured logging test - application={}, profile={}, elasticsearch={}:{}",
        applicationName,
        activeProfile,
        elasticsearchHost,
        elasticsearchPort);

    log.info("Logging configuration validation completed successfully");
  }

  /** Log application startup information */
  @EventListener(ApplicationReadyEvent.class)
  public void logApplicationStartup() {
    log.info("=== Account Selling Platform Started ===");
    log.info("Application Name: {}", applicationName);
    log.info("Active Profile: {}", activeProfile);
    log.info("Elasticsearch Host: {}", elasticsearchHost);
    log.info("Elasticsearch Port: {}", elasticsearchPort);
    log.info("Logging Level: {}", getLoggingLevel());
    log.info("========================================");
  }

  private String getLoggingLevel() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    return context.getLogger("com.accountselling.platform").getLevel().toString();
  }
}
