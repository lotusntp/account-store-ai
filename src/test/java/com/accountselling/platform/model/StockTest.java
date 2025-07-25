package com.accountselling.platform.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Stock entity. Tests stock creation, reservation management, and business logic.
 */
@DisplayName("Stock Entity Tests")
class StockTest {

  private Stock stock;
  private Product product;
  private Category category;

  @BeforeEach
  void setUp() {
    category = new Category("Games");
    product = new Product("Test Game", new BigDecimal("99.99"), category);
    stock = new Stock();
  }

  @Test
  @DisplayName("Should create stock with default constructor")
  void shouldCreateStockWithDefaultConstructor() {
    Stock newStock = new Stock();

    assertNotNull(newStock);
    assertNull(newStock.getProduct());
    assertNull(newStock.getCredentials());
    assertFalse(newStock.getSold());
    assertNull(newStock.getReservedUntil());
    assertNull(newStock.getAdditionalInfo());
    assertNull(newStock.getSoldAt());
  }

  @Test
  @DisplayName("Should create stock with product and credentials constructor")
  void shouldCreateStockWithProductAndCredentialsConstructor() {
    Stock newStock = new Stock(product, "username:password");

    assertEquals(product, newStock.getProduct());
    assertEquals("username:password", newStock.getCredentials());
    assertFalse(newStock.getSold());
  }

  @Test
  @DisplayName("Should create stock with product, credentials, and additional info constructor")
  void shouldCreateStockWithAllFieldsConstructor() {
    Stock newStock = new Stock(product, "username:password", "Level 50 character");

    assertEquals(product, newStock.getProduct());
    assertEquals("username:password", newStock.getCredentials());
    assertEquals("Level 50 character", newStock.getAdditionalInfo());
    assertFalse(newStock.getSold());
  }

  @Test
  @DisplayName("Should set and get stock properties")
  void shouldSetAndGetStockProperties() {
    UUID id = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime reservedUntil = now.plusMinutes(30);

    stock.setId(id);
    stock.setProduct(product);
    stock.setCredentials("test:credentials");
    stock.setSold(true);
    stock.setReservedUntil(reservedUntil);
    stock.setAdditionalInfo("Additional information");
    stock.setSoldAt(now);

    assertEquals(id, stock.getId());
    assertEquals(product, stock.getProduct());
    assertEquals("test:credentials", stock.getCredentials());
    assertTrue(stock.getSold());
    assertEquals(reservedUntil, stock.getReservedUntil());
    assertEquals("Additional information", stock.getAdditionalInfo());
    assertEquals(now, stock.getSoldAt());
  }

  @Test
  @DisplayName("Should check if stock is reserved")
  void shouldCheckIfStockIsReserved() {
    assertFalse(stock.isReserved()); // No reservation

    stock.setReservedUntil(LocalDateTime.now().minusMinutes(10)); // Past reservation
    assertFalse(stock.isReserved());

    stock.setReservedUntil(LocalDateTime.now().plusMinutes(30)); // Future reservation
    assertTrue(stock.isReserved());
  }

  @Test
  @DisplayName("Should check if stock is available")
  void shouldCheckIfStockIsAvailable() {
    assertTrue(stock.isAvailable()); // Not sold, not reserved

    stock.setSold(true);
    assertFalse(stock.isAvailable()); // Sold

    stock.setSold(false);
    stock.setReservedUntil(LocalDateTime.now().plusMinutes(30));
    assertFalse(stock.isAvailable()); // Reserved
  }

  @Test
  @DisplayName("Should check if reservation is expired")
  void shouldCheckIfReservationIsExpired() {
    assertFalse(stock.isExpiredReservation()); // No reservation

    stock.setReservedUntil(LocalDateTime.now().plusMinutes(30)); // Future reservation
    assertFalse(stock.isExpiredReservation());

    stock.setReservedUntil(LocalDateTime.now().minusMinutes(10)); // Past reservation
    assertTrue(stock.isExpiredReservation());
  }

  @Test
  @DisplayName("Should reserve stock for specific duration")
  void shouldReserveStockForSpecificDuration() {
    stock.reserve(30); // Reserve for 30 minutes

    assertTrue(stock.isReserved());
    assertNotNull(stock.getReservedUntil());
    assertTrue(stock.getReservedUntil().isAfter(LocalDateTime.now().plusMinutes(29)));
    assertTrue(stock.getReservedUntil().isBefore(LocalDateTime.now().plusMinutes(31)));
  }

