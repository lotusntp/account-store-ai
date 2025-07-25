package com.accountselling.platform.repository;

import static org.assertj.core.api.Assertions.*;

import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.enums.PaymentStatus;
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
 * Comprehensive unit tests for PaymentRepository. Tests payment management operations including
 * payment tracking, status management, transaction handling, and financial reporting.
 *
 * <p>PaymentRepository testing covering payment management status tracking, transaction handling,
 * and financial reporting
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Payment Repository Tests")
class PaymentRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private PaymentRepository paymentRepository;

  private User testUser1;
  private User testUser2;
  private Order testOrder1;
  private Order testOrder2;
  private Order testOrder3;
  private Payment pendingPayment;
  private Payment processingPayment;
  private Payment completedPayment;
  private Payment failedPayment;
  private Payment refundedPayment;

  @BeforeEach
  void setUp() {
    String validPassword = "$2a$10$12345678901234567890123456789012345678901234567890122";

    testUser1 = new User("testuser1", validPassword, "user1@test.com");
    entityManager.persistAndFlush(testUser1);

    testUser2 = new User("testuser2", validPassword, "user2@test.com");
    entityManager.persistAndFlush(testUser2);

    // Create test orders
    testOrder1 = new Order(testUser1, new BigDecimal("100.00"), OrderStatus.PENDING);
    testOrder1.setOrderNumber("ORD-TEST-001");
    entityManager.persistAndFlush(testOrder1);

    testOrder2 = new Order(testUser1, new BigDecimal("200.00"), OrderStatus.COMPLETED);
    testOrder2.setOrderNumber("ORD-TEST-002");
    entityManager.persistAndFlush(testOrder2);

    testOrder3 = new Order(testUser2, new BigDecimal("300.00"), OrderStatus.PROCESSING);
    testOrder3.setOrderNumber("ORD-TEST-003");
    entityManager.persistAndFlush(testOrder3);

    // Create test payments with different statuses
    pendingPayment = new Payment(testOrder1, new BigDecimal("100.00"), "QR_CODE");
    pendingPayment.setStatus(PaymentStatus.PENDING);
    pendingPayment.setPaymentReference("PAY-PENDING-001");
    pendingPayment.setQrCodeUrl("https://example.com/qr1");
    pendingPayment.setExpirationTime(60);
    entityManager.persistAndFlush(pendingPayment);

    processingPayment = new Payment(testOrder3, new BigDecimal("300.00"), "BANK_TRANSFER");
    processingPayment.setStatus(PaymentStatus.PROCESSING);
    processingPayment.setPaymentReference("PAY-PROCESSING-001");
    processingPayment.setTransactionId("TXN-123456");
    entityManager.persistAndFlush(processingPayment);

    completedPayment = new Payment(testOrder2, new BigDecimal("200.00"), "QR_CODE");
    completedPayment.setStatus(PaymentStatus.COMPLETED);
    completedPayment.setPaymentReference("PAY-COMPLETED-001");
    completedPayment.setTransactionId("TXN-789012");
    completedPayment.setPaidAt(LocalDateTime.now().minusHours(1));
    entityManager.persistAndFlush(completedPayment);

    // Create additional test orders for failed and refunded payments
    Order failedOrder = new Order(testUser2, new BigDecimal("150.00"), OrderStatus.FAILED);
    failedOrder.setOrderNumber("ORD-TEST-004");
    entityManager.persistAndFlush(failedOrder);

    failedPayment = new Payment(failedOrder, new BigDecimal("150.00"), "CREDIT_CARD");
    failedPayment.setStatus(PaymentStatus.FAILED);
    failedPayment.setPaymentReference("PAY-FAILED-001");
    failedPayment.setFailureReason("Insufficient funds");
    entityManager.persistAndFlush(failedPayment);

    Order refundedOrder = new Order(testUser1, new BigDecimal("250.00"), OrderStatus.COMPLETED);
    refundedOrder.setOrderNumber("ORD-TEST-005");
    entityManager.persistAndFlush(refundedOrder);

    refundedPayment = new Payment(refundedOrder, new BigDecimal("250.00"), "QR_CODE");
    refundedPayment.setStatus(PaymentStatus.REFUNDED);
    refundedPayment.setPaymentReference("PAY-REFUNDED-001");
    refundedPayment.setTransactionId("TXN-345678");
    refundedPayment.setPaidAt(LocalDateTime.now().minusDays(1));
    refundedPayment.setRefundAmount(new BigDecimal("250.00"));
    refundedPayment.setRefundedAt(LocalDateTime.now().minusHours(2));
    entityManager.persistAndFlush(refundedPayment);

    entityManager.clear();
  }

  // ==================== BASIC PAYMENT QUERIES TESTS ====================

  @Test
  @DisplayName("Should find payment by order")
  void shouldFindPaymentByOrder() {
    // When
    Optional<Payment> result = paymentRepository.findByOrder(testOrder1);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getPaymentReference()).isEqualTo("PAY-PENDING-001");
    assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(result.get().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
  }

  @Test
  @DisplayName("Should find payment by order ID")
  void shouldFindPaymentByOrderId() {
    // When
    Optional<Payment> result = paymentRepository.findByOrderId(testOrder2.getId());

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getPaymentReference()).isEqualTo("PAY-COMPLETED-001");
    assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
  }

  @Test
  @DisplayName("Should find payment by transaction ID")
  void shouldFindPaymentByTransactionId() {
    // When
    Optional<Payment> result = paymentRepository.findByTransactionId("TXN-789012");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    assertThat(result.get().getTransactionId()).isEqualTo("TXN-789012");
  }

  @Test
  @DisplayName("Should find payment by payment reference")
  void shouldFindPaymentByPaymentReference() {
    // When
    Optional<Payment> result = paymentRepository.findByPaymentReference("PAY-PROCESSING-001");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    assertThat(result.get().getPaymentMethod()).isEqualTo("BANK_TRANSFER");
  }

  @Test
  @DisplayName("Should check if transaction ID exists")
  void shouldCheckIfTransactionIdExists() {
    // When & Then
    assertThat(paymentRepository.existsByTransactionId("TXN-789012")).isTrue();
    assertThat(paymentRepository.existsByTransactionId("NON-EXISTENT-TXN")).isFalse();
  }

  @Test
  @DisplayName("Should check if payment reference exists")
  void shouldCheckIfPaymentReferenceExists() {
    // When & Then
    assertThat(paymentRepository.existsByPaymentReference("PAY-COMPLETED-001")).isTrue();
    assertThat(paymentRepository.existsByPaymentReference("NON-EXISTENT-REF")).isFalse();
  }

  // ==================== STATUS-BASED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find payments by status")
  void shouldFindPaymentsByStatus() {
    // When
    List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);
    List<Payment> completedPayments = paymentRepository.findByStatus(PaymentStatus.COMPLETED);

    // Then
    assertThat(pendingPayments).hasSize(1);
    assertThat(pendingPayments.get(0).getPaymentReference()).isEqualTo("PAY-PENDING-001");

    assertThat(completedPayments).hasSize(1);
    assertThat(completedPayments.get(0).getPaymentReference()).isEqualTo("PAY-COMPLETED-001");
  }

  @Test
  @DisplayName("Should find payments by status with pagination")
  void shouldFindPaymentsByStatusWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 1);

    // When
    Page<Payment> result = paymentRepository.findByStatus(PaymentStatus.PENDING, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should find payments by multiple statuses")
  void shouldFindPaymentsByMultipleStatuses() {
    // Given
    List<PaymentStatus> statuses = List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED);

    // When
    List<Payment> result = paymentRepository.findByStatusIn(statuses);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(Payment::getStatus)
        .containsExactlyInAnyOrder(PaymentStatus.PENDING, PaymentStatus.COMPLETED);
  }

  @Test
  @DisplayName("Should find pending payments")
  void shouldFindPendingPayments() {
    // When
    List<Payment> result = paymentRepository.findPendingPayments();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
  }

  @Test
  @DisplayName("Should find completed payments")
  void shouldFindCompletedPayments() {
    // When
    List<Payment> result = paymentRepository.findCompletedPayments();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    assertThat(result.get(0).getPaidAt()).isNotNull();
  }

  @Test
  @DisplayName("Should find failed payments")
  void shouldFindFailedPayments() {
    // When
    List<Payment> result = paymentRepository.findFailedPayments();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(result.get(0).getFailureReason()).isEqualTo("Insufficient funds");
  }

  // ==================== USER-BASED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find payments by user")
  void shouldFindPaymentsByUser() {
    // When
    List<Payment> result = paymentRepository.findByUser(testUser1);

    // Then
    assertThat(result).hasSize(3); // pending, completed, refunded
    assertThat(result)
        .allSatisfy(
            payment ->
                assertThat(payment.getOrder().getUser().getId()).isEqualTo(testUser1.getId()));
  }

  @Test
  @DisplayName("Should find payments by user with pagination")
  void shouldFindPaymentsByUserWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 2);

    // When
    Page<Payment> result = paymentRepository.findByUser(testUser1, pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getTotalElements()).isEqualTo(3);
  }

  @Test
  @DisplayName("Should find payments by username")
  void shouldFindPaymentsByUsername() {
    // When
    List<Payment> result = paymentRepository.findByUsername("testuser2");

    // Then
    assertThat(result).hasSize(2); // processing, failed
    assertThat(result)
        .allSatisfy(
            payment ->
                assertThat(payment.getOrder().getUser().getUsername()).isEqualTo("testuser2"));
  }

  @Test
  @DisplayName("Should find payments by user and status")
  void shouldFindPaymentsByUserAndStatus() {
    // When
    List<Payment> result =
        paymentRepository.findByUserAndStatus(testUser1, PaymentStatus.COMPLETED);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    assertThat(result.get(0).getOrder().getUser().getId()).isEqualTo(testUser1.getId());
  }

  // ==================== DATE-BASED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find payments created within date range")
  void shouldFindPaymentsCreatedWithinDateRange() {
    // Given
    LocalDateTime startDate = LocalDateTime.now().minusHours(1);
    LocalDateTime endDate = LocalDateTime.now().plusHours(1);

    // When
    List<Payment> result = paymentRepository.findByCreatedAtBetween(startDate, endDate);

    // Then
    assertThat(result).hasSizeGreaterThanOrEqualTo(4); // Most payments created recently
    assertThat(result)
        .allSatisfy(payment -> assertThat(payment.getCreatedAt()).isBetween(startDate, endDate));
  }

  @Test
  @DisplayName("Should find payments paid within date range")
  void shouldFindPaymentsPaidWithinDateRange() {
    // Given
    LocalDateTime startDate = LocalDateTime.now().minusDays(2);
    LocalDateTime endDate = LocalDateTime.now().plusHours(1);

    // When
    List<Payment> result = paymentRepository.findByPaidAtBetween(startDate, endDate);

    // Then
    assertThat(result).hasSize(2); // completed and refunded payments
    assertThat(result)
        .allSatisfy(
            payment -> {
              assertThat(payment.getPaidAt()).isNotNull();
              assertThat(payment.getPaidAt()).isBetween(startDate, endDate);
            });
  }

  // ==================== EXPIRATION-BASED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find payments expiring soon")
  void shouldFindPaymentsExpiringSoon() {
    // Given
    LocalDateTime threshold = LocalDateTime.now().plusHours(2);

    // When
    List<Payment> result = paymentRepository.findPaymentsExpiringSoon(threshold);

    // Then
    assertThat(result).hasSize(1); // The pending payment expires in 1 hour
    assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
  }

  // ==================== AMOUNT-BASED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find payments by amount range")
  void shouldFindPaymentsByAmountRange() {
    // Given
    BigDecimal minAmount = new BigDecimal("150.00");
    BigDecimal maxAmount = new BigDecimal("300.00");

    // When
    List<Payment> result = paymentRepository.findByAmountBetween(minAmount, maxAmount);

    // Then
    assertThat(result).hasSize(4); // 200.00, 300.00, 250.00, 150.00
    assertThat(result)
        .allSatisfy(payment -> assertThat(payment.getAmount()).isBetween(minAmount, maxAmount));
  }

  @Test
  @DisplayName("Should find payments with amount greater than threshold")
  void shouldFindPaymentsWithAmountGreaterThan() {
    // Given
    BigDecimal threshold = new BigDecimal("200.00");

    // When
    List<Payment> result = paymentRepository.findByAmountGreaterThan(threshold);

    // Then
    assertThat(result).hasSize(2); // 300.00, 250.00
    assertThat(result)
        .allSatisfy(payment -> assertThat(payment.getAmount()).isGreaterThan(threshold));
  }

  // ==================== METHOD-BASED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find payments by payment method")
  void shouldFindPaymentsByPaymentMethod() {
    // When
    List<Payment> qrPayments = paymentRepository.findByPaymentMethod("QR_CODE");
    List<Payment> bankPayments = paymentRepository.findByPaymentMethod("BANK_TRANSFER");

    // Then
    assertThat(qrPayments).hasSize(3);
    assertThat(bankPayments).hasSize(1);
  }

  @Test
  @DisplayName("Should get distinct payment methods")
  void shouldGetDistinctPaymentMethods() {
    // When
    List<String> result = paymentRepository.findDistinctPaymentMethods();

    // Then
    assertThat(result).containsExactlyInAnyOrder("QR_CODE", "BANK_TRANSFER", "CREDIT_CARD");
  }

  // ==================== COUNTING QUERIES TESTS ====================

  @Test
  @DisplayName("Should count payments by status")
  void shouldCountPaymentsByStatus() {
    // When
    long pendingCount = paymentRepository.countByStatus(PaymentStatus.PENDING);
    long completedCount = paymentRepository.countByStatus(PaymentStatus.COMPLETED);

    // Then
    assertThat(pendingCount).isEqualTo(1);
    assertThat(completedCount).isEqualTo(1);
  }

  @Test
  @DisplayName("Should count payments by user")
  void shouldCountPaymentsByUser() {
    // When
    long user1Count = paymentRepository.countByUser(testUser1);
    long user2Count = paymentRepository.countByUser(testUser2);

    // Then
    assertThat(user1Count).isEqualTo(3);
    assertThat(user2Count).isEqualTo(2);
  }

  @Test
  @DisplayName("Should count payments by payment method")
  void shouldCountPaymentsByPaymentMethod() {
    // When
    long qrCount = paymentRepository.countByPaymentMethod("QR_CODE");
    long bankCount = paymentRepository.countByPaymentMethod("BANK_TRANSFER");

    // Then
    assertThat(qrCount).isEqualTo(3);
    assertThat(bankCount).isEqualTo(1);
  }

  // ==================== AGGREGATION QUERIES TESTS ====================

  @Test
  @DisplayName("Should calculate total amount by status")
  void shouldCalculateTotalAmountByStatus() {
    // When
    BigDecimal completedTotal =
        paymentRepository.calculateTotalAmountByStatus(PaymentStatus.COMPLETED);
    BigDecimal failedTotal = paymentRepository.calculateTotalAmountByStatus(PaymentStatus.FAILED);

    // Then
    assertThat(completedTotal).isEqualByComparingTo(new BigDecimal("200.00"));
    assertThat(failedTotal).isEqualByComparingTo(new BigDecimal("150.00"));
  }

  @Test
  @DisplayName("Should calculate total completed amount")
  void shouldCalculateTotalCompletedAmount() {
    // When
    BigDecimal result = paymentRepository.calculateTotalCompletedAmount();

    // Then
    assertThat(result).isEqualByComparingTo(new BigDecimal("200.00"));
  }

  @Test
  @DisplayName("Should calculate total amount by user")
  void shouldCalculateTotalAmountByUser() {
    // When
    BigDecimal user1Total = paymentRepository.calculateTotalAmountByUser(testUser1);
    BigDecimal user2Total = paymentRepository.calculateTotalAmountByUser(testUser2);

    // Then
    assertThat(user1Total).isEqualByComparingTo(new BigDecimal("550.00")); // 100+200+250
    assertThat(user2Total).isEqualByComparingTo(new BigDecimal("450.00")); // 300+150
  }

  @Test
  @DisplayName("Should calculate average payment amount")
  void shouldCalculateAveragePaymentAmount() {
    // When
    BigDecimal result = paymentRepository.calculateAveragePaymentAmount();

    // Then
    assertThat(result).isGreaterThan(BigDecimal.ZERO);
    // 5 payments: 100+200+300+150+250 = 1000, average = 200
    assertThat(result).isEqualByComparingTo(new BigDecimal("200.00"));
  }

  // ==================== REFUND-RELATED QUERIES TESTS ====================

  @Test
  @DisplayName("Should find refunded payments")
  void shouldFindRefundedPayments() {
    // When
    List<Payment> result = paymentRepository.findRefundedPayments();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    assertThat(result.get(0).getRefundAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
  }

  @Test
  @DisplayName("Should calculate total refunded amount")
  void shouldCalculateTotalRefundedAmount() {
    // When
    BigDecimal result = paymentRepository.calculateTotalRefundedAmount();

    // Then
    assertThat(result).isEqualByComparingTo(new BigDecimal("250.00"));
  }

  // ==================== SEARCH QUERIES TESTS ====================

  @Test
  @DisplayName("Should search payments by multiple criteria")
  void shouldSearchPaymentsByMultipleCriteria() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<Payment> result =
        paymentRepository.searchPayments(
            "TXN-789", // transaction ID pattern
            null, // payment reference
            "testuser1", // username
            PaymentStatus.COMPLETED, // status
            "QR_CODE", // payment method
            null,
            null, // date range
            new BigDecimal("100.00"), // min amount
            new BigDecimal("300.00"), // max amount
            pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getTransactionId()).contains("TXN-789");
    assertThat(result.getContent().get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
  }

  @Test
  @DisplayName("Should find payments with QR codes")
  void shouldFindPaymentsWithQrCodes() {
    // When
    List<Payment> result = paymentRepository.findPaymentsWithQrCodes();

    // Then
    assertThat(result).hasSize(1); // Only pending payment has QR code URL
    assertThat(result.get(0).getQrCodeUrl()).isNotNull();
  }

  @Test
  @DisplayName("Should find payments with failure reasons")
  void shouldFindPaymentsWithFailureReasons() {
    // When
    List<Payment> result = paymentRepository.findPaymentsWithFailureReasons();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getFailureReason()).isEqualTo("Insufficient funds");
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
    assertThat(paymentRepository.findByUser(emptyUser)).isEmpty();
    assertThat(paymentRepository.countByUser(emptyUser)).isZero();
    assertThat(paymentRepository.calculateTotalAmountByUser(emptyUser))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("Should handle non-existent transaction ID")
  void shouldHandleNonExistentTransactionId() {
    // When
    Optional<Payment> result = paymentRepository.findByTransactionId("NON-EXISTENT-TXN");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should handle non-existent payment method")
  void shouldHandleNonExistentPaymentMethod() {
    // When
    List<Payment> result = paymentRepository.findByPaymentMethod("NON_EXISTENT_METHOD");
    long count = paymentRepository.countByPaymentMethod("NON_EXISTENT_METHOD");

    // Then
    assertThat(result).isEmpty();
    assertThat(count).isZero();
  }

  @Test
  @DisplayName("Should handle search with all null parameters")
  void shouldHandleSearchWithAllNullParameters() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When - Search with all null parameters
    Page<Payment> result =
        paymentRepository.searchPayments(
            null, null, null, null, null, null, null, null, null, pageable);

    // Then
    assertThat(result.getTotalElements()).isEqualTo(5); // All payments
  }
}
