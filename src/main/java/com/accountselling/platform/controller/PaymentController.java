package com.accountselling.platform.controller;

import com.accountselling.platform.dto.payment.PaymentCreateRequestDto;
import com.accountselling.platform.dto.payment.PaymentResponseDto;
import com.accountselling.platform.exception.PaymentException;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.exception.WebhookProcessingException;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.Payment;
import com.accountselling.platform.model.User;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.PaymentService;
import com.accountselling.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for payment operations. Handles payment creation with QR code generation, payment
 * status checking, and payment gateway webhook processing.
 *
 * <p>Controller สำหรับจัดการการชำระเงิน รวมถึงการสร้าง QR Code การตรวจสอบสถานะ และการรับ webhook
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment processing and QR code generation endpoints")
public class PaymentController {

  private final PaymentService paymentService;
  private final OrderService orderService;
  private final UserService userService;

  /**
   * Generate payment QR code for an order. Creates a new payment record and generates QR code for
   * payment processing by external gateway.
   *
   * @param request the payment creation request containing order ID and payment method
   * @return payment response with QR code URL and payment details
   */
  @PostMapping("/generate")
  @PreAuthorize("hasRole('USER')")
  @Operation(
      summary = "Generate payment QR code",
      description = "Create payment and generate QR code for order payment processing")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Payment created successfully with QR code"),
        @ApiResponse(responseCode = "400", description = "Invalid payment request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - order does not belong to user"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "409", description = "Payment already exists for order")
      })
  public ResponseEntity<PaymentResponseDto> generatePayment(
      @Valid @RequestBody PaymentCreateRequestDto request) {
    log.info("Processing payment generation request for order: {}", request.orderId());

    User currentUser = getCurrentAuthenticatedUser();

    // Verify order exists and belongs to current user
    Order order = orderService.findById(request.orderId());

    if (!order.getUser().getId().equals(currentUser.getId())) {
      log.warn(
          "User {} attempted to create payment for order {} belonging to another user",
          currentUser.getUsername(),
          request.orderId());
      throw new PaymentException("Cannot create payment for order that doesn't belong to you");
    }

    // Create payment with QR code
    Payment payment =
        paymentService.createPayment(
            order, request.getNormalizedPaymentMethod(), request.getEffectiveExpirationMinutes());

    PaymentResponseDto response = convertToPaymentResponseDto(payment);

    log.info(
        "Payment generated successfully - ID: {}, Order: {}, User: {}",
        payment.getId(),
        request.orderId(),
        currentUser.getUsername());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Check payment status by payment ID. Returns current payment status and details for tracking
   * payment progress.
   *
   * @param paymentId the payment ID to check status for
   * @return payment response with current status and details
   */
  @GetMapping("/status/{paymentId}")
  @PreAuthorize("hasRole('USER')")
  @Operation(
      summary = "Check payment status",
      description = "Get current payment status and details by payment ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Payment status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - payment does not belong to user"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
      })
  public ResponseEntity<PaymentResponseDto> getPaymentStatus(@PathVariable UUID paymentId) {
    log.info("Processing payment status request for payment: {}", paymentId);

    User currentUser = getCurrentAuthenticatedUser();

    // Get payment and verify ownership
    Payment payment = paymentService.findById(paymentId);

    if (!payment.getOrder().getUser().getId().equals(currentUser.getId())) {
      log.warn(
          "User {} attempted to check status of payment {} belonging to another user",
          currentUser.getUsername(),
          paymentId);
      throw new PaymentException("Cannot access payment that doesn't belong to you");
    }

    PaymentResponseDto response = convertToPaymentResponseDto(payment);

    log.info(
        "Payment status retrieved successfully - ID: {}, Status: {}, User: {}",
        paymentId,
        payment.getStatus(),
        currentUser.getUsername());

    return ResponseEntity.ok(response);
  }

  /**
   * Process payment gateway webhook notifications. Handles incoming webhook calls from payment
   * gateway to update payment status when payments are processed.
   *
   * <p>Note: This endpoint does not require authentication as it's called by external gateway.
   *
   * @param webhookData the webhook data from payment gateway
   * @return confirmation response for webhook processing
   */
  @PostMapping("/webhook")
  @Operation(
      summary = "Process payment webhook",
      description = "Handle payment gateway webhook notifications for status updates")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid webhook data"),
        @ApiResponse(responseCode = "404", description = "Payment not found for webhook"),
        @ApiResponse(responseCode = "500", description = "Webhook processing failed")
      })
  public ResponseEntity<Map<String, String>> processWebhook(
      @RequestBody Map<String, Object> webhookData) {
    log.info("Processing payment webhook: {}", webhookData);

    try {
      // Extract required fields from webhook data
      String transactionId = extractStringField(webhookData, "transaction_id");
      String status = extractStringField(webhookData, "status");
      String gatewayResponse = webhookData.toString();

      if (transactionId == null || transactionId.trim().isEmpty()) {
        log.error("Webhook missing required transaction_id field");
        throw new WebhookProcessingException("Missing required field: transaction_id");
      }

      if (status == null || status.trim().isEmpty()) {
        log.error("Webhook missing required status field");
        throw new WebhookProcessingException("Missing required field: status");
      }

      // Process webhook with payment service
      Payment updatedPayment =
          paymentService.processWebhook(transactionId, status, gatewayResponse);

      Map<String, String> response = new HashMap<>();
      response.put("status", "success");
      response.put("message", "Webhook processed successfully");
      response.put("paymentId", updatedPayment.getId().toString());
      response.put("paymentStatus", updatedPayment.getStatus().toString());
      response.put(
          "processedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

      log.info(
          "Webhook processed successfully - Transaction: {}, Payment: {}, Status: {}",
          transactionId,
          updatedPayment.getId(),
          updatedPayment.getStatus());

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Failed to process webhook: {}", e.getMessage(), e);

      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("status", "error");
      errorResponse.put("message", "Failed to process webhook: " + e.getMessage());
      errorResponse.put(
          "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  /**
   * Get current authenticated user from security context.
   *
   * @return the current authenticated user
   * @throws ResourceNotFoundException if user is not authenticated or not found
   */
  private User getCurrentAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResourceNotFoundException("User is not authenticated");
    }

    String username = authentication.getName();
    return userService
        .findByUsername(username)
        .orElseThrow(
            () -> new ResourceNotFoundException("Authenticated user not found: " + username));
  }

  /**
   * Convert Payment entity to PaymentResponseDto.
   *
   * @param payment the payment entity to convert
   * @return the converted payment response DTO
   */
  private PaymentResponseDto convertToPaymentResponseDto(Payment payment) {
    Order order = payment.getOrder();

    PaymentResponseDto.OrderSummaryDto orderSummary =
        new PaymentResponseDto.OrderSummaryDto(
            order.getId(),
            order.getOrderNumber(),
            order.getUser().getUsername(),
            order.getTotalAmount(),
            "$" + order.getTotalAmount().toString(),
            order.getStatus().toString(),
            order.getCreatedAt());

    return new PaymentResponseDto(
        payment.getId(),
        payment.getPaymentReference(),
        payment.getTransactionId(),
        payment.getAmount(),
        "$" + payment.getAmount().toString(),
        payment.getStatus(),
        payment.getStatus().getDisplayName(),
        payment.getPaymentMethod(),
        payment.getQrCodeUrl(),
        payment.getPaidAt(),
        payment.getExpiresAt(),
        payment.getFailureReason(),
        payment.getRefundAmount(),
        payment.getRefundAmount() != null ? "$" + payment.getRefundAmount().toString() : null,
        payment.getRefundedAt(),
        payment.getNotes(),
        payment.getCreatedAt(),
        payment.getUpdatedAt(),
        orderSummary);
  }

  /**
   * Extract string field from webhook data map.
   *
   * @param data the webhook data map
   * @param fieldName the field name to extract
   * @return the string value or null if not found
   */
  private String extractStringField(Map<String, Object> data, String fieldName) {
    Object value = data.get(fieldName);
    return value != null ? value.toString() : null;
  }
}
