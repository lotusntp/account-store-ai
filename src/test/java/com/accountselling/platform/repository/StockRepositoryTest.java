package com.accountselling.platform.repository;

import static org.assertj.core.api.Assertions.*;

import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

/**
 * Comprehensive unit tests for StockRepository. Tests all stock management operations including
 * availability checks, reservations, sales tracking, and cleanup operations.
 *
 * <p>Comprehensive StockRepository testing covering stock management availability checks,
 * reservations, sales tracking, and data cleanup
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Stock Repository Tests")
class StockRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private StockRepository stockRepository;

  private Category testCategory;
  private Product testProduct1;
  private Product testProduct2;
  private Stock availableStock1;
  private Stock availableStock2;
  private Stock soldStock;
  private Stock reservedStock;
  private Stock expiredReservationStock;

  @BeforeEach
  void setUp() {
    // Create test category
    testCategory = new Category("Test Category");
    entityManager.persistAndFlush(testCategory);

    // Create test products
    testProduct1 = new Product("Test Product 1", new BigDecimal("100.00"), testCategory);
    testProduct1.setLowStockThreshold(2);
    entityManager.persistAndFlush(testProduct1);

    testProduct2 = new Product("Test Product 2", new BigDecimal("200.00"), testCategory);
    testProduct2.setLowStockThreshold(1);
    entityManager.persistAndFlush(testProduct2);

    // Create available stock items
    availableStock1 = new Stock(testProduct1, "username1:password1", "Additional info 1");
    entityManager.persistAndFlush(availableStock1);

    availableStock2 = new Stock(testProduct1, "username2:password2", "Additional info 2");
    entityManager.persistAndFlush(availableStock2);

    // Create sold stock item
    soldStock = new Stock(testProduct1, "username3:password3");
    soldStock.markAsSold();
    entityManager.persistAndFlush(soldStock);

    // Create reserved stock item
    reservedStock = new Stock(testProduct2, "username4:password4");
    reservedStock.reserve(30); // Reserve for 30 minutes
    entityManager.persistAndFlush(reservedStock);

    // Create expired reservation stock item
    expiredReservationStock = new Stock(testProduct2, "username5:password5");
    expiredReservationStock.reserveUntil(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
    entityManager.persistAndFlush(expiredReservationStock);

    entityManager.clear(); // Clear persistence context to ensure fresh queries
  }

  // ==================== BASIC STOCK QUERIES TESTS ====================

  @Test
  @DisplayName("Should find stock items by product")
  void shouldFindStockItemsByProduct() {
    // When
    List<Stock> result = stockRepository.findByProduct(testProduct1);

    // Then
    assertThat(result).hasSize(3); // 2 available + 1 sold
    assertThat(result)
        .extracting(Stock::getCredentials)
        .containsExactlyInAnyOrder(
            "username1:password1", "username2:password2", "username3:password3");
  }

  @Test
  @DisplayName("Should find stock items by product ID")
  void shouldFindStockItemsByProductId() {
    // When
    List<Stock> result = stockRepository.findByProductId(testProduct1.getId());

    // Then
    assertThat(result).hasSize(3);
    assertThat(result)
        .allSatisfy(
            stock -> assertThat(stock.getProduct().getId()).isEqualTo(testProduct1.getId()));
  }

  @Test
  @DisplayName("Should find stock items by product with pagination")
  void shouldFindStockItemsByProductWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 2);

    // When
    Page<Stock> result = stockRepository.findByProduct(testProduct1, pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getTotalElements()).isEqualTo(3);
    assertThat(result.getTotalPages()).isEqualTo(2);
  }

  // ==================== AVAILABILITY QUERIES TESTS ====================

  @Test
  @DisplayName("Should find available stock items")
  void shouldFindAvailableStockItems() {
    // When
    List<Stock> result = stockRepository.findAvailableStock();

    // Then
    assertThat(result).hasSize(3); // 2 from product1 + 1 expired reservation from product2
    assertThat(result)
        .extracting(Stock::getCredentials)
        .containsExactlyInAnyOrder(
            "username1:password1", "username2:password2", "username5:password5");
  }

  @Test
  @DisplayName("Should find available stock items by product")
  void shouldFindAvailableStockItemsByProduct() {
    // When
    List<Stock> result = stockRepository.findAvailableStockByProduct(testProduct1);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(Stock::getCredentials)
        .containsExactlyInAnyOrder("username1:password1", "username2:password2");
    assertThat(result)
        .allSatisfy(
            stock -> {
              assertThat(stock.getSold()).isFalse();
              assertThat(stock.isAvailable()).isTrue();
            });
  }

  @Test
  @DisplayName("Should find available stock items by product ID")
  void shouldFindAvailableStockItemsByProductId() {
    // When
    List<Stock> result = stockRepository.findAvailableStockByProductId(testProduct1.getId());

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .allSatisfy(
            stock -> {
              assertThat(stock.getProduct().getId()).isEqualTo(testProduct1.getId());
              assertThat(stock.getSold()).isFalse();
              assertThat(stock.isAvailable()).isTrue();
            });
  }

  @Test
  @DisplayName("Should find first available stock item by product")
  void shouldFindFirstAvailableStockItemByProduct() {
    // When
    Optional<Stock> result = stockRepository.findFirstAvailableByProduct(testProduct1);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getProduct().getId()).isEqualTo(testProduct1.getId());
    assertThat(result.get().getSold()).isFalse();
    assertThat(result.get().isAvailable()).isTrue();
  }

  @Test
  @DisplayName("Should find first available stock item by product ID")
  void shouldFindFirstAvailableStockItemByProductId() {
    // When
    Optional<Stock> result = stockRepository.findFirstAvailableByProductId(testProduct1.getId());

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getProduct().getId()).isEqualTo(testProduct1.getId());
    assertThat(result.get().getSold()).isFalse();
    assertThat(result.get().isAvailable()).isTrue();
  }

  @Test
  @DisplayName("Should return empty when no available stock exists")
  void shouldReturnEmptyWhenNoAvailableStockExists() {
    // Given - Create a product with only sold stock
    Product productWithoutStock =
        new Product("No Stock Product", new BigDecimal("50.00"), testCategory);
    entityManager.persistAndFlush(productWithoutStock);

    Stock onlySoldStock = new Stock(productWithoutStock, "sold:credentials");
    onlySoldStock.markAsSold();
    entityManager.persistAndFlush(onlySoldStock);
    entityManager.flush();

    // When
    Optional<Stock> result = stockRepository.findFirstAvailableByProduct(productWithoutStock);

    // Then
    assertThat(result).isEmpty();
  }

  // ==================== SOLD STOCK QUERIES TESTS ====================

  @Test
  @DisplayName("Should find sold stock items")
  void shouldFindSoldStockItems() {
    // When
    List<Stock> result = stockRepository.findBySoldTrue();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCredentials()).isEqualTo("username3:password3");
    assertThat(result.get(0).getSold()).isTrue();
    assertThat(result.get(0).getSoldAt()).isNotNull();
  }

  @Test
  @DisplayName("Should find sold stock items by product")
  void shouldFindSoldStockItemsByProduct() {
    // When
    List<Stock> result = stockRepository.findByProductAndSoldTrue(testProduct1);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCredentials()).isEqualTo("username3:password3");
    assertThat(result.get(0).getProduct().getId()).isEqualTo(testProduct1.getId());
  }

  @Test
  @DisplayName("Should find sold stock items within date range")
  void shouldFindSoldStockItemsWithinDateRange() {
    // Given
    LocalDateTime startDate = LocalDateTime.now().minusHours(1);
    LocalDateTime endDate = LocalDateTime.now().plusHours(1);

    // When
    List<Stock> result = stockRepository.findSoldBetweenDates(startDate, endDate);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCredentials()).isEqualTo("username3:password3");
    assertThat(result.get(0).getSoldAt()).isBetween(startDate, endDate);
  }

  @Test
  @DisplayName("Should find sold stock items by product within date range")
  void shouldFindSoldStockItemsByProductWithinDateRange() {
    // Given
    LocalDateTime startDate = LocalDateTime.now().minusHours(1);
    LocalDateTime endDate = LocalDateTime.now().plusHours(1);

    // When
    List<Stock> result =
        stockRepository.findSoldByProductBetweenDates(testProduct1, startDate, endDate);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getProduct().getId()).isEqualTo(testProduct1.getId());
    assertThat(result.get(0).getSoldAt()).isBetween(startDate, endDate);
  }

  // ==================== RESERVATION QUERIES TESTS ====================

  @Test
  @DisplayName("Should find reserved stock items")
  void shouldFindReservedStockItems() {
    // When
    List<Stock> result = stockRepository.findReservedStock();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCredentials()).isEqualTo("username4:password4");
    assertThat(result.get(0).isReserved()).isTrue();
  }

  @Test
  @DisplayName("Should find reserved stock items by product")
  void shouldFindReservedStockItemsByProduct() {
    // When
    List<Stock> result = stockRepository.findReservedStockByProduct(testProduct2);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getProduct().getId()).isEqualTo(testProduct2.getId());
    assertThat(result.get(0).isReserved()).isTrue();
  }

  @Test
  @DisplayName("Should find expired reservation stock items")
  void shouldFindExpiredReservationStockItems() {
    // When
    List<Stock> result = stockRepository.findExpiredReservations();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCredentials()).isEqualTo("username5:password5");
    assertThat(result.get(0).isExpiredReservation()).isTrue();
  }

  @Test
  @DisplayName("Should find reservations expiring soon")
  void shouldFindReservationsExpiringSoon() {
    // Given
    LocalDateTime threshold = LocalDateTime.now().plusHours(1);

    // When
    List<Stock> result = stockRepository.findReservationsExpiringSoon(threshold);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCredentials()).isEqualTo("username4:password4");
    assertThat(result.get(0).getReservedUntil()).isBefore(threshold);
  }

  // ==================== STOCK COUNTING QUERIES TESTS ====================

  @Test
  @DisplayName("Should count total stock items by product")
  void shouldCountTotalStockItemsByProduct() {
    // When
    long result = stockRepository.countByProduct(testProduct1);

    // Then
    assertThat(result).isEqualTo(3); // 2 available + 1 sold
  }

  @Test
  @DisplayName("Should count available stock items by product")
  void shouldCountAvailableStockItemsByProduct() {
    // When
    long result = stockRepository.countAvailableByProduct(testProduct1);

    // Then
    assertThat(result).isEqualTo(2);
  }

  @Test
  @DisplayName("Should count available stock items by product ID")
  void shouldCountAvailableStockItemsByProductId() {
    // When
    long result = stockRepository.countAvailableByProductId(testProduct1.getId());

    // Then
    assertThat(result).isEqualTo(2);
  }

  @Test
  @DisplayName("Should count sold stock items by product")
  void shouldCountSoldStockItemsByProduct() {
    // When
    long result = stockRepository.countByProductAndSoldTrue(testProduct1);

    // Then
    assertThat(result).isEqualTo(1);
  }

  @Test
  @DisplayName("Should count reserved stock items by product")
  void shouldCountReservedStockItemsByProduct() {
    // When
    long result = stockRepository.countReservedByProduct(testProduct2);

    // Then
    assertThat(result).isEqualTo(1);
  }

  // ==================== BULK OPERATIONS TESTS ====================

  @Test
  @DisplayName("Should clear expired reservations in bulk")
  void shouldClearExpiredReservationsInBulk() {
    // Given - Verify we have expired reservations
    List<Stock> expiredBefore = stockRepository.findExpiredReservations();
    assertThat(expiredBefore).hasSize(1);

    // When
    int clearedCount = stockRepository.clearExpiredReservations();
    entityManager.flush(); // Force flush to database
    entityManager.clear(); // Clear persistence context

    // Then
    assertThat(clearedCount).isEqualTo(1);

    List<Stock> expiredAfter = stockRepository.findExpiredReservations();
    assertThat(expiredAfter).isEmpty();

    // Verify the stock item is now available
    Stock updatedStock = stockRepository.findById(expiredReservationStock.getId()).orElseThrow();
    assertThat(updatedStock.getReservedUntil()).isNull();
    assertThat(updatedStock.isAvailable()).isTrue();
  }

  @Test
  @DisplayName("Should reserve stock items in bulk")
  void shouldReserveStockItemsInBulk() {
    // Given
    LocalDateTime reservationTime = LocalDateTime.now().plusHours(1);

    // When
    int reservedCount = stockRepository.reserveStockItems(testProduct1.getId(), reservationTime, 1);

    // Then
    assertThat(reservedCount).isEqualTo(1);

    List<Stock> reservedItems = stockRepository.findReservedStockByProduct(testProduct1);
    assertThat(reservedItems).hasSize(1);
    assertThat(reservedItems.get(0).getReservedUntil()).isEqualToIgnoringNanos(reservationTime);
  }

  @Test
  @DisplayName("Should release reservations for specific stock items")
  void shouldReleaseReservationsForSpecificStockItems() {
    // Given
    List<UUID> stockIds = List.of(reservedStock.getId());

    // Verify reservation exists
    assertThat(reservedStock.isReserved()).isTrue();

    // When
    int releasedCount = stockRepository.releaseReservations(stockIds);

    // Then
    assertThat(releasedCount).isEqualTo(1);

    Stock updatedStock = stockRepository.findById(reservedStock.getId()).orElseThrow();
    assertThat(updatedStock.getReservedUntil()).isNull();
    assertThat(updatedStock.isAvailable()).isTrue();
  }

  // ==================== ADVANCED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find products with low stock")
  void shouldFindProductsWithLowStock() {
    // When - Using threshold of 3 (testProduct1 has 2 available, testProduct2 has 1 available)
    List<Product> result = stockRepository.findProductsWithLowStock(3);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(Product::getName)
        .containsExactlyInAnyOrder("Test Product 1", "Test Product 2");
  }

  @Test
  @DisplayName("Should check if stock exists with specific credentials")
  void shouldCheckIfStockExistsWithSpecificCredentials() {
    // When & Then
    assertThat(
            stockRepository.existsByProductIdAndCredentials(
                testProduct1.getId(), "username1:password1"))
        .isTrue();
    assertThat(
            stockRepository.existsByProductIdAndCredentials(
                testProduct1.getId(), "nonexistent:credentials"))
        .isFalse();
    assertThat(
            stockRepository.existsByProductIdAndCredentials(
                testProduct2.getId(), "username1:password1"))
        .isFalse();
  }

  @Test
  @DisplayName("Should find oldest unsold stock by product")
  void shouldFindOldestUnsoldStockByProduct() {
    // When
    Optional<Stock> result = stockRepository.findOldestUnsoldByProduct(testProduct1);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getSold()).isFalse();
    // Should be either availableStock1 or availableStock2 (oldest created)
    assertThat(result.get().getCredentials()).isIn("username1:password1", "username2:password2");
  }

  @Test
  @DisplayName("Should get stock statistics by product ID")
  void shouldGetStockStatisticsByProductId() {
    // When
    Object[] result = stockRepository.getStockStatisticsByProductId(testProduct1.getId());

    // Then - The result is a single-row array containing all the aggregate values
    assertThat(result).hasSize(1);
    Object[] stats = (Object[]) result[0];
    assertThat(stats[0]).isEqualTo(3L); // total
    assertThat(stats[1]).isEqualTo(2L); // available
    assertThat(stats[2]).isEqualTo(1L); // sold
    assertThat(stats[3]).isEqualTo(0L); // reserved
  }

  @Test
  @DisplayName("Should find stock created within date range")
  void shouldFindStockCreatedWithinDateRange() {
    // Given
    LocalDateTime startDate = LocalDateTime.now().minusHours(1);
    LocalDateTime endDate = LocalDateTime.now().plusHours(1);

    // When
    List<Stock> result = stockRepository.findStockCreatedBetweenDates(startDate, endDate);

    // Then
    assertThat(result).hasSize(5); // All test stock items
    assertThat(result)
        .allSatisfy(stock -> assertThat(stock.getCreatedAt()).isBetween(startDate, endDate));
  }

  @Test
  @DisplayName("Should find duplicate credentials by product ID")
  void shouldFindDuplicateCredentialsByProductId() {
    // Given - Add a duplicate credential
    Stock duplicateStock = new Stock(testProduct1, "username1:password1", "Duplicate credentials");
    entityManager.persistAndFlush(duplicateStock);

    // When
    List<String> result = stockRepository.findDuplicateCredentialsByProductId(testProduct1.getId());

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("username1:password1");
  }

  // ==================== EDGE CASES AND ERROR SCENARIOS ====================

  @Test
  @DisplayName("Should handle empty results gracefully")
  void shouldHandleEmptyResultsGracefully() {
    // Given
    Product emptyProduct = new Product("Empty Product", new BigDecimal("10.00"), testCategory);
    entityManager.persistAndFlush(emptyProduct);

    // When & Then
    assertThat(stockRepository.findByProduct(emptyProduct)).isEmpty();
    assertThat(stockRepository.findAvailableStockByProduct(emptyProduct)).isEmpty();
    assertThat(stockRepository.findFirstAvailableByProduct(emptyProduct)).isEmpty();
    assertThat(stockRepository.countByProduct(emptyProduct)).isZero();
    assertThat(stockRepository.countAvailableByProduct(emptyProduct)).isZero();
  }

  @Test
  @DisplayName("Should handle non-existent product ID")
  void shouldHandleNonExistentProductId() {
    // Given
    UUID nonExistentId = UUID.randomUUID();

    // When & Then
    assertThat(stockRepository.findByProductId(nonExistentId)).isEmpty();
    assertThat(stockRepository.findAvailableStockByProductId(nonExistentId)).isEmpty();
    assertThat(stockRepository.countAvailableByProductId(nonExistentId)).isZero();
  }

  @Test
  @DisplayName("Should handle bulk operations with no matching records")
  void shouldHandleBulkOperationsWithNoMatchingRecords() {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    LocalDateTime futureTime = LocalDateTime.now().plusDays(1);

    // When & Then
    assertThat(stockRepository.reserveStockItems(nonExistentId, futureTime, 10)).isZero();
    assertThat(stockRepository.releaseReservations(List.of(nonExistentId))).isZero();
  }
}
