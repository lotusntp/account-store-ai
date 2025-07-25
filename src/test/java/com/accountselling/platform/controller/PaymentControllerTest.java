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

import com.accountselling.platform.dto.payment.PaymentCreateRequestDto;
import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.enums.PaymentStatus;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.exception.WebhookProcessingException;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.Payment;
import com.accountselling.platform.model.User;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.PaymentService;
import com.accountselling.platform.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for PaymentController. Tests controller logic with mocked service dependencies.
 * Uses @WebMvcTest for focused controller testing with security disabled for unit tests.
 *
 * <p>Unit tests สำหรับ PaymentController ทดสอบ logic ของ controller ด้วย service dependencies
 * ที่ถูก mock ใช้ @WebMvcTest สำหรับการทดสอบ controller อย่างเจาะจงโดยปิด security สำหรับ unit
 * tests
 */
@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private PaymentService paymentService;

  @MockBean private OrderService orderService;

  @MockBean private UserService userService;

  private User testUser;
  private Order testOrder;
  private Payment testPayment;
  private PaymentCreateRequestDto paymentRequest;

  @BeforeEach
  void setUp() {
    // Setup test user
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");

    // Setup test order
    testOrder = new Order();
    testOrder.setId(UUID.randomUUID());
    testOrder.setOrderNumber("ORD-2024-001");
    testOrder.setUser(testUser);
    testOrder.setTotalAmount(new BigDecimal("99.99"));
    testOrder.setStatus(OrderStatus.PENDING);
    testOrder.setCreatedAt(LocalDateTime.now());

    // Setup test payment
    testPayment = new Payment();
    testPayment.setId(UUID.randomUUID());
    testPayment.setPaymentReference("PAY-2024-001");
    testPayment.setTransactionId("TXN-12345678");
    testPayment.setOrder(testOrder);
    testPayment.setAmount(new BigDecimal("99.99"));
    testPayment.setStatus(PaymentStatus.PENDING);
    testPayment.setPaymentMethod("QRCODE");
    testPayment.setQrCodeUrl("https://qr.example.com/pay-12345");
    testPayment.setExpiresAt(LocalDateTime.now().plusMinutes(30));
    testPayment.setCreatedAt(LocalDateTime.now());
    testPayment.setUpdatedAt(LocalDateTime.now());

    // Setup payment request
    paymentRequest = new PaymentCreateRequestDto(testOrder.getId(), "QRCODE", 30, "Test payment");
  }

  @Test
  @DisplayName("Generate Payment - Success")
  void generatePayment_Success() throws Exception {
    // Arrange
    when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(orderService.findById(testOrder.getId())).thenReturn(testOrder);
    when(paymentService.createPayment(any(Order.class), eq("QRCODE"), eq(30)))
        .thenReturn(testPayment);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/generate")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(testPayment.getId().toString()))
        .andExpect(jsonPath("$.paymentReference").value("PAY-2024-001"))
        .andExpect(jsonPath("$.transactionId").value("TXN-12345678"))
        .andExpect(jsonPath("$.amount").value(99.99))
        .andExpect(jsonPath("$.formattedAmount").value("$99.99"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.paymentMethod").value("QRCODE"))
        .andExpect(jsonPath("$.qrCodeUrl").value("https://qr.example.com/pay-12345"))
        .andExpect(jsonPath("$.order.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.order.orderNumber").value("ORD-2024-001"))
        .andExpect(jsonPath("$.order.username").value("testuser"));
  }

  @Test
  @DisplayName("Generate Payment - Order Not Found")
  void generatePayment_OrderNotFound() throws Exception {
    // Arrange
    when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(orderService.findById(testOrder.getId()))
        .thenThrow(new ResourceNotFoundException("Order not found"));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/generate")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Generate Payment - Order Belongs to Different User")
  void generatePayment_OrderBelongsToDifferentUser() throws Exception {
    // Arrange
    User differentUser = new User();
    differentUser.setId(UUID.randomUUID());
    differentUser.setUsername("differentuser");

    Order differentUserOrder = new Order();
    differentUserOrder.setId(testOrder.getId());
    differentUserOrder.setUser(differentUser);

    when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(orderService.findById(testOrder.getId())).thenReturn(differentUserOrder);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/generate")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(
                    containsString("Cannot create payment for order that doesn't belong to you")));
  }

  @Test
  @DisplayName("Generate Payment - Invalid Request Data")
  void generatePayment_InvalidRequestData() throws Exception {
    // Arrange - Invalid request with null order ID
    PaymentCreateRequestDto invalidRequest =
        new PaymentCreateRequestDto(null, "QRCODE", 30, "Test payment");

    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Generate Payment - Unauthorized")
  void generatePayment_Unauthorized() throws Exception {
    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/generate")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Get Payment Status - Success")
  void getPaymentStatus_Success() throws Exception {
    // Arrange
    when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(paymentService.findById(testPayment.getId())).thenReturn(testPayment);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/payments/status/{paymentId}", testPayment.getId())
                .with(user("testuser").roles("USER")))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testPayment.getId().toString()))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.paymentMethod").value("QRCODE"))
        .andExpect(jsonPath("$.qrCodeUrl").value("https://qr.example.com/pay-12345"));
  }

  @Test
  @DisplayName("Get Payment Status - Payment Not Found")
  void getPaymentStatus_PaymentNotFound() throws Exception {
    // Arrange
    UUID nonExistentPaymentId = UUID.randomUUID();
    when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(paymentService.findById(nonExistentPaymentId))
        .thenThrow(new ResourceNotFoundException("Payment not found"));

    // Act & Assert
    mockMvc
        .perform(get("/api/payments/status/{paymentId}", nonExistentPaymentId))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Get Payment Status - Payment Belongs to Different User")
  void getPaymentStatus_PaymentBelongsToDifferentUser() throws Exception {
    // Arrange
    User differentUser = new User();
    differentUser.setId(UUID.randomUUID());
    differentUser.setUsername("differentuser");

    Order differentUserOrder = new Order();
    differentUserOrder.setUser(differentUser);

    Payment differentUserPayment = new Payment();
    differentUserPayment.setId(testPayment.getId());
    differentUserPayment.setOrder(differentUserOrder);

    when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(paymentService.findById(testPayment.getId())).thenReturn(differentUserPayment);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/payments/status/{paymentId}", testPayment.getId())
                .with(user("testuser").roles("USER")))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(containsString("Cannot access payment that doesn't belong to you")));
  }

  @Test
  @DisplayName("Get Payment Status - Unauthorized")
  void getPaymentStatus_Unauthorized() throws Exception {
    // Act & Assert
    mockMvc
        .perform(
            get("/api/payments/status/{paymentId}", testPayment.getId())
                .with(user("testuser").roles("USER")))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Process Webhook - Success")
  void processWebhook_Success() throws Exception {
    // Arrange
    Map<String, Object> webhookData = new HashMap<>();
    webhookData.put("transaction_id", "TXN-12345678");
    webhookData.put("status", "completed");
    webhookData.put("amount", "99.99");
    webhookData.put("currency", "USD");

    Payment completedPayment = new Payment();
    completedPayment.setId(testPayment.getId());
    completedPayment.setTransactionId("TXN-12345678");
    completedPayment.setStatus(PaymentStatus.COMPLETED);
    completedPayment.setPaidAt(LocalDateTime.now());

    when(paymentService.processWebhook(eq("TXN-12345678"), eq("completed"), any(String.class)))
        .thenReturn(completedPayment);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookData)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("Webhook processed successfully"))
        .andExpect(jsonPath("$.paymentId").value(completedPayment.getId().toString()))
        .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
  }

  @Test
  @DisplayName("Process Webhook - Missing Transaction ID")
  void processWebhook_MissingTransactionId() throws Exception {
    // Arrange
    Map<String, Object> webhookData = new HashMap<>();
    webhookData.put("status", "completed");
    webhookData.put("amount", "99.99");
    // Missing transaction_id

    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookData)))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value(containsString("Failed to process webhook")));
  }

  @Test
  @DisplayName("Process Webhook - Missing Status")
  void processWebhook_MissingStatus() throws Exception {
    // Arrange
    Map<String, Object> webhookData = new HashMap<>();
    webhookData.put("transaction_id", "TXN-12345678");
    webhookData.put("amount", "99.99");
    // Missing status

    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookData)))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value(containsString("Failed to process webhook")));
  }

  @Test
  @DisplayName("Process Webhook - Payment Not Found for Transaction")
  void processWebhook_PaymentNotFound() throws Exception {
    // Arrange
    Map<String, Object> webhookData = new HashMap<>();
    webhookData.put("transaction_id", "TXN-NONEXISTENT");
    webhookData.put("status", "completed");

    when(paymentService.processWebhook(eq("TXN-NONEXISTENT"), eq("completed"), any(String.class)))
        .thenThrow(new ResourceNotFoundException("Payment not found for transaction ID"));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookData)))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value(containsString("Failed to process webhook")));
  }

  @Test
  @DisplayName("Process Webhook - Webhook Processing Exception")
  void processWebhook_WebhookProcessingException() throws Exception {
    // Arrange
    Map<String, Object> webhookData = new HashMap<>();
    webhookData.put("transaction_id", "TXN-12345678");
    webhookData.put("status", "invalid_status");

    when(paymentService.processWebhook(eq("TXN-12345678"), eq("invalid_status"), any(String.class)))
        .thenThrow(new WebhookProcessingException("Invalid webhook status"));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookData)))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value(containsString("Failed to process webhook")));
  }

  @Test
  @DisplayName("Process Webhook - Empty Request Body")
  void processWebhook_EmptyRequestBody() throws Exception {
    // Act & Assert
    mockMvc
        .perform(
            post("/api/payments/webhook").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value("error"));
  }
}
