package com.accountselling.platform.health;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service for checking Elasticsearch connectivity. Provides health check functionality for log
 * shipping infrastructure.
 */
@Slf4j
@Component
public class ElasticsearchHealthIndicator {

  @Value("${logging.elasticsearch.host:192.168.100.11}")
  private String elasticsearchHost;

  @Value("${logging.elasticsearch.port:5000}")
  private int elasticsearchPort;

  /** Check Elasticsearch health and return status information */
  public Map<String, Object> checkHealth() {
    Map<String, Object> health = new HashMap<>();

    try {
      if (isElasticsearchReachable()) {
        health.put("status", "UP");
        health.put("elasticsearch.host", elasticsearchHost);
        health.put("elasticsearch.port", elasticsearchPort);
        health.put("connection", "Connected");
        log.debug("Elasticsearch health check passed: {}:{}", elasticsearchHost, elasticsearchPort);
      } else {
        health.put("status", "DOWN");
        health.put("elasticsearch.host", elasticsearchHost);
        health.put("elasticsearch.port", elasticsearchPort);
        health.put("connection", "Connection failed");
        health.put("error", "Unable to connect to Elasticsearch");
        log.warn("Elasticsearch health check failed: {}:{}", elasticsearchHost, elasticsearchPort);
      }
    } catch (Exception e) {
      log.error("Error checking Elasticsearch health", e);
      health.put("status", "DOWN");
      health.put("elasticsearch.host", elasticsearchHost);
      health.put("elasticsearch.port", elasticsearchPort);
      health.put("connection", "Error");
      health.put("error", e.getMessage());
    }

    return health;
  }

  /** Simple boolean check for Elasticsearch connectivity */
  public boolean isHealthy() {
    try {
      return isElasticsearchReachable();
    } catch (Exception e) {
      log.error("Error checking Elasticsearch connectivity", e);
      return false;
    }
  }

  private boolean isElasticsearchReachable() {
    try (Socket socket = new Socket()) {
      socket.connect(new java.net.InetSocketAddress(elasticsearchHost, elasticsearchPort), 5000);
      return true;
    } catch (SocketTimeoutException e) {
      log.debug("Elasticsearch connection timeout: {}:{}", elasticsearchHost, elasticsearchPort);
      return false;
    } catch (IOException e) {
      log.debug(
          "Elasticsearch connection failed: {}:{} - {}",
          elasticsearchHost,
          elasticsearchPort,
          e.getMessage());
      return false;
    }
  }

  public String getElasticsearchHost() {
    return elasticsearchHost;
  }

  public int getElasticsearchPort() {
    return elasticsearchPort;
  }
}