  @Test
  @DisplayName("Should not reserve sold stock")
  void shouldNotReserveSoldStock() {
    stock.setSold(true);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              stock.reserve(30);
            });

    assertEquals("Cannot reserve sold stock item", exception.getMessage());
  }

  @Test
  @DisplayName("Should reserve stock until specific time")
  void shouldReserveStockUntilSpecificTime() {
    LocalDateTime until = LocalDateTime.now().plusHours(2);
    stock.reserveUntil(until);

    assertTrue(stock.isReserved());
    assertEquals(until, stock.getReservedUntil());
  }

  @Test
  @DisplayName("Should not reserve sold stock until specific time")
  void shouldNotReserveSoldStockUntilSpecificTime() {
    stock.setSold(true);
    LocalDateTime until = LocalDateTime.now().plusHours(2);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              stock.reserveUntil(until);
            });

    assertEquals("Cannot reserve sold stock item", exception.getMessage());
  }

  @Test
  @DisplayName("Should release reservation")
  void shouldReleaseReservation() {
    stock.setReservedUntil(LocalDateTime.now().plusMinutes(30));
    assertTrue(stock.isReserved());

    stock.releaseReservation();

    assertNull(stock.getReservedUntil());
    assertFalse(stock.isReserved());
  }

  @Test
  @DisplayName("Should mark stock as sold")
  void shouldMarkStockAsSold() {
    stock.setReservedUntil(LocalDateTime.now().plusMinutes(30)); // Set reservation first

    stock.markAsSold();

    assertTrue(stock.getSold());
    assertNotNull(stock.getSoldAt());
    assertNull(stock.getReservedUntil()); // Reservation should be cleared
    assertTrue(stock.getSoldAt().isBefore(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  @DisplayName("Should not mark already sold stock as sold")
  void shouldNotMarkAlreadySoldStockAsSold() {
    stock.setSold(true);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              stock.markAsSold();
            });

    assertEquals("Stock item is already sold", exception.getMessage());
  }

  @Test
  @DisplayName("Should get remaining reservation time")
  void shouldGetRemainingReservationTime() {
    assertEquals(0, stock.getRemainingReservationMinutes()); // No reservation

    stock.setReservedUntil(LocalDateTime.now().plusMinutes(45));
    long remaining = stock.getRemainingReservationMinutes();

    assertTrue(remaining >= 44 && remaining <= 45); // Allow for small time differences
  }

  @Test
  @DisplayName("Should check if reservation is expiring soon")
  void shouldCheckIfReservationIsExpiringSoon() {
    assertFalse(stock.isReservationExpiringSoon(10)); // No reservation

    stock.setReservedUntil(LocalDateTime.now().plusMinutes(30));
    assertFalse(stock.isReservationExpiringSoon(10)); // Not expiring soon

    stock.setReservedUntil(LocalDateTime.now().plusMinutes(5));
    assertTrue(stock.isReservationExpiringSoon(10)); // Expiring soon
  }

  @Test
  @DisplayName("Should get status display correctly")
  void shouldGetStatusDisplayCorrectly() {
    assertEquals("AVAILABLE", stock.getStatusDisplay());

    stock.setReservedUntil(LocalDateTime.now().plusMinutes(30));
    assertEquals("RESERVED", stock.getStatusDisplay());

    stock.setSold(true);
    assertEquals("SOLD", stock.getStatusDisplay());
  }

  @Test
  @DisplayName("Should cleanup expired reservation")
  void shouldCleanupExpiredReservation() {
    stock.setReservedUntil(LocalDateTime.now().minusMinutes(10)); // Expired reservation
    assertTrue(stock.isExpiredReservation());

    stock.cleanupExpiredReservation();

    assertNull(stock.getReservedUntil());
    assertFalse(stock.isExpiredReservation());
  }

  @Test
  @DisplayName("Should not cleanup active reservation")
  void shouldNotCleanupActiveReservation() {
    LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(30);
    stock.setReservedUntil(reservedUntil);

    stock.cleanupExpiredReservation();

    assertEquals(reservedUntil, stock.getReservedUntil()); // Should remain unchanged
  }

  @Test
  @DisplayName("Should get product name")
  void shouldGetProductName() {
    assertNull(stock.getProductName()); // No product

    stock.setProduct(product);
    assertEquals("Test Game", stock.getProductName());
  }

  @Test
  @DisplayName("Should get product category name")
  void shouldGetProductCategoryName() {
    assertNull(stock.getProductCategoryName()); // No product

    stock.setProduct(product);
    assertEquals("Games", stock.getProductCategoryName());

    product.setCategory(null);
    assertNull(stock.getProductCategoryName()); // Product has no category
  }

  @Test
  @DisplayName("Should implement toString correctly")
  void shouldImplementToStringCorrectly() {
    stock.setCredentials("test:credentials");
    stock.setSold(false);

    String toString = stock.toString();

    assertNotNull(toString);
    assertTrue(toString.contains("Stock("));
    assertTrue(toString.contains("sold=false"));
    // Should not contain credentials for security
    assertFalse(toString.contains("test:credentials"));
  }

  @Test
  @DisplayName("Should handle null values gracefully")
  void shouldHandleNullValuesGracefully() {
    Stock newStock = new Stock();

    assertNull(newStock.getProduct());
    assertNull(newStock.getCredentials());
    assertNull(newStock.getReservedUntil());

    // Should not throw exceptions
    assertFalse(newStock.isReserved());
    assertTrue(newStock.isAvailable());
    assertFalse(newStock.isExpiredReservation());
    assertEquals(0, newStock.getRemainingReservationMinutes());
    assertFalse(newStock.isReservationExpiringSoon(10));
    assertEquals("AVAILABLE", newStock.getStatusDisplay());
    assertNull(newStock.getProductName());
    assertNull(newStock.getProductCategoryName());
  }

  @Test
  @DisplayName("Should handle edge cases in time calculations")
  void shouldHandleEdgeCasesInTimeCalculations() {
    // Test reservation exactly at current time
    stock.setReservedUntil(LocalDateTime.now());
    // This might be true or false depending on exact timing, but shouldn't throw exception
    assertDoesNotThrow(() -> stock.isReserved());

    // Test very short reservation
    stock.setReservedUntil(LocalDateTime.now().plusSeconds(1));
    assertTrue(stock.isReserved());

    // Test very long reservation
    stock.setReservedUntil(LocalDateTime.now().plusYears(1));
    assertTrue(stock.isReserved());
    assertFalse(stock.isReservationExpiringSoon(60));
  }
}
