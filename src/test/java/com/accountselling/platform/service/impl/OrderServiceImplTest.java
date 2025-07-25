package com.accountselling.platform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.accountselling.platform.dto.statistics.OrderStatistics;
import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.exception.*;
import com.accountselling.platform.model.*;
import com.accountselling.platform.repository.*;
import com.accountselling.platform.service.StockService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock private OrderRepository orderRepository;

  @Mock private OrderItemRepository orderItemRepository;

  @Mock private UserRepository userRepository;

  @Mock private ProductRepository productRepository;

  @Mock private StockRepository stockRepository;

  @Mock private StockService stockService;

  @InjectMocks private OrderServiceImpl orderService;

  private User testUser;
  private Product testProduct;
  private Stock testStock;
  private Order testOrder;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");

    testProduct = new Product();
    testProduct.setId(UUID.randomUUID());
    testProduct.setName("Test Product");
    testProduct.setPrice(BigDecimal.valueOf(100.00));

    testStock = new Stock();
    testStock.setId(UUID.randomUUID());
    testStock.setProduct(testProduct);
    testStock.setCredentials("test-credentials");
    testStock.setSold(false);

    testOrder = new Order(testUser, BigDecimal.valueOf(100.00));
    testOrder.setId(UUID.randomUUID());
  }

  // ==================== ORDER CREATION TESTS ====================

  @Test
  void createOrder_WithValidInput_ShouldCreateOrder() {
    // Arrange
    Map<UUID, Integer> productQuantities = Map.of(testProduct.getId(), 1);
    List<Stock> reservedStocks = List.of(testStock);

    when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    when(stockRepository.countAvailableByProductId(testProduct.getId())).thenReturn(5L);
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
    when(stockService.reserveStock(testProduct.getId(), 1, 30)).thenReturn(reservedStocks);

    // Act
    Order result = orderService.createOrder(testUser, productQuantities);

    // Assert
    assertNotNull(result);
    assertEquals(testUser, result.getUser());
    assertEquals(BigDecimal.valueOf(100.00), result.getTotalAmount());
    verify(orderRepository).save(any(Order.class));
    verify(stockService).reserveStock(testProduct.getId(), 1, 30);
  }

  @Test
  void createOrder_WithInsufficientStock_ShouldThrowException() {
    // Arrange
    Map<UUID, Integer> productQuantities = Map.of(testProduct.getId(), 5);

    when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    when(stockRepository.countAvailableByProductId(testProduct.getId())).thenReturn(2L);

    // Act & Assert
    assertThrows(
        InsufficientStockException.class,
        () -> orderService.createOrder(testUser, productQuantities));
  }

  @Test
  void createOrder_WithNonExistentProduct_ShouldThrowException() {
    // Arrange
    Map<UUID, Integer> productQuantities = Map.of(UUID.randomUUID(), 1);

    when(productRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(
        ResourceNotFoundException.class,
        () -> orderService.createOrder(testUser, productQuantities));
  }

  @Test
  void createOrder_WithInvalidQuantity_ShouldThrowException() {
    // Arrange
    Map<UUID, Integer> productQuantities = Map.of(testProduct.getId(), 0);

    // Act & Assert
    assertThrows(
        InvalidOrderException.class, () -> orderService.createOrder(testUser, productQuantities));
  }

  @Test
  void createOrderByUsername_WithValidUsername_ShouldCreateOrder() {
    // Arrange
    Map<UUID, Integer> productQuantities = Map.of(testProduct.getId(), 1);
    List<Stock> reservedStocks = List.of(testStock);

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    when(stockRepository.countAvailableByProductId(testProduct.getId())).thenReturn(5L);
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
    when(stockService.reserveStock(testProduct.getId(), 1, 30)).thenReturn(reservedStocks);

    // Act
    Order result = orderService.createOrderByUsername("testuser", productQuantities);

    // Assert
    assertNotNull(result);
    assertEquals(testUser, result.getUser());
    verify(userRepository).findByUsername("testuser");
  }

  @Test
  void createOrderByUsername_WithInvalidUsername_ShouldThrowException() {
    // Arrange
    Map<UUID, Integer> productQuantities = Map.of(testProduct.getId(), 1);

    when(userRepository.findByUsername("invaliduser")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(
        ResourceNotFoundException.class,
        () -> orderService.createOrderByUsername("invaliduser", productQuantities));
  }

  // ==================== ORDER RETRIEVAL TESTS ====================

  @Test
  void findById_WithValidId_ShouldReturnOrder() {
    // Arrange
    UUID orderId = testOrder.getId();
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

    // Act
    Order result = orderService.findById(orderId);

    // Assert
    assertNotNull(result);
    assertEquals(testOrder, result);
    verify(orderRepository).findById(orderId);
  }

  @Test
  void findById_WithInvalidId_ShouldThrowException() {
    // Arrange
    UUID orderId = UUID.randomUUID();
    when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> orderService.findById(orderId));
  }

  @Test
  void findByOrderNumber_WithValidOrderNumber_ShouldReturnOrder() {
    // Arrange
    String orderNumber = "ORD-123456";
    testOrder.setOrderNumber(orderNumber);
    when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(testOrder));

    // Act
    Order result = orderService.findByOrderNumber(orderNumber);

    // Assert
    assertNotNull(result);
    assertEquals(testOrder, result);
    verify(orderRepository).findByOrderNumber(orderNumber);
  }

  @Test
  void getOrdersByUser_WithValidUser_ShouldReturnOrders() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    List<Order> orders = List.of(testOrder);
    Page<Order> orderPage = new PageImpl<>(orders, pageable, 1);

    when(orderRepository.findByUser(testUser, pageable)).thenReturn(orderPage);

    // Act
    Page<Order> result = orderService.getOrdersByUser(testUser, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(testOrder, result.getContent().get(0));
    verify(orderRepository).findByUser(testUser, pageable);
  }

  // ==================== ORDER STATUS MANAGEMENT TESTS ====================

  @Test
  void markOrderAsProcessing_WithValidOrder_ShouldUpdateStatus() {
    // Arrange
    testOrder.setStatus(OrderStatus.PENDING);
    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Act
    Order result = orderService.markOrderAsProcessing(testOrder.getId());

    // Assert
    assertNotNull(result);
    assertEquals(OrderStatus.PROCESSING, result.getStatus());
    verify(orderRepository).save(testOrder);
  }

  @Test
  void markOrderAsCompleted_WithValidOrder_ShouldUpdateStatusAndMarkStockAsSold() {
    // Arrange
    testOrder.setStatus(OrderStatus.PROCESSING);
    OrderItem orderItem = new OrderItem(testOrder, testProduct, testStock, testProduct.getPrice());
    testOrder.addOrderItem(orderItem);

    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Act
    Order result = orderService.markOrderAsCompleted(testOrder.getId());

    // Assert
    assertNotNull(result);
    assertEquals(OrderStatus.COMPLETED, result.getStatus());
    verify(orderRepository).save(testOrder);
  }

  @Test
  void cancelOrder_WithValidOrder_ShouldUpdateStatusAndReleaseStock() {
    // Arrange
    testOrder.setStatus(OrderStatus.PENDING);
    OrderItem orderItem = new OrderItem(testOrder, testProduct, testStock, testProduct.getPrice());
    testOrder.addOrderItem(orderItem);

    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Act
    Order result = orderService.cancelOrder(testOrder.getId(), "User cancelled");

    // Assert
    assertNotNull(result);
    assertEquals(OrderStatus.CANCELLED, result.getStatus());
    verify(orderRepository).save(testOrder);
  }

  @Test
  void cancelOrderByUser_WithValidUserAndOrder_ShouldCancelOrder() {
    // Arrange
    testOrder.setStatus(OrderStatus.PENDING);
    testOrder.setUser(testUser);

    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Act
    Order result = orderService.cancelOrderByUser(testOrder.getId(), testUser, "User cancelled");

    // Assert
    assertNotNull(result);
    assertEquals(OrderStatus.CANCELLED, result.getStatus());
    verify(orderRepository).save(testOrder);
  }

  @Test
  void cancelOrderByUser_WithUnauthorizedUser_ShouldThrowException() {
    // Arrange
    User otherUser = new User();
    otherUser.setId(UUID.randomUUID());
    otherUser.setUsername("otheruser");

    testOrder.setStatus(OrderStatus.PENDING);
    testOrder.setUser(testUser);

    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

    // Act & Assert
    assertThrows(
        UnauthorizedException.class,
        () -> orderService.cancelOrderByUser(testOrder.getId(), otherUser, "User cancelled"));
  }

  // ==================== ORDER VALIDATION TESTS ====================

  @Test
  void validateOrder_WithValidInput_ShouldNotThrowException() {
    // Arrange
    Map<UUID, Integer> productQuantities = Map.of(testProduct.getId(), 1);

    when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    when(stockRepository.countAvailableByProductId(testProduct.getId())).thenReturn(5L);

    // Act & Assert
    assertDoesNotThrow(() -> orderService.validateOrder(testUser, productQuantities));
  }

  @Test
  void canUserCreateOrders_WithValidUser_ShouldReturnTrue() {
    // Act
    boolean result = orderService.canUserCreateOrders(testUser);

    // Assert
    assertTrue(result);
  }

  @Test
  void canUserCreateOrders_WithNullUser_ShouldReturnFalse() {
    // Act
    boolean result = orderService.canUserCreateOrders(null);

    // Assert
    assertFalse(result);
  }

  @Test
  void canOrderBeCancelled_WithCancellableOrder_ShouldReturnTrue() {
    // Arrange
    testOrder.setStatus(OrderStatus.PENDING);
    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

    // Act
    boolean result = orderService.canOrderBeCancelled(testOrder.getId());

    // Assert
    assertTrue(result);
  }

  @Test
  void doesOrderBelongToUser_WithCorrectUser_ShouldReturnTrue() {
    // Arrange
    testOrder.setUser(testUser);
    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

    // Act
    boolean result = orderService.doesOrderBelongToUser(testOrder.getId(), testUser);

    // Assert
    assertTrue(result);
  }

  // ==================== ORDER BUSINESS LOGIC TESTS ====================

  @Test
  void calculateOrderTotal_WithValidProducts_ShouldReturnCorrectTotal() {
    // Arrange
    Map<UUID, Integer> productQuantities = Map.of(testProduct.getId(), 2);

    when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));

    // Act
    BigDecimal result = orderService.calculateOrderTotal(productQuantities);

    // Assert
    assertEquals(BigDecimal.valueOf(200.00), result);
  }

  @Test
  void getAvailableQuantityForProduct_WithValidProduct_ShouldReturnQuantity() {
    // Arrange
    when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    when(stockRepository.countAvailableByProductId(testProduct.getId())).thenReturn(5L);

    // Act
    int result = orderService.getAvailableQuantityForProduct(testProduct.getId());

    // Assert
    assertEquals(5, result);
  }

  @Test
  void reserveStockForOrder_WithValidInput_ShouldReserveStock() {
    // Arrange
    Map<UUID, Integer> productQuantities = Map.of(testProduct.getId(), 1);
    List<Stock> reservedStocks = List.of(testStock);

    when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    when(stockService.reserveStock(testProduct.getId(), 1, 30)).thenReturn(reservedStocks);

    // Act
    assertDoesNotThrow(() -> orderService.reserveStockForOrder(testOrder, productQuantities));

    // Assert
    verify(stockService).reserveStock(testProduct.getId(), 1, 30);
  }

  // ==================== ORDER SEARCH AND REPORTING TESTS ====================

  @Test
  void searchOrders_WithCriteria_ShouldReturnMatchingOrders() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    List<Order> orders = List.of(testOrder);
    Page<Order> orderPage = new PageImpl<>(orders, pageable, 1);

    when(orderRepository.searchOrders(
            anyString(),
            anyString(),
            any(OrderStatus.class),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            eq(pageable)))
        .thenReturn(orderPage);

    // Act
    Page<Order> result =
        orderService.searchOrders(
            "ORD",
            "testuser",
            OrderStatus.PENDING,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now(),
            BigDecimal.ZERO,
            BigDecimal.valueOf(1000),
            pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
  }

  @Test
  void getOrderStatistics_ShouldReturnStatistics() {
    // Arrange
    Object[] stats = {10L, 3L, 5L, 2L}; // total, pending, completed, failed
    when(orderRepository.getOrderStatistics()).thenReturn(stats);
    when(orderRepository.countByStatus(OrderStatus.PROCESSING)).thenReturn(1L);
    when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(1L);
    when(orderRepository.calculateTotalRevenue()).thenReturn(BigDecimal.valueOf(1000));
    when(orderRepository.calculateAverageOrderValue()).thenReturn(BigDecimal.valueOf(100));

    // Act
    OrderStatistics result = orderService.getOrderStatistics();

    // Assert
    assertNotNull(result);
    assertEquals(10L, result.total());
    assertEquals(3L, result.pending());
    assertEquals(5L, result.completed());
    assertEquals(2L, result.failed());
  }

  @Test
  void getTopCustomersByOrderCount_ShouldReturnTopCustomers() {
    // Arrange
    List<User> topCustomers = List.of(testUser);
    when(orderRepository.findTopCustomersByOrderCount(5)).thenReturn(topCustomers);

    // Act
    List<User> result = orderService.getTopCustomersByOrderCount(5);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testUser, result.get(0));
  }

  @Test
  void getRecentOrders_ShouldReturnRecentOrders() {
    // Arrange
    List<Order> recentOrders = List.of(testOrder);
    when(orderRepository.findRecentOrders(10)).thenReturn(recentOrders);

    // Act
    List<Order> result = orderService.getRecentOrders(10);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testOrder, result.get(0));
  }

  // ==================== ORDER INTEGRATION TESTS ====================

  @Test
  void processOrderCompletion_WithValidOrder_ShouldCompleteOrder() {
    // Arrange
    testOrder.setStatus(OrderStatus.PROCESSING);
    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Act
    assertDoesNotThrow(() -> orderService.processOrderCompletion(testOrder.getId(), "TXN-123"));

    // Assert
    verify(orderRepository).save(testOrder);
  }

  @Test
  void processOrderFailure_WithValidOrder_ShouldFailOrder() {
    // Arrange
    testOrder.setStatus(OrderStatus.PROCESSING);
    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Act
    assertDoesNotThrow(() -> orderService.processOrderFailure(testOrder.getId(), "Payment failed"));

    // Assert
    verify(orderRepository).save(testOrder);
  }

  @Test
  void getOrderDownloadInfo_WithCompletedOrder_ShouldReturnDownloadInfo() {
    // Arrange
    testOrder.setStatus(OrderStatus.COMPLETED);
    testOrder.setUser(testUser);
    OrderItem orderItem = new OrderItem(testOrder, testProduct, testStock, testProduct.getPrice());
    testOrder.addOrderItem(orderItem);

    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

    // Act
    Map<String, String> result = orderService.getOrderDownloadInfo(testOrder.getId(), testUser);

    // Assert
    assertNotNull(result);
    assertTrue(result.containsKey(testProduct.getName()));
    assertEquals(testStock.getCredentials(), result.get(testProduct.getName()));
  }

  @Test
  void getOrderDownloadInfo_WithUnauthorizedUser_ShouldThrowException() {
    // Arrange
    User otherUser = new User();
    otherUser.setId(UUID.randomUUID());

    testOrder.setStatus(OrderStatus.COMPLETED);
    testOrder.setUser(testUser);

    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

    // Act & Assert
    assertThrows(
        UnauthorizedException.class,
        () -> orderService.getOrderDownloadInfo(testOrder.getId(), otherUser));
  }

  @Test
  void getOrderDownloadInfo_WithIncompleteOrder_ShouldThrowException() {
    // Arrange
    testOrder.setStatus(OrderStatus.PENDING);
    testOrder.setUser(testUser);

    when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

    // Act & Assert
    assertThrows(
        InvalidOrderStatusException.class,
        () -> orderService.getOrderDownloadInfo(testOrder.getId(), testUser));
  }
}
