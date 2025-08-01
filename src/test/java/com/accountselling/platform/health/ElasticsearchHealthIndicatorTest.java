package com.accountselling.platform.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for ElasticsearchHealthIndicator. Tests health check functionality for Elasticsearch
 * connectivity.
 */
class ElasticsearchHealthIndicatorTest {

  private ElasticsearchHealthIndicator healthIndicator;

  @BeforeEach
  void setUp() {
    healthIndicator = new ElasticsearchHealthIndicator();
  }

  @Test
  void testCheckHealth_WithValidConfiguration() {
    // Given
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchHost", "localhost");
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchPort", 9200);

    // When
    Map<String, Object> health = healthIndicator.checkHealth();

    // Then
    // Note: This test will likely show DOWN status unless Elasticsearch is actually running
    // In a real test environment, you might want to use TestContainers or mock the socket
    // connection
    assertThat(health).isNotNull();
    assertThat(health).containsKey("elasticsearch.host");
    assertThat(health).containsKey("elasticsearch.port");
    assertThat(health).containsKey("status");
    assertThat(health.get("elasticsearch.host")).isEqualTo("localhost");
    assertThat(health.get("elasticsearch.port")).isEqualTo(9200);
  }

  @Test
  void testCheckHealth_WithInvalidHost() {
    // Given
    ReflectionTestUtils.setField(
        healthIndicator, "elasticsearchHost", "invalid-host-that-does-not-exist");
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchPort", 9200);

    // When
    Map<String, Object> health = healthIndicator.checkHealth();

    // Then
    assertThat(health.get("status")).isEqualTo("DOWN");
    assertThat(health).containsKey("elasticsearch.host");
    assertThat(health).containsKey("elasticsearch.port");
    assertThat(health).containsKey("connection");
    assertThat(health).containsKey("error");
    assertThat(health.get("elasticsearch.host")).isEqualTo("invalid-host-that-does-not-exist");
    assertThat(health.get("elasticsearch.port")).isEqualTo(9200);
    assertThat(health.get("connection")).isEqualTo("Connection failed");
  }

  @Test
  void testCheckHealth_WithInvalidPort() {
    // Given
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchHost", "localhost");
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchPort", 99999);

    // When
    Map<String, Object> health = healthIndicator.checkHealth();

    // Then
    assertThat(health.get("status")).isEqualTo("DOWN");
    assertThat(health).containsKey("elasticsearch.host");
    assertThat(health).containsKey("elasticsearch.port");
    assertThat(health).containsKey("connection");
    assertThat(health).containsKey("error");
    assertThat(health.get("elasticsearch.host")).isEqualTo("localhost");
    assertThat(health.get("elasticsearch.port")).isEqualTo(99999);
    assertThat(health.get("connection")).isEqualTo("Error");
  }

  @Test
  void testCheckHealth_DefaultConfiguration() {
    // Given - using default values from @Value annotations
    // elasticsearchHost defaults to "192.168.100.11"
    // elasticsearchPort defaults to 5000

    // When
    Map<String, Object> health = healthIndicator.checkHealth();

    // Then
    assertThat(health).isNotNull();
    assertThat(health).containsKey("elasticsearch.host");
    assertThat(health).containsKey("elasticsearch.port");
    assertThat(health).containsKey("status");

    // The actual status will depend on whether the default Elasticsearch instance is running
    // In most test environments, this will be DOWN
    if ("DOWN".equals(health.get("status"))) {
      assertThat(health).containsKey("error");
      // Could be either "Connection failed" or "Error" depending on the specific failure
      assertThat(health.get("connection")).isIn("Connection failed", "Error");
    } else {
      assertThat(health.get("connection")).isEqualTo("Connected");
    }
  }

  @Test
  void testCheckHealth_DetailsContainCorrectKeys() {
    // Given
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchHost", "test-host");
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchPort", 1234);

    // When
    Map<String, Object> health = healthIndicator.checkHealth();

    // Then
    assertThat(health)
        .containsKeys("elasticsearch.host", "elasticsearch.port", "status", "connection");

    // If connection fails, should also contain error details
    if ("DOWN".equals(health.get("status"))) {
      assertThat(health).containsKey("error");
    }
  }

  @Test
  void testIsHealthy_WithInvalidHost() {
    // Given
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchHost", "invalid-host");
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchPort", 9200);

    // When
    boolean isHealthy = healthIndicator.isHealthy();

    // Then
    assertThat(isHealthy).isFalse();
  }

  @Test
  void testGetElasticsearchHost() {
    // Given
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchHost", "test-host");

    // When
    String host = healthIndicator.getElasticsearchHost();

    // Then
    assertThat(host).isEqualTo("test-host");
  }

  @Test
  void testGetElasticsearchPort() {
    // Given
    ReflectionTestUtils.setField(healthIndicator, "elasticsearchPort", 1234);

    // When
    int port = healthIndicator.getElasticsearchPort();

    // Then
    assertThat(port).isEqualTo(1234);
  }
}
