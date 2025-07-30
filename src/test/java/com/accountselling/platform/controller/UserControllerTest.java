package com.accountselling.platform.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.model.*;
import com.accountselling.platform.repository.*;
import com.accountselling.platform.service.JwtTokenService;
import com.accountselling.platform.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for UserController. Tests user profile management, order history, and account
 * downloads.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

  @Autowired private WebApplicationContext context;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private ProductRepository productRepository;

  @Autowired private OrderRepository orderRepository;

  @Autowired private StockRepository stockRepository;

  @Autowired private PaymentRepository paymentRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtTokenService jwtTokenService;

  @Autowired private UserService userService;

  private MockMvc mockMvc;

  private User testUser;
  private Role userRole;
  private String accessToken;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Create USER role if it doesn't exist
    userRole =
        roleRepository
            .findByName("USER")
            .orElseGet(
                () -> {
                  Role role = new Role();
                  role.setName("USER");
                  return roleRepository.save(role);
                });

    // Create test user
    testUser = new User();
    testUser.setUsername("testuser");
    testUser.setPassword(passwordEncoder.encode("password123"));
    testUser.setEmail("test@example.com");
    testUser.setFirstName("Test");
    testUser.setLastName("User");
    testUser.setRoles(Set.of(userRole));
    testUser = userRepository.save(testUser);

    // Generate access token using UserDetails
    UserDetails userDetails = userService.loadUserByUsername(testUser.getUsername());
    accessToken = jwtTokenService.generateAccessToken(userDetails);
  }

  @Test
  @DisplayName("Should get user profile successfully")
  void shouldGetUserProfileSuccessfully() throws Exception {
    mockMvc
        .perform(
            get("/api/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is("testuser")))
        .andExpect(jsonPath("$.email", is("test@example.com")))
        .andExpect(jsonPath("$.firstName", is("Test")))
        .andExpect(jsonPath("$.lastName", is("User")))
        .andExpect(jsonPath("$.fullName", is("Test User")))
        .andExpect(jsonPath("$.roles", hasItem("USER")))
        .andExpect(jsonPath("$.enabled", is(true)))
        .andExpect(jsonPath("$.totalOrders", notNullValue()))
        .andExpect(jsonPath("$.completedOrders", notNullValue()))
        .andExpect(jsonPath("$.pendingOrders", notNullValue()));
  }

  @Test
  @DisplayName("Should reject profile request without authentication")
  void shouldRejectProfileRequestWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/users/profile").contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should reject profile request with invalid token")
  void shouldRejectProfileRequestWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            get("/api/users/profile")
                .header("Authorization", "Bearer invalid.token.here")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should get user orders successfully")
  void shouldGetUserOrdersSuccessfully() throws Exception {
    // Create test data
    Category category = createTestCategory();
    Product product = createTestProduct(category);
    Stock stock = createTestStock(product);
    Order order = createTestOrder(testUser, product, stock);

    mockMvc
        .perform(
            get("/api/users/orders")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[0].orderId", is(order.getId().toString())))
        .andExpect(jsonPath("$[0].status", is(order.getStatus().toString())))
        .andExpect(jsonPath("$[0].totalAmount", is(order.getTotalAmount().doubleValue())))
        .andExpect(jsonPath("$[0].totalItems", is(1)))
        .andExpect(jsonPath("$[0].items", hasSize(1)))
        .andExpect(jsonPath("$[0].items[0].productName", is(product.getName())));
  }

  @Test
  @DisplayName("Should reject orders request without authentication")
  void shouldRejectOrdersRequestWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/users/orders").contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should download account data for completed order")
  void shouldDownloadAccountDataForCompletedOrder() throws Exception {
    // Create test data
    Category category = createTestCategory();
    Product product = createTestProduct(category);
    Stock stock = createTestStock(product);
    Order order = createTestOrder(testUser, product, stock);

    // Mark order as completed
    order.setStatus(OrderStatus.COMPLETED);
    orderRepository.save(order);

    mockMvc
        .perform(
            get("/api/users/download/" + order.getId())
                .header("Authorization", "Bearer " + accessToken))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    containsString(
                        "attachment; filename=\"accounts_order_" + order.getId() + ".txt\"")))
        .andExpect(content().string(containsString("=== ACCOUNT INFORMATION ===")))
        .andExpect(content().string(containsString("Order ID: " + order.getId())))
        .andExpect(content().string(containsString("Customer: " + testUser.getUsername())))
        .andExpect(content().string(containsString(product.getName())))
        .andExpect(content().string(containsString(stock.getAccountData())));
  }

  @Test
  @DisplayName("Should reject download for non-completed order")
  void shouldRejectDownloadForNonCompletedOrder() throws Exception {
    // Create test data with pending order
    Category category = createTestCategory();
    Product product = createTestProduct(category);
    Stock stock = createTestStock(product);
    Order order = createTestOrder(testUser, product, stock);

    // Keep order as pending
    order.setStatus(OrderStatus.PENDING);
    orderRepository.save(order);

    mockMvc
        .perform(
            get("/api/users/download/" + order.getId())
                .header("Authorization", "Bearer " + accessToken))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.error", is("Bad Request")))
        .andExpect(jsonPath("$.message", containsString("must be completed")))
        .andExpect(jsonPath("$.path", is("/api/users/download/" + order.getId())));
  }

  @Test
  @DisplayName("Should reject download for non-existent order")
  void shouldRejectDownloadForNonExistentOrder() throws Exception {
    UUID nonExistentOrderId = UUID.randomUUID();

    mockMvc
        .perform(
            get("/api/users/download/" + nonExistentOrderId)
                .header("Authorization", "Bearer " + accessToken))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString("Order not found")));
  }

  @Test
  @DisplayName("Should reject download for order belonging to another user")
  void shouldRejectDownloadForOrderBelongingToAnotherUser() throws Exception {
    // Create another user
    User anotherUser = new User();
    anotherUser.setUsername("anotheruser");
    anotherUser.setPassword(passwordEncoder.encode("password123"));
    anotherUser.setRoles(Set.of(userRole));
    anotherUser = userRepository.save(anotherUser);

    // Create test data for another user
    Category category = createTestCategory();
    Product product = createTestProduct(category);
    Stock stock = createTestStock(product);
    Order order = createTestOrder(anotherUser, product, stock);

    // Mark order as completed
    order.setStatus(OrderStatus.COMPLETED);
    orderRepository.save(order);

    mockMvc
        .perform(
            get("/api/users/download/" + order.getId())
                .header("Authorization", "Bearer " + accessToken))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should reject download without authentication")
  void shouldRejectDownloadWithoutAuthentication() throws Exception {
    UUID orderId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/users/download/" + orderId))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  private Category createTestCategory() {
    Category category = new Category();
    category.setName("Test Category");
    category.setDescription("Test category description");
    return categoryRepository.save(category);
  }

  private Product createTestProduct(Category category) {
    Product product = new Product();
    product.setName("Test Product");
    product.setDescription("Test product description");
    product.setPrice(new BigDecimal("99.99"));
    product.setImageUrl("http://example.com/image.jpg");
    product.setServer("Test Server");
    product.setCategory(category);
    product.setActive(true);
    return productRepository.save(product);
  }

  private Stock createTestStock(Product product) {
    Stock stock = new Stock();
    stock.setProduct(product);
    stock.setAccountData("username:testuser\npassword:testpass123");
    stock.setSold(false);
    return stockRepository.save(stock);
  }

  private Order createTestOrder(User user, Product product, Stock stock) {
    Order order = new Order();
    order.setUser(user);
    order.setTotalAmount(product.getPrice());
    order.setStatus(OrderStatus.PENDING);
    order = orderRepository.save(order);

    // Create order item
    OrderItem orderItem = new OrderItem();
    orderItem.setOrder(order);
    orderItem.setProduct(product);
    orderItem.setStockItem(stock);
    orderItem.setPrice(product.getPrice());
    // Set required fields for database constraints
    orderItem.setProductName(product.getName());
    orderItem.setProductDescription(product.getDescription());
    orderItem.setCategoryName(product.getCategory().getName());
    orderItem.setServer(product.getServer());

    order.getOrderItems().add(orderItem);
    return orderRepository.save(order);
  }
}
