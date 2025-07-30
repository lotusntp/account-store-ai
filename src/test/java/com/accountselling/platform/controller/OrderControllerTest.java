package com.accountselling.platform.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.config.TestSecurityConfig;
import com.accountselling.platform.dto.order.OrderCreateRequestDto;
import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.exception.InsufficientStockException;
import com.accountselling.platform.exception.InvalidOrderStatusException;
import com.accountselling.platform.exception.OrderAccessDeniedException;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.OrderItem;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.model.User;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for OrderController. Tests controller logic with mocked service dependencies.
 * Uses @WebMvcTest for focused controller testing with security enabled.
 *
 * <p>Unit tests สำหรับ OrderController ทดสอบ logic ของ controller ด้วย service dependencies ที่ถูก
 * mock ใช้ @WebMvcTest สำหรับการทดสอบ controller อย่างเจาะจงพร้อม security
 */
@WebMvcTest(controllers = OrderController.class)
@Import(TestSecurityConfig.class)
class OrderControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private OrderService orderService;

  @MockBean private UserService userService;

  private User testUser;
  private Order testOrder;
  private Product testProduct;
  private Category testCategory;
  private OrderItem testOrderItem;
  private Stock testStock;
  private OrderCreateRequestDto orderRequest;

  @BeforeEach
  void setUp() {
    // Setup test user
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");

    // Setup test category
    testCategory = new Category();
    testCategory.setId(UUID.randomUUID());
    testCategory.setName("Gaming");
    testCategory.setDescription("Gaming accounts");

    // Setup test product
    testProduct = new Product();
    testProduct.setId(UUID.randomUUID());
    testProduct.setName("Premium Game Account");
    testProduct.setDescription("Level 80 Premium Account");
    testProduct.setPrice(new BigDecimal("99.99"));
    testProduct.setCategory(testCategory);
    testProduct.setActive(true);

    // Setup test stock
    testStock = new Stock();
    testStock.setId(UUID.randomUUID());
    testStock.setProduct(testProduct);
    testStock.setAccountData("username:password123");
    testStock.setReservedUntil(null);
    testStock.setSold(false);

    // Setup test order item
    testOrderItem = new OrderItem();
    testOrderItem.setId(UUID.randomUUID());
    testOrderItem.setProduct(testProduct);
    testOrderItem.setPrice(new BigDecimal("99.99"));
    testOrderItem.setStockItem(testStock);

    // Setup test order
    testOrder = new Order();
    testOrder.setId(UUID.randomUUID());
    testOrder.setOrderNumber("ORD-2024-001");
    testOrder.setUser(testUser);
    testOrder.setTotalAmount(new BigDecimal("99.99"));
    testOrder.setStatus(OrderStatus.PENDING);
    testOrder.setCreatedAt(LocalDateTime.now());
    testOrder.setUpdatedAt(LocalDateTime.now());

    // Add order item to order using helper method
    testOrder.addOrderItem(testOrderItem);

    // Setup order item back reference
    testOrderItem.setOrder(testOrder);

    // Setup order request
    Map<UUID, Integer> productQuantities = new HashMap<>();
    productQuantities.put(testProduct.getId(), 1);
    orderRequest = new OrderCreateRequestDto(productQuantities, "Test order");
  }

  @Test
  @DisplayName("Create Order - Success")
  void createOrder_Success() throws Exception {
    // Arrange
    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.createOrder(eq(testUser), any())).thenReturn(testOrder);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/orders")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.orderNumber").value("ORD-2024-001"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.totalAmount").value(99.99))
        .andExpect(jsonPath("$.formattedTotalAmount").value("$99.99"))
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.orderItems", hasSize(1)))
        .andExpect(jsonPath("$.orderItems[0].productName").value("Premium Game Account"))
        .andExpect(jsonPath("$.orderItems[0].categoryName").value("Gaming"))
        .andExpect(jsonPath("$.orderItems[0].price").value(99.99));
  }

  @Test
  @DisplayName("Create Order - Invalid Request Data")
  void createOrder_InvalidRequestData() throws Exception {
    // Arrange - Empty product quantities
    OrderCreateRequestDto invalidRequest = new OrderCreateRequestDto(new HashMap<>(), "Test order");

    // Act & Assert
    mockMvc
        .perform(
            post("/api/orders")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Create Order - Product Not Found")
  void createOrder_ProductNotFound() throws Exception {
    // Arrange
    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.createOrder(eq(testUser), any()))
        .thenThrow(new ResourceNotFoundException("Product not found"));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/orders")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Create Order - Insufficient Stock")
  void createOrder_InsufficientStock() throws Exception {
    // Arrange
    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.createOrder(eq(testUser), any()))
        .thenThrow(new InsufficientStockException("Insufficient stock available"));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/orders")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andDo(print())
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Create Order - Unauthorized")
  void createOrder_Unauthorized() throws Exception {
    // Act & Assert - No authentication provided
    mockMvc
        .perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andDo(print())
        .andExpect(status().isForbidden()); // Spring Security returns 403 when no auth provided
  }

  @Test
  @DisplayName("Get User Orders - Success")
  void getUserOrders_Success() throws Exception {
    // Arrange
    Page<Order> orderPage = new PageImpl<>(List.of(testOrder), PageRequest.of(0, 20), 1);
    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.getOrdersByUser(eq(testUser), any(Pageable.class))).thenReturn(orderPage);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/orders")
                .with(user("testuser").roles("USER"))
                .param("page", "0")
                .param("size", "20")
                .param("sort", "createdAt,desc"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-2024-001"))
        .andExpect(jsonPath("$.content[0].status").value("PENDING"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.number").value(0));
  }

  @Test
  @DisplayName("Get User Orders - Unauthorized")
  void getUserOrders_Unauthorized() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/orders"))
        .andDo(print())
        .andExpect(status().isForbidden()); // Spring Security returns 403 when no auth provided
  }

  @Test
  @DisplayName("Get Order By ID - Success")
  void getOrderById_Success() throws Exception {
    // Arrange
    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.findById(testOrder.getId())).thenReturn(testOrder);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/orders/{orderId}", testOrder.getId()).with(user("testuser").roles("USER")))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.orderNumber").value("ORD-2024-001"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.totalAmount").value(99.99))
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.orderItems", hasSize(1)));
  }

  @Test
  @DisplayName("Get Order By ID - Order Not Found")
  void getOrderById_OrderNotFound() throws Exception {
    // Arrange
    UUID nonExistentOrderId = UUID.randomUUID();
    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.findById(nonExistentOrderId))
        .thenThrow(new ResourceNotFoundException("Order not found"));

    // Act & Assert
    mockMvc
        .perform(
            get("/api/orders/{orderId}", nonExistentOrderId).with(user("testuser").roles("USER")))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Get Order By ID - Order Belongs to Different User")
  void getOrderById_OrderBelongsToDifferentUser() throws Exception {
    // Arrange
    User differentUser = new User();
    differentUser.setId(UUID.randomUUID());
    differentUser.setUsername("differentuser");

    Order differentUserOrder = new Order();
    differentUserOrder.setId(testOrder.getId());
    differentUserOrder.setUser(differentUser);

    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.findById(testOrder.getId())).thenReturn(differentUserOrder);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/orders/{orderId}", testOrder.getId()).with(user("testuser").roles("USER")))
        .andDo(print())
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(containsString("Access denied")));
  }

  @Test
  @DisplayName("Cancel Order - Success")
  void cancelOrder_Success() throws Exception {
    // Arrange
    Order cancelledOrder = new Order();
    cancelledOrder.setId(testOrder.getId());
    cancelledOrder.setUser(testUser);
    cancelledOrder.setStatus(OrderStatus.CANCELLED);
    cancelledOrder.setOrderNumber("ORD-2024-001");
    cancelledOrder.setTotalAmount(new BigDecimal("99.99")); // Fix: Set totalAmount to prevent NPE
    cancelledOrder.setCreatedAt(LocalDateTime.now());
    cancelledOrder.setUpdatedAt(LocalDateTime.now());
    // Set up order items for cancelled order
    cancelledOrder.addOrderItem(testOrderItem);

    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.cancelOrderByUser(eq(testOrder.getId()), eq(testUser), any(String.class)))
        .thenReturn(cancelledOrder);

    // Act & Assert
    mockMvc
        .perform(
            delete("/api/orders/{orderId}", testOrder.getId())
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .param("reason", "Changed mind"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  @DisplayName("Cancel Order - Order Cannot Be Cancelled")
  void cancelOrder_CannotBeCancelled() throws Exception {
    // Arrange
    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.cancelOrderByUser(eq(testOrder.getId()), eq(testUser), any(String.class)))
        .thenThrow(new InvalidOrderStatusException("Order cannot be cancelled"));

    // Act & Assert
    mockMvc
        .perform(
            delete("/api/orders/{orderId}", testOrder.getId())
                .with(user("testuser").roles("USER"))
                .with(csrf()))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Download Order Credentials - Success")
  void downloadOrderCredentials_Success() throws Exception {
    // Arrange
    Map<String, String> downloadInfo = new HashMap<>();
    downloadInfo.put("Premium Game Account", "username:password123");

    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.getOrderDownloadInfo(eq(testOrder.getId()), eq(testUser)))
        .thenReturn(downloadInfo);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/orders/{orderId}/download", testOrder.getId())
                .with(user("testuser").roles("USER")))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    "attachment; filename=\"accounts_order_" + testOrder.getId() + ".txt\""))
        .andExpect(content().string(containsString("=== ACCOUNT CREDENTIALS ===")))
        .andExpect(content().string(containsString("Order ID: " + testOrder.getId())))
        .andExpect(content().string(containsString("Customer: testuser")))
        .andExpect(content().string(containsString("=== Premium Game Account ===")))
        .andExpect(content().string(containsString("username:password123")))
        .andExpect(content().string(containsString("=== IMPORTANT NOTES ===")))
        .andExpect(content().string(containsString("Change passwords immediately")));
  }

  @Test
  @DisplayName("Download Order Credentials - Order Not Found")
  void downloadOrderCredentials_OrderNotFound() throws Exception {
    // Arrange
    UUID nonExistentOrderId = UUID.randomUUID();
    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.getOrderDownloadInfo(eq(nonExistentOrderId), eq(testUser)))
        .thenThrow(new ResourceNotFoundException("Order not found"));

    // Act & Assert
    mockMvc
        .perform(
            get("/api/orders/{orderId}/download", nonExistentOrderId)
                .with(user("testuser").roles("USER")))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Download Order Credentials - Order Belongs to Different User")
  void downloadOrderCredentials_OrderBelongsToDifferentUser() throws Exception {
    // Arrange
    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.getOrderDownloadInfo(eq(testOrder.getId()), eq(testUser)))
        .thenThrow(new OrderAccessDeniedException(testOrder.getId().toString(), "testuser"));

    // Act & Assert
    mockMvc
        .perform(
            get("/api/orders/{orderId}/download", testOrder.getId())
                .with(user("testuser").roles("USER")))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Download Order Credentials - Order Not Completed")
  void downloadOrderCredentials_OrderNotCompleted() throws Exception {
    // Arrange
    when(userService.findByUsername(any(String.class))).thenReturn(Optional.of(testUser));
    when(orderService.getOrderDownloadInfo(eq(testOrder.getId()), eq(testUser)))
        .thenThrow(new InvalidOrderStatusException("Order is not completed"));

    // Act & Assert
    mockMvc
        .perform(
            get("/api/orders/{orderId}/download", testOrder.getId())
                .with(user("testuser").roles("USER")))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Download Order Credentials - Unauthorized")
  void downloadOrderCredentials_Unauthorized() throws Exception {
    // Act & Assert - No authentication provided
    mockMvc
        .perform(get("/api/orders/{orderId}/download", testOrder.getId()))
        .andDo(print())
        .andExpect(status().isForbidden()); // Spring Security returns 403 when no auth provided
  }
}
