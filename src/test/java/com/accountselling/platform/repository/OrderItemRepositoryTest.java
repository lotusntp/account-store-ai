package com.accountselling.platform.repository;

import static org.assertj.core.api.Assertions.*;

import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
 * Comprehensive unit tests for OrderItemRepository. Tests order item management operations
 * including product analysis, user purchases, and sales reporting functionality.
 *
 * <p>การทดสอบ OrderItemRepository ครอบคลุมการจัดการรายการสินค้าในคำสั่งซื้อ การวิเคราะห์สินค้า
 * การซื้อของผู้ใช้ และการรายงานการขาย
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("OrderItem Repository Tests")
class OrderItemRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private OrderItemRepository orderItemRepository;

  private User testUser1;
  private User testUser2;
  private Category testCategory;
  private Product testProduct1;
  private Product testProduct2;
  private Stock testStock1;
  private Stock testStock2;
  private Stock testStock3;
  private Order testOrder1;
  private Order testOrder2;
  private OrderItem orderItem1;
  private OrderItem orderItem2;
  private OrderItem orderItem3;

  @BeforeEach
  void setUp() {
    // Create test users
    String validPassword = "$2a$10$12345678901234567890123456789012345678901234567890122";
    testUser1 = new User("testuser1", validPassword, "user1@test.com");
    entityManager.persistAndFlush(testUser1);

    testUser2 = new User("testuser2", validPassword, "user2@test.com");
    entityManager.persistAndFlush(testUser2);

    // Create test category
    testCategory = new Category("Test Category");
    entityManager.persistAndFlush(testCategory);

    // Create test products
    testProduct1 = new Product("Test Product 1", new BigDecimal("100.00"), testCategory);
    testProduct1.setServer("Server1");
    entityManager.persistAndFlush(testProduct1);

    testProduct2 = new Product("Test Product 2", new BigDecimal("200.00"), testCategory);
    testProduct2.setServer("Server2");
    entityManager.persistAndFlush(testProduct2);

    // Create test stock items
    testStock1 = new Stock(testProduct1, "user1:pass1", "Stock 1 info");
    testStock1.markAsSold();
    entityManager.persistAndFlush(testStock1);

    testStock2 = new Stock(testProduct1, "user2:pass2", "Stock 2 info");
    testStock2.markAsSold();
    entityManager.persistAndFlush(testStock2);

    testStock3 = new Stock(testProduct2, "user3:pass3", "Stock 3 info");
    testStock3.markAsSold();
    entityManager.persistAndFlush(testStock3);

    // Create test orders
    testOrder1 = new Order(testUser1, new BigDecimal("300.00"), OrderStatus.COMPLETED);
    entityManager.persistAndFlush(testOrder1);

    testOrder2 = new Order(testUser2, new BigDecimal("200.00"), OrderStatus.COMPLETED);
    entityManager.persistAndFlush(testOrder2);

    // Create test order items
    orderItem1 =
        new OrderItem(
            testOrder1, testProduct1, testStock1, new BigDecimal("100.00"), "Order item 1");
    entityManager.persistAndFlush(orderItem1);

    orderItem2 =
        new OrderItem(
            testOrder1, testProduct1, testStock2, new BigDecimal("100.00"), "Order item 2");
    entityManager.persistAndFlush(orderItem2);

    orderItem3 =
        new OrderItem(
            testOrder2, testProduct2, testStock3, new BigDecimal("200.00"), "Order item 3");
    entityManager.persistAndFlush(orderItem3);

    entityManager.clear(); // Clear persistence context
  }

  // ==================== BASIC ORDER ITEM QUERIES TESTS ====================

  @Test
  @DisplayName("Should find order items by order")
  void shouldFindOrderItemsByOrder() {
    // When
    List<OrderItem> result = orderItemRepository.findByOrder(testOrder1);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(OrderItem::getNotes)
        .containsExactlyInAnyOrder("Order item 1", "Order item 2");
  }

  @Test
  @DisplayName("Should find order items by order with pagination")
  void shouldFindOrderItemsByOrderWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 1);

    // When
    Page<OrderItem> result = orderItemRepository.findByOrder(testOrder1, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should find order items by order ID")
  void shouldFindOrderItemsByOrderId() {
    // When
    List<OrderItem> result = orderItemRepository.findByOrderId(testOrder1.getId());

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .allSatisfy(item -> assertThat(item.getOrder().getId()).isEqualTo(testOrder1.getId()));
  }

  @Test
  @DisplayName("Should find order item by stock item")
  void shouldFindOrderItemByStockItem() {
    // When
    Optional<OrderItem> result = orderItemRepository.findByStockItem(testStock1);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getStockItem().getId()).isEqualTo(testStock1.getId());
    assertThat(result.get().getNotes()).isEqualTo("Order item 1");
  }

  @Test
  @DisplayName("Should find order item by stock item ID")
  void shouldFindOrderItemByStockItemId() {
    // When
    Optional<OrderItem> result = orderItemRepository.findByStockItemId(testStock1.getId());

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getStockItem().getId()).isEqualTo(testStock1.getId());
  }

  // ==================== PRODUCT-BASED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find order items by product")
  void shouldFindOrderItemsByProduct() {
    // When
    List<OrderItem> result = orderItemRepository.findByProduct(testProduct1);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .allSatisfy(item -> assertThat(item.getProduct().getId()).isEqualTo(testProduct1.getId()));
  }

  @Test
  @DisplayName("Should find order items by product ID")
  void shouldFindOrderItemsByProductId() {
    // When
    List<OrderItem> result = orderItemRepository.findByProductId(testProduct2.getId());

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getProduct().getId()).isEqualTo(testProduct2.getId());
  }

  @Test
  @DisplayName("Should find order items by product name")
  void shouldFindOrderItemsByProductName() {
    // When
    List<OrderItem> result = orderItemRepository.findByProductNameContainingIgnoreCase("Product 1");

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .allSatisfy(item -> assertThat(item.getProductName()).containsIgnoringCase("Product 1"));
  }

  @Test
  @DisplayName("Should find order items by category name")
  void shouldFindOrderItemsByCategoryName() {
    // When
    List<OrderItem> result = orderItemRepository.findByCategoryName("Test Category");

    // Then
    assertThat(result).hasSize(3);
    assertThat(result)
        .allSatisfy(item -> assertThat(item.getCategoryName()).isEqualTo("Test Category"));
  }

  @Test
  @DisplayName("Should find order items by server")
  void shouldFindOrderItemsByServer() {
    // When
    List<OrderItem> result = orderItemRepository.findByServer("Server1");

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).allSatisfy(item -> assertThat(item.getServer()).isEqualTo("Server1"));
  }

  // ==================== USER-BASED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find order items by user")
  void shouldFindOrderItemsByUser() {
    // When
    List<OrderItem> result = orderItemRepository.findByUser(testUser1);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .allSatisfy(
            item -> assertThat(item.getOrder().getUser().getId()).isEqualTo(testUser1.getId()));
  }

  @Test
  @DisplayName("Should find order items by username")
  void shouldFindOrderItemsByUsername() {
    // When
    List<OrderItem> result = orderItemRepository.findByUsername("testuser2");

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOrder().getUser().getUsername()).isEqualTo("testuser2");
  }

  @Test
  @DisplayName("Should find order items by user and product")
  void shouldFindOrderItemsByUserAndProduct() {
    // When
    List<OrderItem> result = orderItemRepository.findByUserAndProduct(testUser1, testProduct1);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .allSatisfy(
            item -> {
              assertThat(item.getOrder().getUser().getId()).isEqualTo(testUser1.getId());
              assertThat(item.getProduct().getId()).isEqualTo(testProduct1.getId());
            });
  }

  // ==================== DATE-BASED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find order items created within date range")
  void shouldFindOrderItemsCreatedWithinDateRange() {
    // Given
    LocalDateTime startDate = LocalDateTime.now().minusHours(1);
    LocalDateTime endDate = LocalDateTime.now().plusHours(1);

    // When
    List<OrderItem> result = orderItemRepository.findByCreatedAtBetween(startDate, endDate);

    // Then
    assertThat(result).hasSize(3);
    assertThat(result)
        .allSatisfy(item -> assertThat(item.getCreatedAt()).isBetween(startDate, endDate));
  }

  @Test
  @DisplayName("Should find order items by product within date range")
  void shouldFindOrderItemsByProductWithinDateRange() {
    // Given
    LocalDateTime startDate = LocalDateTime.now().minusHours(1);
    LocalDateTime endDate = LocalDateTime.now().plusHours(1);

    // When
    List<OrderItem> result =
        orderItemRepository.findByProductAndCreatedAtBetween(testProduct1, startDate, endDate);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .allSatisfy(
            item -> {
              assertThat(item.getProduct().getId()).isEqualTo(testProduct1.getId());
              assertThat(item.getCreatedAt()).isBetween(startDate, endDate);
            });
  }

  // ==================== PRICE-BASED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find order items by price range")
  void shouldFindOrderItemsByPriceRange() {
    // Given
    BigDecimal minPrice = new BigDecimal("100.00");
    BigDecimal maxPrice = new BigDecimal("150.00");

    // When
    List<OrderItem> result = orderItemRepository.findByPriceBetween(minPrice, maxPrice);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .allSatisfy(item -> assertThat(item.getPrice()).isBetween(minPrice, maxPrice));
  }

  @Test
  @DisplayName("Should find order items with price greater than threshold")
  void shouldFindOrderItemsWithPriceGreaterThan() {
    // Given
    BigDecimal threshold = new BigDecimal("150.00");

    // When
    List<OrderItem> result = orderItemRepository.findByPriceGreaterThan(threshold);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPrice()).isGreaterThan(threshold);
  }

  // ==================== COUNTING QUERIES TESTS ====================

  @Test
  @DisplayName("Should count order items by order")
  void shouldCountOrderItemsByOrder() {
    // When
    long count = orderItemRepository.countByOrder(testOrder1);

    // Then
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("Should count order items by product")
  void shouldCountOrderItemsByProduct() {
    // When
    long count = orderItemRepository.countByProduct(testProduct1);

    // Then
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("Should count order items by user")
  void shouldCountOrderItemsByUser() {
    // When
    long count = orderItemRepository.countByUser(testUser1);

    // Then
    assertThat(count).isEqualTo(2);
  }

  // ==================== AGGREGATION QUERIES TESTS ====================

  @Test
  @DisplayName("Should calculate total revenue by product")
  void shouldCalculateTotalRevenueByProduct() {
    // When
    BigDecimal revenue = orderItemRepository.calculateTotalRevenueByProduct(testProduct1);

    // Then
    assertThat(revenue).isEqualByComparingTo(new BigDecimal("200.00")); // 100 + 100
  }

  @Test
  @DisplayName("Should calculate total spent by user")
  void shouldCalculateTotalSpentByUser() {
    // When
    BigDecimal totalSpent = orderItemRepository.calculateTotalSpentByUser(testUser1);

    // Then
    assertThat(totalSpent).isEqualByComparingTo(new BigDecimal("200.00"));
  }

  @Test
  @DisplayName("Should calculate total revenue by category")
  void shouldCalculateTotalRevenueByCategoryName() {
    // When
    BigDecimal revenue = orderItemRepository.calculateTotalRevenueByCategoryName("Test Category");

    // Then
    assertThat(revenue).isEqualByComparingTo(new BigDecimal("400.00")); // Sum of all order items
  }

  @Test
  @DisplayName("Should calculate average price by product")
  void shouldCalculateAveragePriceByProduct() {
    // When
    BigDecimal avgPrice = orderItemRepository.calculateAveragePriceByProduct(testProduct1);

    // Then
    assertThat(avgPrice).isEqualByComparingTo(new BigDecimal("100.00"));
  }

  // ==================== REPORTING QUERIES TESTS ====================

  @Test
  @DisplayName("Should get top selling products")
  void shouldGetTopSellingProducts() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<Object[]> result = orderItemRepository.getTopSellingProducts(pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);

    // Check top selling product
    Object[] topProduct = result.getContent().get(0);
    Product product = (Product) topProduct[0];
    Long salesCount = (Long) topProduct[1];

    assertThat(product.getName()).isEqualTo("Test Product 1");
    assertThat(salesCount).isEqualTo(2L);
  }

  @Test
  @DisplayName("Should get sales statistics by category")
  void shouldGetSalesStatisticsByCategory() {
    // When
    List<Object[]> result = orderItemRepository.getSalesStatisticsByCategory();

    // Then
    assertThat(result).hasSize(1);

    Object[] categoryStat = result.get(0);
    String categoryName = (String) categoryStat[0];
    Long salesCount = (Long) categoryStat[1];
    BigDecimal totalRevenue = (BigDecimal) categoryStat[2];

    assertThat(categoryName).isEqualTo("Test Category");
    assertThat(salesCount).isEqualTo(3L);
    assertThat(totalRevenue).isEqualByComparingTo(new BigDecimal("400.00"));
  }

  // ==================== SEARCH QUERIES TESTS ====================

  @Test
  @DisplayName("Should search order items by multiple criteria")
  void shouldSearchOrderItemsByMultipleCriteria() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<OrderItem> result =
        orderItemRepository.searchOrderItems(
            "Product 1",
            "Test Category",
            "Server1",
                OrderStatus.PENDING,
            null,
            null,
            new BigDecimal("50.00"),
            new BigDecimal("150.00"),
            pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .allSatisfy(
            item -> {
              assertThat(item.getProductName()).contains("Product 1");
              assertThat(item.getCategoryName()).isEqualTo("Test Category");
              assertThat(item.getServer()).isEqualTo("Server1");
            });
  }

  @Test
  @DisplayName("Should find order items by completed orders")
  void shouldFindOrderItemsByCompletedOrders() {
    // When
    List<OrderItem> result = orderItemRepository.findByCompletedOrders();

    // Then
    assertThat(result).hasSize(3);
    assertThat(result)
        .allSatisfy(
            item -> assertThat(item.getOrder().getStatus()).isEqualTo(OrderStatus.COMPLETED));
  }

  @Test
  @DisplayName("Should find distinct category names")
  void shouldFindDistinctCategoryNames() {
    // When
    List<String> result = orderItemRepository.findDistinctCategoryNames();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result).contains("Test Category");
  }

  @Test
  @DisplayName("Should find distinct servers")
  void shouldFindDistinctServers() {
    // When
    List<String> result = orderItemRepository.findDistinctServers();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).containsExactlyInAnyOrder("Server1", "Server2");
  }

  // ==================== EDGE CASES TESTS ====================

  @Test
  @DisplayName("Should handle empty results gracefully")
  void shouldHandleEmptyResultsGracefully() {
    // Given
    String validPassword = "$2a$10$12345678901234567890123456789012345678901234567890122";
    User emptyUser = new User("emptyuser", validPassword, "empty@test.com");
    entityManager.persistAndFlush(emptyUser);

    // When & Then
    assertThat(orderItemRepository.findByUser(emptyUser)).isEmpty();
    assertThat(orderItemRepository.countByUser(emptyUser)).isZero();
    assertThat(orderItemRepository.calculateTotalSpentByUser(emptyUser))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("Should handle non-existent stock item")
  void shouldHandleNonExistentStockItem() {
    // Given
    Stock nonExistentStock = new Stock(testProduct1, "nonexistent:stock");
    entityManager.persistAndFlush(nonExistentStock);

    // When
    Optional<OrderItem> result = orderItemRepository.findByStockItem(nonExistentStock);

    // Then
    assertThat(result).isEmpty();
  }
}
