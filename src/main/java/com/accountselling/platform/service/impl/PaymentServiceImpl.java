package com.accountselling.platform.service.impl;

import com.accountselling.platform.dto.statistics.DailyPaymentStatistics;
import com.accountselling.platform.dto.statistics.PaymentMethodStatistics;
import com.accountselling.platform.dto.statistics.PaymentStatistics;
import com.accountselling.platform.enums.PaymentStatus;
import com.accountselling.platform.exception.*;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.Payment;
import com.accountselling.platform.model.User;
import com.accountselling.platform.repository.OrderRepository;
import com.accountselling.platform.repository.PaymentRepository;
import com.accountselling.platform.repository.UserRepository;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of PaymentService for payment management operations. Provides comprehensive
 * payment processing functionality including creation, QR code generation, status management,
 * payment gateway integration, and financial reporting with proper transaction management and
 * concurrent access protection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;
  private final UserRepository userRepository;
  private final OrderService orderService;

  @Value("${app.payment.default-expiration-minutes:30}")
  private int defaultExpirationMinutes;

  @Value("${app.payment.qr-code.base-url:https://payment-gateway.example.com/qr}")
  private String qrCodeBaseUrl;

  // Supported payment methods
  private static final List<String> SUPPORTED_PAYMENT_METHODS =
      Arrays.asList("QR_CODE", "QRCODE", "BANK_TRANSFER", "CREDIT_CARD", "MOBILE_BANKING");

  // ==================== PAYMENT CREATION ====================

  @Override
  @Transactional
  public Payment createPayment(Order order, String paymentMethod, int expirationMinutes) {
    log.info(
        "Creating payment for order: {} with method: {} expiration: {} minutes",
        order.getOrderNumber(),
        paymentMethod,
        expirationMinutes);

    // Validate payment creation
    validatePayment(order, paymentMethod);

    // Check if payment already exists for this order
    if (paymentRepository.findByOrder(order).isPresent()) {
      log.error("Payment already exists for order: {}", order.getOrderNumber());
      throw new PaymentAlreadyExistsException(
          "Payment already exists for order: " + order.getOrderNumber());
    }

    // Create payment entity
    Payment payment = new Payment(order, order.getTotalAmount(), paymentMethod);
    payment.setExpirationTime(expirationMinutes);

    // Generate QR code
    String qrCodeUrl = generateQrCode(payment);
    payment.setQrCodeUrl(qrCodeUrl);

    Payment savedPayment = paymentRepository.save(payment);

    // Update order status to processing if it was pending
    if (order.isPending()) {
      try {
        orderService.markOrderAsProcessing(order.getId());
        log.debug("Marked order as processing: {}", order.getOrderNumber());
      } catch (Exception e) {
        log.warn(
            "Failed to mark order as processing: {}, continuing with payment creation",
            order.getOrderNumber(),
            e);
        // Continue with payment creation even if order status update fails
      }
    }

    log.info(
        "Successfully created payment: {} for order: {}",
        savedPayment.getPaymentReference(),
        order.getOrderNumber());

    return savedPayment;
  }

  @Override
  @Transactional
  public Payment createPayment(Order order, String paymentMethod) {
    return createPayment(order, paymentMethod, defaultExpirationMinutes);
  }

  @Override
  @Transactional
  public Payment createPaymentByOrderId(UUID orderId, String paymentMethod, int expirationMinutes) {
    log.info("Creating payment for order ID: {} with method: {}", orderId, paymentMethod);

    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () -> {
                  log.error("Order not found with ID: {}", orderId);
                  return new ResourceNotFoundException("Order not found with ID: " + orderId);
                });

    return createPayment(order, paymentMethod, expirationMinutes);
  }

  // ==================== QR CODE MANAGEMENT ====================

  @Override
  public String generateQrCode(Payment payment) {
    log.debug("Generating QR code for payment: {}", payment.getPaymentReference());

    try {
      // Generate QR code URL with payment reference and amount
      // In a real implementation, this would integrate with actual QR code generation service
      String qrCodeContent =
          String.format(
              "%s?ref=%s&amount=%.2f&expires=%s",
              qrCodeBaseUrl,
              payment.getPaymentReference(),
              payment.getAmount(),
              payment.getExpiresAt());

      log.debug("Generated QR code URL for payment: {}", payment.getPaymentReference());
      return qrCodeContent;

    } catch (Exception e) {
      log.error("Failed to generate QR code for payment: {}", payment.getPaymentReference(), e);
      throw new QrCodeGenerationException("Failed to generate QR code: " + e.getMessage());
    }
  }

  @Override
  @Transactional
  public Payment regenerateQrCode(UUID paymentId) {
    log.info("Regenerating QR code for payment: {}", paymentId);

    Payment payment = findById(paymentId);

    if (!payment.isPending() && !payment.isProcessing()) {
      log.error("Cannot regenerate QR code for payment in status: {}", payment.getStatus());
      throw new InvalidPaymentStatusException(
          "Cannot regenerate QR code for payment in status: " + payment.getStatus());
    }

    String newQrCodeUrl = generateQrCode(payment);
    payment.setQrCodeUrl(newQrCodeUrl);

    Payment updatedPayment = paymentRepository.save(payment);

    log.info("Successfully regenerated QR code for payment: {}", payment.getPaymentReference());
    return updatedPayment;
  }

  @Override
  public String getQrCodeContent(UUID paymentId) {
    log.debug("Getting QR code content for payment: {}", paymentId);

    Payment payment = findById(paymentId);

    if (payment.getQrCodeUrl() == null || payment.getQrCodeUrl().isEmpty()) {
      log.error("No QR code available for payment: {}", payment.getPaymentReference());
      throw new ResourceNotFoundException(
          "No QR code available for payment: " + payment.getPaymentReference());
    }

    return payment.getQrCodeUrl();
  }

  // ==================== PAYMENT RETRIEVAL ====================

  @Override
  public Payment findById(UUID paymentId) {
    log.debug("Finding payment by ID: {}", paymentId);

    return paymentRepository
        .findById(paymentId)
        .orElseThrow(
            () -> {
              log.error("Payment not found with ID: {}", paymentId);
              return new ResourceNotFoundException("Payment not found with ID: " + paymentId);
            });
  }

  @Override
  public Payment findByTransactionId(String transactionId) {
    log.debug("Finding payment by transaction ID: {}", transactionId);

    return paymentRepository
        .findByTransactionId(transactionId)
        .orElseThrow(
            () -> {
              log.error("Payment not found with transaction ID: {}", transactionId);
              return new ResourceNotFoundException(
                  "Payment not found with transaction ID: " + transactionId);
            });
  }

  @Override
  public Payment findByPaymentReference(String paymentReference) {
    log.debug("Finding payment by reference: {}", paymentReference);

    return paymentRepository
        .findByPaymentReference(paymentReference)
        .orElseThrow(
            () -> {
              log.error("Payment not found with reference: {}", paymentReference);
              return new ResourceNotFoundException(
                  "Payment not found with reference: " + paymentReference);
            });
  }

  @Override
  public Payment findByOrder(Order order) {
    log.debug("Finding payment by order: {}", order.getOrderNumber());

    return paymentRepository
        .findByOrder(order)
        .orElseThrow(
            () -> {
              log.error("Payment not found for order: {}", order.getOrderNumber());
              return new ResourceNotFoundException(
                  "Payment not found for order: " + order.getOrderNumber());
            });
  }

  @Override
  public Payment findByOrderId(UUID orderId) {
    log.debug("Finding payment by order ID: {}", orderId);

    return paymentRepository
        .findByOrderId(orderId)
        .orElseThrow(
            () -> {
              log.error("Payment not found for order ID: {}", orderId);
              return new ResourceNotFoundException("Payment not found for order ID: " + orderId);
            });
  }

  // ==================== PAYMENT STATUS MANAGEMENT ====================

  @Override
  @Transactional
  public Payment markPaymentAsProcessing(UUID paymentId) {
    log.info("Marking payment as processing: {}", paymentId);

    Payment payment = findById(paymentId);

    try {
      payment.markAsProcessing();
      Payment updatedPayment = paymentRepository.save(payment);

      log.info("Successfully marked payment as processing: {}", payment.getPaymentReference());
      return updatedPayment;

    } catch (IllegalStateException e) {
      log.error(
          "Cannot transition payment {} to processing status: {}",
          payment.getPaymentReference(),
          e.getMessage());
      throw new InvalidPaymentStatusException(
          "Cannot mark payment as processing: " + e.getMessage());
    }
  }

  @Override
  @Transactional
  public Payment markPaymentAsCompleted(
      UUID paymentId, String transactionId, String gatewayResponse) {
    log.info("Marking payment as completed: {} with transaction: {}", paymentId, transactionId);

    Payment payment = findById(paymentId);

    try {
      payment.markAsCompleted(transactionId);
      if (gatewayResponse != null) {
        payment.setGatewayResponse(gatewayResponse);
      }

      Payment updatedPayment = paymentRepository.save(payment);

      // Mark associated order as completed
      try {
        orderService.processOrderCompletion(payment.getOrder().getId(), transactionId);
        log.debug("Processed order completion for payment: {}", payment.getPaymentReference());
      } catch (Exception e) {
        log.error(
            "Failed to process order completion for payment: {}", payment.getPaymentReference(), e);
        // Payment is marked as completed, but order processing failed
        // This creates an inconsistent state that may require manual intervention
        throw new PaymentFailedException(
            "Payment completed but order processing failed: " + e.getMessage());
      }

      log.info("Successfully marked payment as completed: {}", payment.getPaymentReference());
      return updatedPayment;

    } catch (IllegalStateException e) {
      log.error(
          "Cannot transition payment {} to completed status: {}",
          payment.getPaymentReference(),
          e.getMessage());
      throw new InvalidPaymentStatusException(
          "Cannot mark payment as completed: " + e.getMessage());
    }
  }

  @Override
  @Transactional
  public Payment markPaymentAsFailed(UUID paymentId, String failureReason, String gatewayResponse) {
    log.info("Marking payment as failed: {} with reason: {}", paymentId, failureReason);

    Payment payment = findById(paymentId);

    try {
      payment.markAsFailed(failureReason);
      if (gatewayResponse != null) {
        payment.setGatewayResponse(gatewayResponse);
      }

      Payment updatedPayment = paymentRepository.save(payment);

      // Mark associated order as failed
      try {
        orderService.processOrderFailure(payment.getOrder().getId(), failureReason);
        log.debug("Processed order failure for payment: {}", payment.getPaymentReference());
      } catch (Exception e) {
        log.error(
            "Failed to process order failure for payment: {}", payment.getPaymentReference(), e);
        // Continue with payment failure marking even if order update fails
      }

      log.info("Successfully marked payment as failed: {}", payment.getPaymentReference());
      return updatedPayment;

    } catch (IllegalStateException e) {
      log.error(
          "Cannot transition payment {} to failed status: {}",
          payment.getPaymentReference(),
          e.getMessage());
      throw new InvalidPaymentStatusException("Cannot mark payment as failed: " + e.getMessage());
    }
  }

  @Override
  @Transactional
  public Payment cancelPayment(UUID paymentId, String reason) {
    log.info("Cancelling payment: {} with reason: {}", paymentId, reason);

    Payment payment = findById(paymentId);

    if (!payment.canBeCancelled()) {
      log.error(
          "Payment cannot be cancelled: {} with status: {}",
          payment.getPaymentReference(),
          payment.getStatus());
      throw new InvalidPaymentStatusException(
          "Payment cannot be cancelled in current status: " + payment.getStatus());
    }

    try {
      payment.markAsCancelled();
      if (reason != null && !reason.trim().isEmpty()) {
        payment.setNotes(
            payment.getNotes() != null
                ? payment.getNotes() + "\nCancellation reason: " + reason
                : "Cancellation reason: " + reason);
      }

      Payment updatedPayment = paymentRepository.save(payment);

      // Cancel associated order
      try {
        orderService.cancelOrder(payment.getOrder().getId(), reason);
        log.debug("Cancelled order for payment: {}", payment.getPaymentReference());
      } catch (Exception e) {
        log.error("Failed to cancel order for payment: {}", payment.getPaymentReference(), e);
        // Continue with payment cancellation even if order update fails
      }

      log.info("Successfully cancelled payment: {}", payment.getPaymentReference());
      return updatedPayment;

    } catch (IllegalStateException e) {
      log.error(
          "Cannot transition payment {} to cancelled status: {}",
          payment.getPaymentReference(),
          e.getMessage());
      throw new InvalidPaymentStatusException("Cannot cancel payment: " + e.getMessage());
    }
  }

  @Override
  @Transactional
  public Payment processRefund(UUID paymentId, BigDecimal refundAmount, String reason) {
    log.info(
        "Processing refund for payment: {} amount: {} reason: {}", paymentId, refundAmount, reason);

    Payment payment = findById(paymentId);

    if (!payment.canBeRefunded()) {
      log.error(
          "Payment cannot be refunded: {} with status: {}",
          payment.getPaymentReference(),
          payment.getStatus());
      throw new InvalidPaymentStatusException(
          "Payment cannot be refunded in current status: " + payment.getStatus());
    }

    if (!isRefundAmountValid(paymentId, refundAmount)) {
      log.error(
          "Invalid refund amount: {} for payment: {}", refundAmount, payment.getPaymentReference());
      throw new InvalidRefundException("Invalid refund amount: " + refundAmount);
    }

    try {
      payment.markAsRefunded(refundAmount);
      if (reason != null && !reason.trim().isEmpty()) {
        payment.setNotes(
            payment.getNotes() != null
                ? payment.getNotes() + "\nRefund reason: " + reason
                : "Refund reason: " + reason);
      }

      Payment updatedPayment = paymentRepository.save(payment);

      log.info(
          "Successfully processed refund: {} for payment: {}",
          refundAmount,
          payment.getPaymentReference());
      return updatedPayment;

    } catch (IllegalStateException e) {
      log.error(
          "Cannot transition payment {} to refunded status: {}",
          payment.getPaymentReference(),
          e.getMessage());
      throw new InvalidPaymentStatusException("Cannot process refund: " + e.getMessage());
    }
  }

  // ==================== PAYMENT GATEWAY INTEGRATION ====================

  @Override
  @Transactional
  public Payment processWebhook(String transactionId, String status, String gatewayResponse) {
    log.info("Processing webhook for transaction: {} with status: {}", transactionId, status);

    // Try to find by transaction ID first, then by payment reference
    Payment payment;
    try {
      payment = findByTransactionId(transactionId);
    } catch (ResourceNotFoundException e) {
      // If not found by transaction ID, try by payment reference
      log.debug("Payment not found by transaction ID, trying payment reference: {}", transactionId);
      payment = findByPaymentReference(transactionId);
    }

    try {
      switch (status.toUpperCase()) {
        case "COMPLETED", "SUCCESS", "PAID" -> {
          return markPaymentAsCompleted(payment.getId(), transactionId, gatewayResponse);
        }
        case "FAILED", "FAILURE", "ERROR" -> {
          return markPaymentAsFailed(
              payment.getId(), "Payment failed via webhook", gatewayResponse);
        }
        case "CANCELLED", "CANCELED" -> {
          return cancelPayment(payment.getId(), "Payment cancelled via webhook");
        }
        case "PROCESSING", "PENDING" -> {
          return markPaymentAsProcessing(payment.getId());
        }
        default -> {
          log.warn("Unknown webhook status: {} for transaction: {}", status, transactionId);
          throw new WebhookProcessingException("Unknown webhook status: " + status);
        }
      }
    } catch (Exception e) {
      log.error("Failed to process webhook for transaction: {}", transactionId, e);
      throw new WebhookProcessingException("Failed to process webhook: " + e.getMessage());
    }
  }

  @Override
  @Transactional
  public Payment verifyPaymentWithGateway(UUID paymentId) {
    log.info("Verifying payment with gateway: {}", paymentId);

    Payment payment = findById(paymentId);

    try {
      // In a real implementation, this would make an API call to payment gateway
      // For now, we'll simulate verification logic
      String gatewayStatus = checkPaymentStatusWithGateway(payment.getTransactionId());

      return processWebhook(payment.getTransactionId(), gatewayStatus, "Verified via gateway");

    } catch (Exception e) {
      log.error("Failed to verify payment with gateway: {}", payment.getPaymentReference(), e);
      throw new PaymentVerificationException("Failed to verify payment: " + e.getMessage());
    }
  }

  @Override
  public String checkPaymentStatusWithGateway(String transactionId) {
    log.debug("Checking payment status with gateway for transaction: {}", transactionId);

    try {
      // In a real implementation, this would make an API call to payment gateway
      // For now, we'll simulate the response with timeout simulation

      // Simulate potential timeout scenario
      if (transactionId != null && transactionId.contains("timeout")) {
        throw new PaymentTimeoutException(
            "Gateway request timed out for transaction: " + transactionId);
      }

      // Simulate gateway communication failure
      if (transactionId != null && transactionId.contains("gateway_error")) {
        throw new PaymentGatewayException(
            "Gateway communication failed for transaction: " + transactionId);
      }

      return "COMPLETED"; // Simulated response

    } catch (PaymentTimeoutException | PaymentGatewayException e) {
      // Re-throw specific payment exceptions
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to check payment status with gateway for transaction: {}", transactionId, e);
      throw new PaymentGatewayException("Failed to check payment status: " + e.getMessage());
    }
  }

  // ==================== PAYMENT VALIDATION ====================

  @Override
  public void validatePayment(Order order, String paymentMethod) {
    log.debug(
        "Validating payment for order: {} with method: {}", order.getOrderNumber(), paymentMethod);

    // Validate order status
    if (order.isCompleted() || order.isFailed() || order.isCancelled()) {
      log.error("Cannot create payment for order in status: {}", order.getStatus());
      throw new InvalidPaymentException(
          "Cannot create payment for order in status: " + order.getStatus());
    }

    // Validate payment method
    if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
      log.error("Payment method cannot be null or empty");
      throw new InvalidPaymentException("Payment method cannot be null or empty");
    }

    String normalizedMethod = paymentMethod.toUpperCase().trim();
    if (!SUPPORTED_PAYMENT_METHODS.contains(normalizedMethod)) {
      log.error("Unsupported payment method: {}", paymentMethod);
      throw new InvalidPaymentException("Unsupported payment method: " + paymentMethod);
    }

    // Validate order amount
    if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
      log.error("Invalid order amount: {}", order.getTotalAmount());
      throw new InvalidPaymentException("Order amount must be greater than zero");
    }

    log.debug("Payment validation passed for order: {}", order.getOrderNumber());
  }

  @Override
  public boolean isPaymentExpired(UUID paymentId) {
    try {
      Payment payment = findById(paymentId);
      return payment.isExpired();
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean canPaymentBeCancelled(UUID paymentId) {
    try {
      Payment payment = findById(paymentId);
      return payment.canBeCancelled();
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean canPaymentBeRefunded(UUID paymentId) {
    try {
      Payment payment = findById(paymentId);
      return payment.canBeRefunded();
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean isRefundAmountValid(UUID paymentId, BigDecimal refundAmount) {
    try {
      Payment payment = findById(paymentId);

      if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
        return false;
      }

      BigDecimal remainingRefundable = payment.getRemainingRefundableAmount();
      return refundAmount.compareTo(remainingRefundable) <= 0;

    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  // ==================== PAYMENT EXPIRATION MANAGEMENT ====================

  @Override
  @Transactional
  public int processExpiredPayments() {
    log.info("Processing expired payments");

    int processedCount = paymentRepository.markExpiredPaymentsAsFailed("Payment expired");

    if (processedCount > 0) {
      log.info("Marked {} expired payments as failed", processedCount);

      // Also update associated orders
      List<Payment> expiredPayments = paymentRepository.findExpiredPayments();
      for (Payment payment : expiredPayments) {
        if (payment.isFailed()) {
          try {
            orderService.processOrderFailure(payment.getOrder().getId(), "Payment expired");
          } catch (Exception e) {
            log.error(
                "Failed to process order failure for expired payment: {}",
                payment.getPaymentReference(),
                e);
            // Continue processing other expired payments even if one fails
          }
        }
      }
    }

    return processedCount;
  }

  @Override
  public List<Payment> getPaymentsExpiringSoon(int withinMinutes) {
    log.debug("Getting payments expiring within {} minutes", withinMinutes);

    LocalDateTime threshold = LocalDateTime.now().plusMinutes(withinMinutes);
    return paymentRepository.findPaymentsExpiringSoon(threshold);
  }

  @Override
  @Transactional
  public Payment extendPaymentExpiration(UUID paymentId, int additionalMinutes) {
    log.info("Extending payment expiration: {} by {} minutes", paymentId, additionalMinutes);

    Payment payment = findById(paymentId);

    if (!payment.isPending() && !payment.isProcessing()) {
      log.error("Cannot extend expiration for payment in status: {}", payment.getStatus());
      throw new InvalidPaymentStatusException(
          "Cannot extend expiration for payment in status: " + payment.getStatus());
    }

    if (payment.getExpiresAt() != null) {
      payment.setExpirationTime(payment.getExpiresAt().plusMinutes(additionalMinutes));
    } else {
      payment.setExpirationTime(additionalMinutes);
    }

    Payment updatedPayment = paymentRepository.save(payment);

    log.info(
        "Successfully extended payment expiration: {} by {} minutes",
        payment.getPaymentReference(),
        additionalMinutes);

    return updatedPayment;
  }

  // ==================== PAYMENT SEARCH AND REPORTING ====================

  @Override
  public Page<Payment> getPaymentsByUser(User user, Pageable pageable) {
    log.debug("Getting payments for user: {} with pagination", user.getUsername());

    return paymentRepository.findByUser(user, pageable);
  }

  @Override
  public Page<Payment> getPaymentsByUsername(String username, Pageable pageable) {
    log.debug("Getting payments for username: {} with pagination", username);

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> {
                  log.error("User not found with username: {}", username);
                  return new ResourceNotFoundException("User not found with username: " + username);
                });

    return getPaymentsByUser(user, pageable);
  }

  @Override
  public Page<Payment> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
    log.debug("Getting payments by status: {} with pagination", status);

    return paymentRepository.findByStatus(status, pageable);
  }

  @Override
  public Page<Payment> searchPayments(
      String transactionId,
      String paymentReference,
      String username,
      PaymentStatus status,
      String paymentMethod,
      LocalDateTime startDate,
      LocalDateTime endDate,
      BigDecimal minAmount,
      BigDecimal maxAmount,
      Pageable pageable) {
    log.debug(
        "Searching payments with criteria - transactionId: {}, reference: {}, username: {}",
        transactionId,
        paymentReference,
        username);

    return paymentRepository.searchPayments(
        transactionId,
        paymentReference,
        username,
        status,
        paymentMethod,
        startDate,
        endDate,
        minAmount,
        maxAmount,
        pageable);
  }

  @Override
  public PaymentStatistics getPaymentStatistics() {
    log.debug("Getting payment statistics");

    Object[] stats = paymentRepository.getPaymentStatistics();

    long total = ((Number) stats[0]).longValue();
    long pending = ((Number) stats[1]).longValue();
    long processing = paymentRepository.countByStatus(PaymentStatus.PROCESSING);
    long completed = ((Number) stats[2]).longValue();
    long failed = ((Number) stats[3]).longValue();
    long cancelled = ((Number) stats[4]).longValue();
    long refunded = ((Number) stats[5]).longValue();

    BigDecimal totalRevenue = paymentRepository.calculateTotalRevenue();
    BigDecimal totalRefunded = paymentRepository.calculateTotalRefundedAmount();
    BigDecimal averagePaymentAmount = paymentRepository.calculateAveragePaymentAmount();

    double successRate = total > 0 ? (double) completed / total * 100 : 0.0;

    return new PaymentStatistics(
        total,
        pending,
        processing,
        completed,
        failed,
        cancelled,
        refunded,
        totalRevenue,
        totalRefunded,
        averagePaymentAmount,
        successRate);
  }

  @Override
  public PaymentStatistics getPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate) {
    log.debug("Getting payment statistics for period: {} to {}", startDate, endDate);

    long total = paymentRepository.countPaymentsBetweenDates(startDate, endDate);
    long pending = paymentRepository.countByStatus(PaymentStatus.PENDING);
    long processing = paymentRepository.countByStatus(PaymentStatus.PROCESSING);
    long completed = paymentRepository.countCompletedPaymentsBetweenDates(startDate, endDate);
    long failed = paymentRepository.countByStatus(PaymentStatus.FAILED);
    long cancelled = paymentRepository.countByStatus(PaymentStatus.CANCELLED);
    long refunded = paymentRepository.countByStatus(PaymentStatus.REFUNDED);

    BigDecimal totalRevenue = paymentRepository.calculateRevenueBetweenDates(startDate, endDate);
    BigDecimal totalRefunded =
        paymentRepository.calculateRefundedAmountBetweenDates(startDate, endDate);
    BigDecimal averagePaymentAmount =
        total > 0
            ? totalRevenue.divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    double successRate = total > 0 ? (double) completed / total * 100 : 0.0;

    return new PaymentStatistics(
        total,
        pending,
        processing,
        completed,
        failed,
        cancelled,
        refunded,
        totalRevenue,
        totalRefunded,
        averagePaymentAmount,
        successRate);
  }

  @Override
  public List<DailyPaymentStatistics> getDailyPaymentStatistics(
      LocalDateTime startDate, LocalDateTime endDate) {
    log.debug("Getting daily payment statistics for period: {} to {}", startDate, endDate);

    List<Object[]> dailyStats = paymentRepository.getDailyPaymentStatistics(startDate, endDate);

    return dailyStats.stream()
        .map(
            stat ->
                new DailyPaymentStatistics(
                    (LocalDateTime) stat[0],
                    ((Number) stat[1]).longValue(),
                    (BigDecimal) stat[2],
                    (BigDecimal) stat[3]))
        .toList();
  }

  @Override
  public List<PaymentMethodStatistics> getPaymentMethodStatistics() {
    log.debug("Getting payment method statistics");

    List<Object[]> methodStats = paymentRepository.getPaymentMethodStatistics();

    return methodStats.stream()
        .map(
            stat ->
                new PaymentMethodStatistics(
                    (String) stat[0],
                    ((Number) stat[1]).longValue(),
                    (BigDecimal) stat[2],
                    ((Number) stat[3]).doubleValue()))
        .toList();
  }

  @Override
  public List<Payment> getRecentPayments(int limit) {
    log.debug("Getting {} recent payments", limit);

    return paymentRepository.findRecentPayments(limit);
  }

  // ==================== PAYMENT UTILITY METHODS ====================

  @Override
  public BigDecimal calculateTotalRevenue() {
    log.debug("Calculating total revenue");

    return paymentRepository.calculateTotalRevenue();
  }

  @Override
  public BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate) {
    log.debug("Calculating revenue for period: {} to {}", startDate, endDate);

    return paymentRepository.calculateRevenueBetweenDates(startDate, endDate);
  }

  @Override
  public BigDecimal calculateTotalRefundedAmount() {
    log.debug("Calculating total refunded amount");

    return paymentRepository.calculateTotalRefundedAmount();
  }

  @Override
  public List<String> getSupportedPaymentMethods() {
    return List.copyOf(SUPPORTED_PAYMENT_METHODS);
  }

  @Override
  public int getDefaultExpirationMinutes() {
    return defaultExpirationMinutes;
  }
}
