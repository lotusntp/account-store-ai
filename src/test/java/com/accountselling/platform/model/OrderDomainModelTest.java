package com.accountselling.platform.model;

import static org.junit.jupiter.api.Assertions.*;

import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Order, OrderItem, and Payment domain models. Tests entity relationships, business
 * logic, and validation.
 */
@DisplayName("Order Domain Model Tests")
class OrderDomainModelTest {

  private User testUser;
  private Category testCategory;
  private Product testProduct;
  private Stock testStock;
  private Order testOrder;
  private OrderItem testOrderItem;
  private Payment testPayment;

  @BeforeEach
  void setUp() {
    // Create test user
    testUser = new User("testuser", "hashedpassword");
    testUser.setEmail("test@example.com");

    // Create test category
    testCategory = new Category("Test Category");

    // Create test product
    testProduct = new Product("Test Product", new BigDecimal("100.00"), testCategory);
    testProduct.setDescription("Test product description");
    testProduct.setServer("Test Server");

    // Create test stock
    testStock = new Stock(testProduct, "test:credentials");

    // Create test order
    testOrder = new Order(testUser, new BigDecimal("100.00"));

    // Create test order item
    testOrderItem = new OrderItem(testOrder, testProduct, testStock, new BigDecimal("100.00"));

    // Create test payment
    testPayment = new Payment(testOrder, new BigDecimal("100.00"), "QR_CODE");
  }

  @Test
  @DisplayName("Order entity should be created with correct properties")
  void testOrderCreation() {
    assertNotNull(testOrder);
    assertEquals(testUser, testOrder.getUser());
    assertEquals(new BigDecimal("100.00"), testOrder.getTotalAmount());
    assertEquals(OrderStatus.PENDING, testOrder.getStatus());
    assertNotNull(testOrder.getOrderNumber());
    assertTrue(testOrder.getOrderNumber().startsWith("ORD-"));
  }

  @Test
  @DisplayName("Order should manage order items correctly")
  void testOrderItemManagement() {
    testOrder.addOrderItem(testOrderItem);

    assertEquals(1, testOrder.getOrderItems().size());
    assertTrue(testOrder.getOrderItems().contains(testOrderItem));
    assertEquals(testOrder, testOrderItem.getOrder());
    assertEquals(1, testOrder.getTotalItemCount());
    assertFalse(testOrder.isEmpty());
  }

  @Test
  @DisplayName("Order should calculate total amount correctly")
  void testOrderTotalCalculation() {
    testOrder.addOrderItem(testOrderItem);

    BigDecimal calculatedTotal = testOrder.calculateTotalAmount();
    assertEquals(new BigDecimal("100.00"), calculatedTotal);

    testOrder.updateTotalAmount();
    assertEquals(calculatedTotal, testOrder.getTotalAmount());
  }

  @Test
  @DisplayName("Order status transitions should work correctly")
  void testOrderStatusTransitions() {
    assertTrue(testOrder.isPending());
    assertFalse(testOrder.isCompleted());

    testOrder.markAsProcessing();
    assertTrue(testOrder.isProcessing());

    testOrder.markAsCompleted();
    assertTrue(testOrder.isCompleted());
    assertFalse(testOrder.isPending());
  }

  @Test
  @DisplayName("Order status transitions should validate state")
  void testOrderStatusValidation() {
    testOrder.markAsCompleted();

    assertThrows(IllegalStateException.class, () -> testOrder.markAsFailed());
    assertThrows(IllegalStateException.class, () -> testOrder.markAsCancelled());
  }

  @Test
  @DisplayName("OrderItem should be created with correct properties")
  void testOrderItemCreation() {
    assertNotNull(testOrderItem);
    assertEquals(testOrder, testOrderItem.getOrder());
    assertEquals(testProduct, testOrderItem.getProduct());
    assertEquals(testStock, testOrderItem.getStockItem());
    assertEquals(new BigDecimal("100.00"), testOrderItem.getPrice());
    assertEquals("Test Product", testOrderItem.getProductName());
    assertEquals("Test Category", testOrderItem.getCategoryName());
    assertEquals("Test Server", testOrderItem.getServer());
  }

  @Test
  @DisplayName("OrderItem should validate stock item for product")
  void testOrderItemStockValidation() {
    assertTrue(testOrderItem.isValidStockItemForProduct());
    assertTrue(testOrderItem.isPriceValid());
  }

