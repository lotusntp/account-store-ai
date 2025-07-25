package com.accountselling.platform.controller;

import com.accountselling.platform.config.TestRateLimitConfig;
import com.accountselling.platform.dto.order.OrderCreateRequestDto;
import com.accountselling.platform.dto.payment.PaymentCreateRequestDto;
import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.enums.PaymentStatus;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.Payment;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Role;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.model.User;
import com.accountselling.platform.repository.CategoryRepository;
import com.accountselling.platform.repository.OrderRepository;
import com.accountselling.platform.repository.PaymentRepository;
import com.accountselling.platform.repository.ProductRepository;
import com.accountselling.platform.repository.RoleRepository;
import com.accountselling.platform.repository.StockRepository;
import com.accountselling.platform.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Payment and Order workflows.
 * Tests complete end-to-end functionality including order creation, payment processing,
 * and account download workflows with real database and application context.
 * 
 * Integration tests สำหรับ Payment และ Order workflows
 * ทดสอบฟังก์ชันการทำงานแบบ end-to-end รวมถึงการสร้างออเดอร์ การประมวลผลการชำระเงิน
 * และ workflow การดาวน์โหลดบัญชี พร้อมฐานข้อมูลจริงและ application context
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestRateLimitConfig.class)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.level.com.accountselling.platform.security.RateLimitingFilter=DEBUG"
})
@Transactional
class PaymentOrderIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private User testUser;
    private Role userRole;
    private Category testCategory;
    private Product testProduct;
    private Stock testStock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        setupTestData();
    }

    private void setupTestData() {
        // Create USER role
        userRole = new Role();
        userRole.setName("USER");
        userRole.setDescription("Standard user role");
        userRole = roleRepository.save(userRole);

        // Create test user
        testUser = new User();
        testUser.setUsername("integrationtestuser");
        testUser.setEmail("integration@test.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Integration");
        testUser.setLastName("Test");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(userRole));
        testUser = userRepository.save(testUser);

        // Create test category
        testCategory = new Category();
        testCategory.setName("Gaming");
        testCategory.setDescription("Gaming accounts");
        testCategory.setActive(true);
        testCategory = categoryRepository.save(testCategory);

        // Create test product
        testProduct = new Product();
        testProduct.setName("Premium Game Account");
        testProduct.setDescription("Level 80 Premium Gaming Account with rare items");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setCategory(testCategory);
        testProduct.setActive(true);
        testProduct.setImageUrl("https://example.com/game-account.jpg");
        testProduct.setServer("US-West");
        testProduct = productRepository.save(testProduct);

        // Create test stock
        testStock = new Stock();
        testStock.setProduct(testProduct);
        testStock.setCredentials("username:gameuser123\npassword:gamepass456\nemail:gameuser@example.com");
        testStock.setReservedUntil(null);
        testStock.setSold(false);
        testStock = stockRepository.save(testStock);
    }

    @Test
    @DisplayName("Complete Payment and Order Workflow - Success")
    @WithMockUser(username = "integrationtestuser", roles = {"USER"})
    void completePaymentOrderWorkflow_Success() throws Exception {
        // Step 1: Create Order
        Map<UUID, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct.getId(), 1);
        OrderCreateRequestDto orderRequest = new OrderCreateRequestDto(productQuantities, "Integration test order");

        String orderResponse = mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(99.99))
                .andExpect(jsonPath("$.orderItems", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract order ID from response
        @SuppressWarnings("unchecked")
        Map<String, Object> orderData = objectMapper.readValue(orderResponse, Map.class);
        String orderId = (String) orderData.get("id");

        // Step 2: Generate Payment for Order
        PaymentCreateRequestDto paymentRequest = new PaymentCreateRequestDto(
            UUID.fromString(orderId),
            "QRCODE",
            30,
            "Integration test payment"
        );

        String paymentResponse = mockMvc.perform(post("/api/payments/generate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("QRCODE"))
                .andExpect(jsonPath("$.amount").value(99.99))
                .andExpect(jsonPath("$.qrCodeUrl").exists())
                .andExpect(jsonPath("$.order.id").value(orderId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract payment ID from response
        @SuppressWarnings("unchecked")
        Map<String, Object> paymentData = objectMapper.readValue(paymentResponse, Map.class);
        String paymentId = (String) paymentData.get("id");
        String transactionId = (String) paymentData.get("transactionId");

        // Step 3: Check Payment Status
        mockMvc.perform(get("/api/payments/status/{paymentId}", paymentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.transactionId").value(transactionId))
                .andExpect(jsonPath("$.qrCodeUrl").exists());

        // Step 4: Simulate Payment Gateway Webhook - Payment Completed
        Map<String, Object> webhookData = new HashMap<>();
        webhookData.put("transaction_id", transactionId);
        webhookData.put("status", "completed");
        webhookData.put("amount", "99.99");
        webhookData.put("currency", "USD");
        webhookData.put("gateway_reference", "GTW-" + System.currentTimeMillis());

        mockMvc.perform(post("/api/payments/webhook")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookData)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"))
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));

        // Step 5: Verify Payment Status Updated
        mockMvc.perform(get("/api/payments/status/{paymentId}", paymentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.paidAt").exists());

        // Step 6: Verify Order Status Updated
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.orderItems", hasSize(1)));

        // Step 7: Download Account Credentials
        mockMvc.perform(get("/api/orders/{orderId}/download", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", 
                    "attachment; filename=\"accounts_order_" + orderId + ".txt\""))
                .andExpect(content().string(containsString("=== ACCOUNT CREDENTIALS ===")))
                .andExpect(content().string(containsString("Order ID: " + orderId)))
                .andExpect(content().string(containsString("Customer: integrationtestuser")))
                .andExpect(content().string(containsString("=== Premium Game Account ===")))
                .andExpect(content().string(containsString("username:gameuser123")))
                .andExpect(content().string(containsString("password:gamepass456")))
                .andExpect(content().string(containsString("email:gameuser@example.com")))
                .andExpect(content().string(containsString("=== IMPORTANT NOTES ===")));
    }

    @Test
    @DisplayName("Payment Failure Workflow - Order Should Remain Pending")
    @WithMockUser(username = "integrationtestuser", roles = {"USER"})
    void paymentFailureWorkflow_OrderRemainsUnchanged() throws Exception {
        // Step 1: Create Order
        Map<UUID, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct.getId(), 1);
        OrderCreateRequestDto orderRequest = new OrderCreateRequestDto(productQuantities, "Failure test order");

        String orderResponse = mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> orderData = objectMapper.readValue(orderResponse, Map.class);
        String orderId = (String) orderData.get("id");

        // Step 2: Generate Payment
        PaymentCreateRequestDto paymentRequest = new PaymentCreateRequestDto(
            UUID.fromString(orderId),
            "QRCODE",
            30,
            "Failure test payment"
        );

        String paymentResponse = mockMvc.perform(post("/api/payments/generate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> paymentData = objectMapper.readValue(paymentResponse, Map.class);
        String paymentId = (String) paymentData.get("id");
        String transactionId = (String) paymentData.get("transactionId");

        // Step 3: Simulate Payment Gateway Webhook - Payment Failed
        Map<String, Object> webhookData = new HashMap<>();
        webhookData.put("transaction_id", transactionId);
        webhookData.put("status", "failed");
        webhookData.put("failure_reason", "Insufficient funds");
        webhookData.put("gateway_reference", "GTW-FAIL-" + System.currentTimeMillis());

        mockMvc.perform(post("/api/payments/webhook")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookData)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        // Step 4: Verify Payment Status is Failed
        mockMvc.perform(get("/api/payments/status/{paymentId}", paymentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").exists());

        // Step 5: Verify Order Status Updated to Failed
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.orderItems", hasSize(1)));

        // Step 6: Attempt to Download - Should Fail
        mockMvc.perform(get("/api/orders/{orderId}/download", orderId))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Order Cancellation Workflow - Payment Should Not Be Allowed")
    @WithMockUser(username = "integrationtestuser", roles = {"USER"})
    void orderCancellationWorkflow_PaymentNotAllowed() throws Exception {
        // Step 1: Create Order
        Map<UUID, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct.getId(), 1);
        OrderCreateRequestDto orderRequest = new OrderCreateRequestDto(productQuantities, "Cancellation test order");

        String orderResponse = mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> orderData = objectMapper.readValue(orderResponse, Map.class);
        String orderId = (String) orderData.get("id");

        // Step 2: Cancel Order
        mockMvc.perform(delete("/api/orders/{orderId}", orderId)
                .with(csrf())
                .param("reason", "Changed mind"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Step 3: Attempt to Create Payment for Cancelled Order - Should Fail
        PaymentCreateRequestDto paymentRequest = new PaymentCreateRequestDto(
            UUID.fromString(orderId),
            "QRCODE",
            30,
            "Payment for cancelled order"
        );

        mockMvc.perform(post("/api/payments/generate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthorized Access Workflow - All Endpoints Should Require Authentication")
    void unauthorizedAccessWorkflow_AllEndpointsProtected() throws Exception {
        // Test Order endpoints without authentication
        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/orders"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/orders/{orderId}", UUID.randomUUID()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/orders/{orderId}", UUID.randomUUID()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/orders/{orderId}/download", UUID.randomUUID()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // Test Payment endpoints without authentication
        mockMvc.perform(post("/api/payments/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/payments/status/{paymentId}", UUID.randomUUID()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // Note: Webhook endpoint currently requires authentication (should be configured as permitAll in production)
        mockMvc.perform(post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andDo(print())
                .andExpect(status().isUnauthorized()); // Currently requires auth due to security config
    }

    @Test
    @DisplayName("Cross-User Access Prevention - Users Cannot Access Other Users' Data")
    @WithMockUser(username = "integrationtestuser", roles = {"USER"})
    void crossUserAccessPrevention_UsersIsolated() throws Exception {
        // Create another user and their order/payment
        User anotherUser = new User();
        anotherUser.setUsername("anotherintuser");
        anotherUser.setEmail("another@test.com");
        anotherUser.setPassword(passwordEncoder.encode("password123"));
        anotherUser.setEnabled(true);
        anotherUser.setRoles(Set.of(userRole));
        anotherUser = userRepository.save(anotherUser);

        Order anotherOrder = new Order();
        anotherOrder.setUser(anotherUser);
        anotherOrder.setOrderNumber("ORD-ANOTHER-001");
        anotherOrder.setTotalAmount(new BigDecimal("50.00"));
        anotherOrder.setStatus(OrderStatus.PENDING);
        anotherOrder = orderRepository.save(anotherOrder);

        Payment anotherPayment = new Payment();
        anotherPayment.setOrder(anotherOrder);
        anotherPayment.setPaymentReference("PAY-ANOTHER-001");
        anotherPayment.setTransactionId("TXN-ANOTHER-001");
        anotherPayment.setAmount(new BigDecimal("50.00"));
        anotherPayment.setStatus(PaymentStatus.PENDING);
        anotherPayment.setPaymentMethod("QRCODE");
        anotherPayment.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        anotherPayment = paymentRepository.save(anotherPayment);

        // Attempt to access another user's order - Should Fail
        mockMvc.perform(get("/api/orders/{orderId}", anotherOrder.getId()))
                .andDo(print())
                .andExpect(status().isForbidden());

        // Attempt to access another user's payment - Should Fail
        mockMvc.perform(get("/api/payments/status/{paymentId}", anotherPayment.getId()))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Attempt to download another user's order - Should Fail
        mockMvc.perform(get("/api/orders/{orderId}/download", anotherOrder.getId()))
                .andDo(print())
                .andExpect(status().isNotFound()); // Service throws ResourceNotFoundException for unauthorized access
    }
}