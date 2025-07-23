package com.accountselling.platform.service.impl;

import com.accountselling.platform.dto.statistics.OrderStatistics;
import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.exception.*;
import com.accountselling.platform.model.*;
import com.accountselling.platform.repository.*;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;

/**
 * Unit tests for OrderServiceImpl.
 * Tests comprehensive order management operations including creation,
 * status management, payment integration, and business logic operations
 * with mock dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Implementation Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderItemRepository orderItemRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private StockRepository stockRepository;
    
    @Mock
    private StockService stockService;
    
    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Product testProduct;
    private Category testCategory;
    private Stock testStock;
    private Order testOrder;
    private OrderItem testOrderItem;
    private UUID testUserId;
    private UUID testProductId;
    private UUID testOrderId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testProductId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();

        // Setup test category
        testCategory = new Category();
        testCategory.setId(UUID.randomUUID());
        testCategory.setName("Test Category");

        // Setup test user
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        // Setup test product
        testProduct = new Product();
        testProduct.setId(testProductId);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(BigDecimal.valueOf(100.00));
        testProduct.setCategory(testCategory);
        testProduct.setActive(true);

        // Setup test stock
        testStock = new Stock();
        testStock.setId(UUID.randomUUID());
        testStock.setProduct(testProduct);
        testStock.setCredentials("test:credentials");
        testStock.setSold(false);

        // Setup test order
        testOrder = new Order();
        testOrder.setId(testOrderId);
        testOrder.setUser(testUser);
        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setOrderNumber("ORD-12345");

        // Setup test order item
        testOrderItem = new OrderItem();
        testOrderItem.setId(UUID.randomUUID());
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProduct(testProduct);
        testOrderItem.setStockItem(testStock);
        testOrderItem.setPrice(BigDecimal.valueOf(100.00));
        testOrderItem.setProductName("Test Product");

        testOrder.getOrderItems().add(testOrderItem);
    }

    @Nested
    @DisplayName("Order Creation Tests")
    class OrderCreationTests {

        @Test
        @DisplayName("Create order successfully with valid data")
        void createOrder_WithValidData_ShouldCreateOrderSuccessfully() {
            // Given
            Map<UUID, Integer> productQuantities = Map.of(testProductId, 1);
            List<Stock> reservedStocks = List.of(testStock);

            when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
            when(stockRepository.countAvailableByProductId(testProductId)).thenReturn(5L);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(stockService.reserveStock(testProductId, 1, 30)).thenReturn(reservedStocks);

            // When
            Order result = orderService.createOrder(testUser, productQuantities);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(200.00));
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);

            verify(productRepository, atLeastOnce()).findById(testProductId);
            verify(stockRepository).countAvailableByProductId(testProductId);
            verify(orderRepository).save(any(Order.class));
            verify(stockService).reserveStock(testProductId, 1, 30);
        }

        @Test
        @DisplayName("Create order with single product")
        void createOrder_WithSingleProduct_ShouldCreateOrderSuccessfully() {
            // Given
            List<Stock> reservedStocks = List.of(testStock);

            when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
            when(stockRepository.countAvailableByProductId(testProductId)).thenReturn(5L);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(stockService.reserveStock(testProductId, 2, 30)).thenReturn(reservedStocks);

            // When
            Order result = orderService.createOrder(testUser, testProductId, 2);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);

            verify(productRepository, atLeastOnce()).findById(testProductId);
            verify(stockService).reserveStock(testProductId, 2, 30);
        }

        @Test
        @DisplayName("Create order by username")
        void createOrderByUsername_WithValidUsername_ShouldCreateOrderSuccessfully() {
            // Given
            Map<UUID, Integer> productQuantities = Map.of(testProductId, 1);
            List<Stock> reservedStocks = List.of(testStock);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
            when(stockRepository.countAvailableByProductId(testProductId)).thenReturn(5L);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(stockService.reserveStock(testProductId, 1, 30)).thenReturn(reservedStocks);

            // When
            Order result = orderService.createOrderByUsername("testuser", productQuantities);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);

            verify(userRepository).findByUsername("testuser");
            verify(productRepository, atLeastOnce()).findById(testProductId);
        }

        @Test
        @DisplayName("Create order with non-existent user should throw exception")
        void createOrderByUsername_WithNonExistentUser_ShouldThrowException() {
            // Given
            Map<UUID, Integer> productQuantities = Map.of(testProductId, 1);

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUsername("nonexistent", productQuantities))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with username: nonexistent");

            verify(userRepository).findByUsername("nonexistent");
            verifyNoInteractions(productRepository, orderRepository);
        }

        @Test
        @DisplayName("Create order with insufficient stock should throw exception")
        void createOrder_WithInsufficientStock_ShouldThrowException() {
            // Given
            Map<UUID, Integer> productQuantities = Map.of(testProductId, 10);

            when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
            when(stockRepository.countAvailableByProductId(testProductId)).thenReturn(5L);

            // When & Then
            assertThatThrownBy(() -> orderService.createOrder(testUser, productQuantities))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for product");

            verify(productRepository, atLeastOnce()).findById(testProductId);
            verify(stockRepository).countAvailableByProductId(testProductId);
            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("Create order with non-existent product should throw exception")
        void createOrder_WithNonExistentProduct_ShouldThrowException() {
            // Given
            Map<UUID, Integer> productQuantities = Map.of(testProductId, 1);

            when(productRepository.findById(testProductId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.createOrder(testUser, productQuantities))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with ID");

            verify(productRepository, atLeastOnce()).findById(testProductId);
            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("Create order with invalid quantity should throw exception")
        void createOrder_WithInvalidQuantity_ShouldThrowException() {
            // Given
            Map<UUID, Integer> productQuantities = Map.of(testProductId, 0);

            // When & Then
            assertThatThrownBy(() -> orderService.createOrder(testUser, productQuantities))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be positive");

            verifyNoInteractions(productRepository, orderRepository);
        }
    }

    @Nested
    @DisplayName("Order Retrieval Tests")
    class OrderRetrievalTests {

        @Test
        @DisplayName("Find order by ID successfully")
        void findById_WithValidId_ShouldReturnOrder() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

            // When
            Order result = orderService.findById(testOrderId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testOrderId);

            verify(orderRepository, atLeastOnce()).findById(testOrderId);
        }

        @Test
        @DisplayName("Find order by non-existent ID should throw exception")
        void findById_WithNonExistentId_ShouldThrowException() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.findById(testOrderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with ID");

            verify(orderRepository, atLeastOnce()).findById(testOrderId);
        }

        @Test
        @DisplayName("Find order by order number successfully")
        void findByOrderNumber_WithValidNumber_ShouldReturnOrder() {
            // Given
            when(orderRepository.findByOrderNumber("ORD-12345")).thenReturn(Optional.of(testOrder));

            // When
            Order result = orderService.findByOrderNumber("ORD-12345");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("ORD-12345");

            verify(orderRepository).findByOrderNumber("ORD-12345");
        }

        @Test
        @DisplayName("Get orders by user with pagination")
        void getOrdersByUser_WithValidUser_ShouldReturnPagedOrders() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Order> orders = List.of(testOrder);
            Page<Order> page = new PageImpl<>(orders, pageable, 1);

            when(orderRepository.findByUser(testUser, pageable)).thenReturn(page);

            // When
            Page<Order> result = orderService.getOrdersByUser(testUser, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testOrder);

            verify(orderRepository).findByUser(testUser, pageable);
        }

        @Test
        @DisplayName("Get orders by username with pagination")
        void getOrdersByUsername_WithValidUsername_ShouldReturnPagedOrders() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Order> orders = List.of(testOrder);
            Page<Order> page = new PageImpl<>(orders, pageable, 1);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(orderRepository.findByUser(testUser, pageable)).thenReturn(page);

            // When
            Page<Order> result = orderService.getOrdersByUsername("testuser", pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(userRepository).findByUsername("testuser");
            verify(orderRepository).findByUser(testUser, pageable);
        }
    }

    @Nested
    @DisplayName("Order Status Management Tests")
    class OrderStatusManagementTests {

        @Test
        @DisplayName("Mark order as processing successfully")
        void markOrderAsProcessing_WithValidOrder_ShouldUpdateStatus() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.markOrderAsProcessing(testOrderId);

            // Then
            assertThat(result).isNotNull();
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Mark order as completed successfully")
        void markOrderAsCompleted_WithValidOrder_ShouldUpdateStatusAndMarkStockSold() {
            // Given
            testOrder.setStatus(OrderStatus.PROCESSING);
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.markOrderAsCompleted(testOrderId);

            // Then
            assertThat(result).isNotNull();
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Mark order as failed successfully")
        void markOrderAsFailed_WithValidOrder_ShouldUpdateStatusAndReleaseStock() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.markOrderAsFailed(testOrderId, "Payment failed");

            // Then
            assertThat(result).isNotNull();
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Cancel order successfully")
        void cancelOrder_WithValidOrder_ShouldUpdateStatusAndReleaseStock() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.cancelOrder(testOrderId, "User cancelled");

            // Then
            assertThat(result).isNotNull();
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Cancel order by user successfully")
        void cancelOrderByUser_WithValidUserAndOrder_ShouldCancelOrder() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.cancelOrderByUser(testOrderId, testUser, "Changed mind");

            // Then
            assertThat(result).isNotNull();
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Cancel order by unauthorized user should throw exception")
        void cancelOrderByUser_WithUnauthorizedUser_ShouldThrowException() {
            // Given
            User otherUser = new User();
            otherUser.setId(UUID.randomUUID());
            otherUser.setUsername("otheruser");

            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

            // When & Then
            assertThatThrownBy(() -> orderService.cancelOrderByUser(testOrderId, otherUser, "Reason"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User does not have permission to cancel this order");

            verify(orderRepository, atLeastOnce()).findById(testOrderId);
            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("Order Validation Tests")
    class OrderValidationTests {

        @Test
        @DisplayName("Validate order with valid data should pass")
        void validateOrder_WithValidData_ShouldPass() {
            // Given
            Map<UUID, Integer> productQuantities = Map.of(testProductId, 2);

            when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
            when(stockRepository.countAvailableByProductId(testProductId)).thenReturn(5L);

            // When & Then
            assertThatCode(() -> orderService.validateOrder(testUser, productQuantities))
                .doesNotThrowAnyException();

            verify(productRepository, atLeastOnce()).findById(testProductId);
            verify(stockRepository).countAvailableByProductId(testProductId);
        }

        @Test
        @DisplayName("Validate order with insufficient stock should throw exception")
        void validateOrder_WithInsufficientStock_ShouldThrowException() {
            // Given
            Map<UUID, Integer> productQuantities = Map.of(testProductId, 10);

            when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
            when(stockRepository.countAvailableByProductId(testProductId)).thenReturn(5L);

            // When & Then
            assertThatThrownBy(() -> orderService.validateOrder(testUser, productQuantities))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for product");
        }

        @Test
        @DisplayName("Check if user can create orders")
        void canUserCreateOrders_WithValidUser_ShouldReturnTrue() {
            // When
            boolean result = orderService.canUserCreateOrders(testUser);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Check if null user can create orders")
        void canUserCreateOrders_WithNullUser_ShouldReturnFalse() {
            // When
            boolean result = orderService.canUserCreateOrders(null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Check if order can be cancelled")
        void canOrderBeCancelled_WithPendingOrder_ShouldReturnTrue() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

            // When
            boolean result = orderService.canOrderBeCancelled(testOrderId);

            // Then
            assertThat(result).isTrue();
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
        }

        @Test
        @DisplayName("Check if order belongs to user")
        void doesOrderBelongToUser_WithCorrectUser_ShouldReturnTrue() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

            // When
            boolean result = orderService.doesOrderBelongToUser(testOrderId, testUser);

            // Then
            assertThat(result).isTrue();
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
        }
    }

    @Nested
    @DisplayName("Order Business Logic Tests")
    class OrderBusinessLogicTests {

        @Test
        @DisplayName("Calculate order total successfully")
        void calculateOrderTotal_WithValidProducts_ShouldReturnCorrectTotal() {
            // Given
            Map<UUID, Integer> productQuantities = Map.of(testProductId, 2);
            BigDecimal expectedTotal = BigDecimal.valueOf(200.00);

            when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

            // When
            BigDecimal result = orderService.calculateOrderTotal(productQuantities);

            // Then
            assertThat(result).isEqualByComparingTo(expectedTotal);
            verify(productRepository, atLeastOnce()).findById(testProductId);
        }

        @Test
        @DisplayName("Get available quantity for product")
        void getAvailableQuantityForProduct_WithValidProduct_ShouldReturnCount() {
            // Given
            when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
            when(stockRepository.countAvailableByProductId(testProductId)).thenReturn(10L);

            // When
            int result = orderService.getAvailableQuantityForProduct(testProductId);

            // Then
            assertThat(result).isEqualTo(10);
            verify(productRepository, atLeastOnce()).findById(testProductId);
            verify(stockRepository).countAvailableByProductId(testProductId);
        }

        @Test
        @DisplayName("Reserve stock for order successfully")
        void reserveStockForOrder_WithValidData_ShouldReserveStock() {
            // Given
            Map<UUID, Integer> productQuantities = Map.of(testProductId, 1);
            List<Stock> reservedStocks = List.of(testStock);

            when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
            when(stockService.reserveStock(testProductId, 1, 30)).thenReturn(reservedStocks);

            // When
            orderService.reserveStockForOrder(testOrder, productQuantities);

            // Then
            verify(productRepository, atLeastOnce()).findById(testProductId);
            verify(stockService).reserveStock(testProductId, 1, 30);
        }

        @Test
        @DisplayName("Release stock reservations")
        void releaseStockReservations_WithValidOrder_ShouldReleaseReservations() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

            // When
            orderService.releaseStockReservations(testOrderId);

            // Then
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
        }

        @Test
        @DisplayName("Mark stock items as sold")
        void markStockItemsAsSold_WithValidOrder_ShouldMarkAsSold() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

            // When
            orderService.markStockItemsAsSold(testOrderId);

            // Then
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
        }
    }

    @Nested
    @DisplayName("Order Search and Reporting Tests")
    class OrderSearchAndReportingTests {

        @Test
        @DisplayName("Search orders with criteria")
        void searchOrders_WithCriteria_ShouldReturnFilteredOrders() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Order> orders = List.of(testOrder);
            Page<Order> page = new PageImpl<>(orders, pageable, 1);

            when(orderRepository.searchOrders(any(), any(), any(),
                any(), any(), any(),
                any(), eq(pageable))).thenReturn(page);

            // When
            Page<Order> result = orderService.searchOrders("ORD", "testuser", OrderStatus.PENDING,
                null, null, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(orderRepository).searchOrders(any(), any(), any(),
                any(), any(), any(),
                any(), eq(pageable));
        }

        @Test
        @DisplayName("Get order statistics")
        void getOrderStatistics_ShouldReturnStatistics() {
            // Given
            Object[] stats = {10L, 5L, 3L, 2L}; // total, pending, completed, failed
            when(orderRepository.getOrderStatistics()).thenReturn(stats);
            when(orderRepository.countByStatus(OrderStatus.PROCESSING)).thenReturn(1L);
            when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(1L);
            when(orderRepository.calculateTotalRevenue()).thenReturn(BigDecimal.valueOf(1000.00));
            when(orderRepository.calculateAverageOrderValue()).thenReturn(BigDecimal.valueOf(100.00));

            // When
            OrderStatistics result = orderService.getOrderStatistics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(10L);
            assertThat(result.pending()).isEqualTo(5L);
            assertThat(result.completed()).isEqualTo(3L);
            assertThat(result.failed()).isEqualTo(2L);

            verify(orderRepository).getOrderStatistics();
            verify(orderRepository).countByStatus(OrderStatus.PROCESSING);
            verify(orderRepository).countByStatus(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("Get recent orders")
        void getRecentOrders_WithLimit_ShouldReturnRecentOrders() {
            // Given
            List<Order> orders = List.of(testOrder);
            when(orderRepository.findRecentOrders(5)).thenReturn(orders);

            // When
            List<Order> result = orderService.getRecentOrders(5);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testOrder);

            verify(orderRepository).findRecentOrders(5);
        }
    }

    @Nested
    @DisplayName("Order Integration Tests")
    class OrderIntegrationTests {

        @Test
        @DisplayName("Process order completion")
        void processOrderCompletion_WithValidData_ShouldCompleteOrder() {
            // Given
            testOrder.setStatus(OrderStatus.PROCESSING);
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            orderService.processOrderCompletion(testOrderId, "TXN-12345");

            // Then
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Process order failure")
        void processOrderFailure_WithValidData_ShouldFailOrder() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            orderService.processOrderFailure(testOrderId, "Payment failed");

            // Then
            verify(orderRepository, atLeastOnce()).findById(testOrderId);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Get order download info successfully")
        void getOrderDownloadInfo_WithCompletedOrder_ShouldReturnDownloadInfo() {
            // Given
            testOrder.setStatus(OrderStatus.COMPLETED);
            testStock.setCredentials("username:password");
            
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

            // When
            Map<String, String> result = orderService.getOrderDownloadInfo(testOrderId, testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).containsKey("Test Product");
            assertThat(result.get("Test Product")).isEqualTo("username:password");

            verify(orderRepository, atLeastOnce()).findById(testOrderId);
        }

        @Test
        @DisplayName("Get order download info with unauthorized user should throw exception")
        void getOrderDownloadInfo_WithUnauthorizedUser_ShouldThrowException() {
            // Given
            User otherUser = new User();
            otherUser.setId(UUID.randomUUID());
            otherUser.setUsername("otheruser");

            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

            // When & Then
            assertThatThrownBy(() -> orderService.getOrderDownloadInfo(testOrderId, otherUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User does not have permission to access this order");

            verify(orderRepository, atLeastOnce()).findById(testOrderId);
        }

        @Test
        @DisplayName("Get order download info with incomplete order should throw exception")
        void getOrderDownloadInfo_WithIncompleteOrder_ShouldThrowException() {
            // Given
            testOrder.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

            // When & Then
            assertThatThrownBy(() -> orderService.getOrderDownloadInfo(testOrderId, testUser))
                .isInstanceOf(InvalidOrderStatusException.class)
                .hasMessageContaining("Order must be completed to download information");

            verify(orderRepository, atLeastOnce()).findById(testOrderId);
        }
    }
}