  @Test
  @DisplayName("OrderItem should manage stock item operations")
  void testOrderItemStockOperations() {
    assertFalse(testOrderItem.isStockItemSold());
    assertTrue(testOrderItem.isStockItemAvailable());

    testOrderItem.markStockItemAsSold();
    assertTrue(testOrderItem.isStockItemSold());
    assertFalse(testOrderItem.isStockItemAvailable());
  }

  @Test
  @DisplayName("Payment should be created with correct properties")
  void testPaymentCreation() {
    assertNotNull(testPayment);
    assertEquals(testOrder, testPayment.getOrder());
    assertEquals(new BigDecimal("100.00"), testPayment.getAmount());
    assertEquals(PaymentStatus.PENDING, testPayment.getStatus());
    assertEquals("QR_CODE", testPayment.getPaymentMethod());
    assertNotNull(testPayment.getPaymentReference());
    assertTrue(testPayment.getPaymentReference().startsWith("PAY-"));
  }

  @Test
  @DisplayName("Payment status transitions should work correctly")
  void testPaymentStatusTransitions() {
    assertTrue(testPayment.isPending());
    assertFalse(testPayment.isCompleted());

    testPayment.markAsProcessing();
    assertTrue(testPayment.isProcessing());

    testPayment.markAsCompleted("TXN123456");
    assertTrue(testPayment.isCompleted());
    assertEquals("TXN123456", testPayment.getTransactionId());
    assertNotNull(testPayment.getPaidAt());
  }

  @Test
  @DisplayName("Payment should handle expiration correctly")
  void testPaymentExpiration() {
    assertFalse(testPayment.isExpired());

    testPayment.setExpirationTime(LocalDateTime.now().minusMinutes(1));
    assertTrue(testPayment.isExpired());

    testPayment.setExpirationTime(30);
    assertFalse(testPayment.isExpired());
    assertTrue(testPayment.getRemainingExpirationMinutes() > 0);
  }

  @Test
  @DisplayName("Payment should validate amount against order")
  void testPaymentAmountValidation() {
    assertTrue(testPayment.isAmountValid());

    testPayment.setAmount(new BigDecimal("50.00"));
    assertFalse(testPayment.isAmountValid());
  }

  @Test
  @DisplayName("Payment refund operations should work correctly")
  void testPaymentRefund() {
    testPayment.markAsCompleted("TXN123456");
    assertTrue(testPayment.canBeRefunded());

    testPayment.markAsRefunded(new BigDecimal("100.00"));
    assertTrue(testPayment.isRefunded());
    assertEquals(new BigDecimal("100.00"), testPayment.getRefundAmount());
    assertNotNull(testPayment.getRefundedAt());
  }

  @Test
  @DisplayName("User should manage orders correctly")
  void testUserOrderManagement() {
    testUser.addOrder(testOrder);

    assertTrue(testUser.hasOrders());
    assertEquals(1, testUser.getOrderCount());
    assertTrue(testUser.getOrders().contains(testOrder));
    assertEquals(testUser, testOrder.getUser());
  }

  @Test
  @DisplayName("Order and Payment relationship should work correctly")
  void testOrderPaymentRelationship() {
    testOrder.setPayment(testPayment);

    assertEquals(testPayment, testOrder.getPayment());
    assertEquals(testOrder, testPayment.getOrder());
  }

  @Test
  @DisplayName("Entity relationships should be bidirectional")
  void testBidirectionalRelationships() {
    // Order -> User relationship
    testUser.addOrder(testOrder);
    assertTrue(testUser.getOrders().contains(testOrder));
    assertEquals(testUser, testOrder.getUser());

    // Order -> OrderItem relationship
    testOrder.addOrderItem(testOrderItem);
    assertTrue(testOrder.getOrderItems().contains(testOrderItem));
    assertEquals(testOrder, testOrderItem.getOrder());

    // Order -> Payment relationship
    testOrder.setPayment(testPayment);
    assertEquals(testPayment, testOrder.getPayment());
    assertEquals(testOrder, testPayment.getOrder());
  }

  @Test
  @DisplayName("Display methods should return formatted values")
  void testDisplayMethods() {
    assertEquals("฿100.00", testOrder.getFormattedTotalAmount());
    assertEquals("฿100.00", testOrderItem.getFormattedPrice());
    assertEquals("฿100.00", testPayment.getFormattedAmount());

    assertEquals("Pending", testOrder.getStatusDisplayName());
    assertEquals("Pending", testPayment.getStatusDisplayName());

    assertNotNull(testOrderItem.getDisplayInfo());
    assertTrue(testOrderItem.getDisplayInfo().contains("Test Product"));
  }
}
