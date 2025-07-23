package com.accountselling.platform.repository;

import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for OrderRepository.
 * Tests all order management operations including order tracking,
 * status management, user orders, and reporting functionality.
 * 
 * การทดสอบ OrderRepository ครอบคลุมการจัดการคำสั่งซื้อ
 * การติดตามสถานะ คำสั่งซื้อของผู้ใช้ และการรายงาน
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Order Repository Tests")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private User testUser1;
    private User testUser2;
    private Order pendingOrder;
    private Order processingOrder;
    private Order completedOrder;
    private Order failedOrder;
    private Order cancelledOrder;

    @BeforeEach
    void setUp() {
        // Create test users
        String validPassword = "$2a$10$12345678901234567890123456789012345678901234567890122";
        testUser1 = new User("testuser1", validPassword, "user1@test.com");
        testUser1.setFirstName("Test");
        testUser1.setLastName("User1");
        entityManager.persistAndFlush(testUser1);

        testUser2 = new User("testuser2", validPassword, "user2@test.com");
        testUser2.setFirstName("Test");
        testUser2.setLastName("User2");
        entityManager.persistAndFlush(testUser2);

        // Create test orders with different statuses
        pendingOrder = new Order(testUser1, new BigDecimal("100.00"), OrderStatus.PENDING);
        pendingOrder.setOrderNumber("ORD-PENDING-001");
        entityManager.persistAndFlush(pendingOrder);

        processingOrder = new Order(testUser1, new BigDecimal("200.00"), OrderStatus.PROCESSING);
        processingOrder.setOrderNumber("ORD-PROCESSING-001");
        entityManager.persistAndFlush(processingOrder);

        completedOrder = new Order(testUser1, new BigDecimal("300.00"), OrderStatus.COMPLETED);
        completedOrder.setOrderNumber("ORD-COMPLETED-001");
        entityManager.persistAndFlush(completedOrder);

        failedOrder = new Order(testUser2, new BigDecimal("150.00"), OrderStatus.FAILED);
        failedOrder.setOrderNumber("ORD-FAILED-001");
        entityManager.persistAndFlush(failedOrder);

        cancelledOrder = new Order(testUser2, new BigDecimal("250.00"), OrderStatus.CANCELLED);
        cancelledOrder.setOrderNumber("ORD-CANCELLED-001");
        entityManager.persistAndFlush(cancelledOrder);

        entityManager.clear(); // Clear persistence context
    }

    // ==================== BASIC ORDER QUERIES TESTS ====================

    @Test
    @DisplayName("Should find order by order number")
    void shouldFindOrderByOrderNumber() {
        // When
        Optional<Order> result = orderRepository.findByOrderNumber("ORD-PENDING-001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderNumber()).isEqualTo("ORD-PENDING-001");
        assertThat(result.get().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.get().getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should return empty when order number not found")
    void shouldReturnEmptyWhenOrderNumberNotFound() {
        // When
        Optional<Order> result = orderRepository.findByOrderNumber("NON-EXISTENT-ORDER");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should check if order number exists")
    void shouldCheckIfOrderNumberExists() {
        // When & Then
        assertThat(orderRepository.existsByOrderNumber("ORD-PENDING-001")).isTrue();
        assertThat(orderRepository.existsByOrderNumber("NON-EXISTENT-ORDER")).isFalse();
    }

    @Test
    @DisplayName("Should find orders by user")
    void shouldFindOrdersByUser() {
        // When
        List<Order> result = orderRepository.findByUser(testUser1);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result)
            .extracting(Order::getOrderNumber)
            .containsExactlyInAnyOrder("ORD-PENDING-001", "ORD-PROCESSING-001", "ORD-COMPLETED-001");
    }

    @Test
    @DisplayName("Should find orders by user with pagination")
    void shouldFindOrdersByUserWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Order> result = orderRepository.findByUser(testUser1, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find orders by user ID")
    void shouldFindOrdersByUserId() {
        // When
        List<Order> result = orderRepository.findByUserId(testUser1.getId());

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(order -> 
            assertThat(order.getUser().getId()).isEqualTo(testUser1.getId())
        );
    }

    // ==================== STATUS-BASED QUERIES TESTS ====================

    @Test
    @DisplayName("Should find orders by status")
    void shouldFindOrdersByStatus() {
        // When
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        List<Order> completedOrders = orderRepository.findByStatus(OrderStatus.COMPLETED);

        // Then
        assertThat(pendingOrders).hasSize(1);
        assertThat(pendingOrders.get(0).getOrderNumber()).isEqualTo("ORD-PENDING-001");

        assertThat(completedOrders).hasSize(1);
        assertThat(completedOrders.get(0).getOrderNumber()).isEqualTo("ORD-COMPLETED-001");
    }

    @Test
    @DisplayName("Should find orders by status with pagination")
    void shouldFindOrdersByStatusWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Order> result = orderRepository.findByStatus(OrderStatus.PENDING, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find orders by user and status")
    void shouldFindOrdersByUserAndStatus() {
        // When
        List<Order> result = orderRepository.findByUserAndStatus(testUser1, OrderStatus.COMPLETED);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderNumber()).isEqualTo("ORD-COMPLETED-001");
        assertThat(result.get(0).getUser().getId()).isEqualTo(testUser1.getId());
    }

    @Test
    @DisplayName("Should find orders by multiple statuses")
    void shouldFindOrdersByMultipleStatuses() {
        // Given
        List<OrderStatus> statuses = List.of(OrderStatus.PENDING, OrderStatus.COMPLETED);

        // When
        List<Order> result = orderRepository.findByStatusIn(statuses);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(Order::getStatus)
            .containsExactlyInAnyOrder(OrderStatus.PENDING, OrderStatus.COMPLETED);
    }

    // ==================== DATE-BASED QUERIES TESTS ====================

    @Test
    @DisplayName("Should find orders created within date range")
    void shouldFindOrdersCreatedWithinDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        List<Order> result = orderRepository.findByCreatedAtBetween(startDate, endDate);

        // Then
        assertThat(result).hasSize(5); // All test orders
        assertThat(result).allSatisfy(order -> {
            assertThat(order.getCreatedAt()).isBetween(startDate, endDate);
        });
    }

    @Test
    @DisplayName("Should find orders by user within date range")
    void shouldFindOrdersByUserWithinDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        List<Order> result = orderRepository.findByUserAndCreatedAtBetween(testUser1, startDate, endDate);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(order -> {
            assertThat(order.getUser().getId()).isEqualTo(testUser1.getId());
            assertThat(order.getCreatedAt()).isBetween(startDate, endDate);
        });
    }

    @Test
    @DisplayName("Should find orders by status within date range")
    void shouldFindOrdersByStatusWithinDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        List<Order> result = orderRepository.findByStatusAndCreatedAtBetween(OrderStatus.COMPLETED, startDate, endDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(result.get(0).getCreatedAt()).isBetween(startDate, endDate);
    }

    // ==================== AMOUNT-BASED QUERIES TESTS ====================

    @Test
    @DisplayName("Should find orders by total amount range")
    void shouldFindOrdersByTotalAmountRange() {
        // Given
        BigDecimal minAmount = new BigDecimal("150.00");
        BigDecimal maxAmount = new BigDecimal("300.00");

        // When
        List<Order> result = orderRepository.findByTotalAmountBetween(minAmount, maxAmount);

        // Then
        assertThat(result).hasSize(4); // 150.00, 200.00, 250.00, 300.00
        assertThat(result).allSatisfy(order -> {
            assertThat(order.getTotalAmount()).isBetween(minAmount, maxAmount);
        });
    }

    @Test
    @DisplayName("Should find orders with total amount greater than threshold")
    void shouldFindOrdersWithTotalAmountGreaterThan() {
        // Given
        BigDecimal threshold = new BigDecimal("200.00");

        // When
        List<Order> result = orderRepository.findByTotalAmountGreaterThan(threshold);

        // Then
        assertThat(result).hasSize(2); // 300.00, 250.00
        assertThat(result).allSatisfy(order -> {
            assertThat(order.getTotalAmount()).isGreaterThan(threshold);
        });
    }

    @Test
    @DisplayName("Should find orders with total amount less than threshold")
    void shouldFindOrdersWithTotalAmountLessThan() {
        // Given
        BigDecimal threshold = new BigDecimal("200.00");

        // When
        List<Order> result = orderRepository.findByTotalAmountLessThan(threshold);

        // Then
        assertThat(result).hasSize(2); // 100.00, 150.00
        assertThat(result).allSatisfy(order -> {
            assertThat(order.getTotalAmount()).isLessThan(threshold);
        });
    }

    // ==================== COUNTING QUERIES TESTS ====================

    @Test
    @DisplayName("Should count orders by user")
    void shouldCountOrdersByUser() {
        // When
        long user1Count = orderRepository.countByUser(testUser1);
        long user2Count = orderRepository.countByUser(testUser2);

        // Then
        assertThat(user1Count).isEqualTo(3);
        assertThat(user2Count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count orders by status")
    void shouldCountOrdersByStatus() {
        // When
        long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
        long completedCount = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long failedCount = orderRepository.countByStatus(OrderStatus.FAILED);

        // Then
        assertThat(pendingCount).isEqualTo(1);
        assertThat(completedCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should count orders by user and status")
    void shouldCountOrdersByUserAndStatus() {
        // When
        long user1CompletedCount = orderRepository.countByUserAndStatus(testUser1, OrderStatus.COMPLETED);
        long user2FailedCount = orderRepository.countByUserAndStatus(testUser2, OrderStatus.FAILED);

        // Then
        assertThat(user1CompletedCount).isEqualTo(1);
        assertThat(user2FailedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should count orders created within date range")
    void shouldCountOrdersCreatedWithinDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        long count = orderRepository.countByCreatedAtBetween(startDate, endDate);

        // Then
        assertThat(count).isEqualTo(5);
    }

    // ==================== AGGREGATION QUERIES TESTS ====================

    @Test
    @DisplayName("Should calculate total amount by user")
    void shouldCalculateTotalAmountByUser() {
        // When
        BigDecimal user1Total = orderRepository.calculateTotalAmountByUser(testUser1);
        BigDecimal user2Total = orderRepository.calculateTotalAmountByUser(testUser2);

        // Then
        assertThat(user1Total).isEqualByComparingTo(new BigDecimal("600.00")); // 100+200+300
        assertThat(user2Total).isEqualByComparingTo(new BigDecimal("400.00")); // 150+250
    }

    @Test
    @DisplayName("Should calculate completed amount by user")
    void shouldCalculateCompletedAmountByUser() {
        // When
        BigDecimal user1CompletedTotal = orderRepository.calculateCompletedAmountByUser(testUser1);
        BigDecimal user2CompletedTotal = orderRepository.calculateCompletedAmountByUser(testUser2);

        // Then
        assertThat(user1CompletedTotal).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(user2CompletedTotal).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should calculate total amount by status")
    void shouldCalculateTotalAmountByStatus() {
        // When
        BigDecimal completedTotal = orderRepository.calculateTotalAmountByStatus(OrderStatus.COMPLETED);
        BigDecimal failedTotal = orderRepository.calculateTotalAmountByStatus(OrderStatus.FAILED);

        // Then
        assertThat(completedTotal).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(failedTotal).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Should calculate total amount within date range")
    void shouldCalculateTotalAmountWithinDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        BigDecimal total = orderRepository.calculateTotalAmountBetween(startDate, endDate);

        // Then
        assertThat(total).isEqualByComparingTo(new BigDecimal("1000.00")); // Sum of all orders
    }

    @Test
    @DisplayName("Should calculate average amount by user")
    void shouldCalculateAverageAmountByUser() {
        // When
        BigDecimal user1Average = orderRepository.calculateAverageAmountByUser(testUser1);
        BigDecimal user2Average = orderRepository.calculateAverageAmountByUser(testUser2);

        // Then
        assertThat(user1Average).isEqualByComparingTo(new BigDecimal("200.00")); // 600/3
        assertThat(user2Average).isEqualByComparingTo(new BigDecimal("200.00")); // 400/2
    }

    // ==================== REPORTING QUERIES TESTS ====================

    @Test
    @DisplayName("Should get order statistics by status")
    void shouldGetOrderStatisticsByStatus() {
        // When
        List<Object[]> result = orderRepository.getOrderStatisticsByStatus();

        // Then
        assertThat(result).hasSizeGreaterThanOrEqualTo(5); // At least 5 different statuses
        
        // Check for specific status statistics
        boolean foundCompleted = false;
        for (Object[] stat : result) {
            OrderStatus status = (OrderStatus) stat[0];
            Long count = (Long) stat[1];
            BigDecimal totalAmount = (BigDecimal) stat[2];
            
            if (status == OrderStatus.COMPLETED) {
                assertThat(count).isEqualTo(1L);
                assertThat(totalAmount).isEqualByComparingTo(new BigDecimal("300.00"));
                foundCompleted = true;
            }
        }
        assertThat(foundCompleted).isTrue();
    }

    @Test
    @DisplayName("Should get order statistics within date range")
    void shouldGetOrderStatisticsWithinDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        List<Object[]> result = orderRepository.getOrderStatisticsByStatusBetween(startDate, endDate);

        // Then
        assertThat(result).isNotEmpty();
        
        // Verify that all statistics are within the date range
        long totalCount = result.stream().mapToLong(stat -> (Long) stat[1]).sum();
        assertThat(totalCount).isEqualTo(5L); // All our test orders
    }

    @Test
    @DisplayName("Should get top spending users")
    void shouldGetTopSpendingUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Object[]> result = orderRepository.getTopSpendingUsers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1); // Only users with completed orders
        
        // Verify the top spender
        Object[] topSpender = result.getContent().get(0);
        User topUser = (User) topSpender[0];
        Long orderCount = (Long) topSpender[1];
        BigDecimal totalSpent = (BigDecimal) topSpender[2];
        
        assertThat(topUser.getUsername()).isEqualTo("testuser1");
        assertThat(orderCount).isEqualTo(1L); // Only 1 completed order
        assertThat(totalSpent).isEqualByComparingTo(new BigDecimal("300.00")); // Only completed orders
    }

    @Test
    @DisplayName("Should find recent orders")
    void shouldFindRecentOrders() {
        // Given
        Pageable pageable = PageRequest.of(0, 3);

        // When
        Page<Order> result = orderRepository.findRecentOrders(pageable);

        // Then
        assertThat(result.getContent()).hasSize(3);
        
        // Verify orders are sorted by creation date descending
        List<Order> orders = result.getContent();
        for (int i = 0; i < orders.size() - 1; i++) {
            assertThat(orders.get(i).getCreatedAt()).isAfterOrEqualTo(orders.get(i + 1).getCreatedAt());
        }
    }

    // ==================== SEARCH QUERIES TESTS ====================

    @Test
    @DisplayName("Should search orders by multiple criteria")
    void shouldSearchOrdersByMultipleCriteria() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        BigDecimal minAmount = new BigDecimal("100.00");
        BigDecimal maxAmount = new BigDecimal("300.00");

        // When
        Page<Order> result = orderRepository.searchOrders(
            "PENDING", // order number pattern
            "testuser1", // username pattern
            OrderStatus.PENDING, // status
            null, // start date
            null, // end date
            minAmount,
            maxAmount,
            pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderNumber()).contains("PENDING");
        assertThat(result.getContent().get(0).getUser().getUsername()).isEqualTo("testuser1");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Should find orders by username")
    void shouldFindOrdersByUsername() {
        // When
        List<Order> result = orderRepository.findByUsername("testuser2");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(order -> 
            assertThat(order.getUser().getUsername()).isEqualTo("testuser2")
        );
    }

    @Test
    @DisplayName("Should find orders by username with pagination")
    void shouldFindOrdersByUsernameWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Order> result = orderRepository.findByUsername("testuser1", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().get(0).getUser().getUsername()).isEqualTo("testuser1");
    }

    // ==================== EDGE CASES AND ERROR SCENARIOS ====================

    @Test
    @DisplayName("Should handle empty results gracefully")
    void shouldHandleEmptyResultsGracefully() {
        // Given
        String validPassword = "$2a$10$12345678901234567890123456789012345678901234567890122";
        User emptyUser = new User("emptyuser", validPassword, "empty@test.com");
        entityManager.persistAndFlush(emptyUser);

        // When & Then
        assertThat(orderRepository.findByUser(emptyUser)).isEmpty();
        assertThat(orderRepository.countByUser(emptyUser)).isZero();
        assertThat(orderRepository.calculateTotalAmountByUser(emptyUser))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should handle non-existent user ID")
    void shouldHandleNonExistentUserId() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        assertThat(orderRepository.findByUserId(nonExistentId)).isEmpty();
    }

    @Test
    @DisplayName("Should handle null and empty search parameters")
    void shouldHandleNullAndEmptySearchParameters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When - Search with all null parameters
        Page<Order> result = orderRepository.searchOrders(
            null, null, null, null, null, null, null, pageable
        );

        // Then
        assertThat(result.getTotalElements()).isEqualTo(5); // All orders
    }

    @Test
    @DisplayName("Should handle date range with no matches")
    void shouldHandleDateRangeWithNoMatches() {
        // Given
        LocalDateTime futureStartDate = LocalDateTime.now().plusDays(1);
        LocalDateTime futureEndDate = LocalDateTime.now().plusDays(2);

        // When
        List<Order> result = orderRepository.findByCreatedAtBetween(futureStartDate, futureEndDate);
        long count = orderRepository.countByCreatedAtBetween(futureStartDate, futureEndDate);

        // Then
        assertThat(result).isEmpty();
        assertThat(count).isZero();
    }
}