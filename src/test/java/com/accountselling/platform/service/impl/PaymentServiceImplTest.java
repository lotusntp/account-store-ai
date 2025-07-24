package com.accountselling.platform.service.impl;

import com.accountselling.platform.dto.statistics.DailyPaymentStatistics;
import com.accountselling.platform.dto.statistics.PaymentMethodStatistics;
import com.accountselling.platform.dto.statistics.PaymentStatistics;
import com.accountselling.platform.enums.PaymentStatus;
import com.accountselling.platform.exception.*;
import com.accountselling.platform.model.*;
import com.accountselling.platform.repository.*;
import com.accountselling.platform.service.OrderService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User testUser;
    private Order testOrder;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");

        testOrder = new Order(testUser, BigDecimal.valueOf(100.00));
        testOrder.setId(UUID.randomUUID());

        testPayment = new Payment(testOrder, BigDecimal.valueOf(100.00), "QR_CODE");
        testPayment.setId(UUID.randomUUID());
        
        // Set default values using reflection
        ReflectionTestUtils.setField(paymentService, "defaultExpirationMinutes", 30);
        ReflectionTestUtils.setField(paymentService, "qrCodeBaseUrl", "https://payment-gateway.example.com/qr");
    }   
 // ==================== PAYMENT CREATION TESTS ====================

    @Test
    void createPayment_WithValidInput_ShouldCreatePayment() {
        // Arrange
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.createPayment(testOrder, "QR_CODE", 30);

        // Assert
        assertNotNull(result);
        assertEquals(testOrder, result.getOrder());
        assertEquals(BigDecimal.valueOf(100.00), result.getAmount());
        assertEquals("QR_CODE", result.getPaymentMethod());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_WithExistingPayment_ShouldThrowException() {
        // Arrange
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(PaymentAlreadyExistsException.class, 
                    () -> paymentService.createPayment(testOrder, "QR_CODE", 30));
    }

    @Test
    void createPayment_WithInvalidPaymentMethod_ShouldThrowException() {
        // Act & Assert
        assertThrows(InvalidPaymentException.class, 
                    () -> paymentService.createPayment(testOrder, "INVALID_METHOD", 30));
    }

    @Test
    void createPayment_WithDefaultExpiration_ShouldUseDefaultTime() {
        // Arrange
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.createPayment(testOrder, "QR_CODE");

        // Assert
        assertNotNull(result);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPaymentByOrderId_WithValidOrderId_ShouldCreatePayment() {
        // Arrange
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.createPaymentByOrderId(testOrder.getId(), "QR_CODE", 30);

        // Assert
        assertNotNull(result);
        assertEquals(testOrder, result.getOrder());
        verify(orderRepository).findById(testOrder.getId());
    }

    @Test
    void createPaymentByOrderId_WithInvalidOrderId_ShouldThrowException() {
        // Arrange
        UUID invalidOrderId = UUID.randomUUID();
        when(orderRepository.findById(invalidOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                    () -> paymentService.createPaymentByOrderId(invalidOrderId, "QR_CODE", 30));
    }

    // ==================== QR CODE MANAGEMENT TESTS ====================

    @Test
    void generateQrCode_WithValidPayment_ShouldReturnQrCodeUrl() {
        // Act
        String result = paymentService.generateQrCode(testPayment);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("payment-gateway.example.com"));
        assertTrue(result.contains(testPayment.getPaymentReference()));
    }

    @Test
    void regenerateQrCode_WithValidPayment_ShouldUpdateQrCode() {
        // Arrange
        testPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.regenerateQrCode(testPayment.getId());

        // Assert
        assertNotNull(result);
        assertNotNull(result.getQrCodeUrl());
        verify(paymentRepository).save(testPayment);
    }

    @Test
    void regenerateQrCode_WithCompletedPayment_ShouldThrowException() {
        // Arrange
        testPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(InvalidPaymentStatusException.class, 
                    () -> paymentService.regenerateQrCode(testPayment.getId()));
    }

    @Test
    void getQrCodeContent_WithValidPayment_ShouldReturnContent() {
        // Arrange
        testPayment.setQrCodeUrl("https://example.com/qr/123");
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act
        String result = paymentService.getQrCodeContent(testPayment.getId());

        // Assert
        assertEquals("https://example.com/qr/123", result);
    }

    @Test
    void getQrCodeContent_WithNoQrCode_ShouldThrowException() {
        // Arrange
        testPayment.setQrCodeUrl(null);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                    () -> paymentService.getQrCodeContent(testPayment.getId()));
    }

    // ==================== PAYMENT RETRIEVAL TESTS ====================

    @Test
    void findById_WithValidId_ShouldReturnPayment() {
        // Arrange
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act
        Payment result = paymentService.findById(testPayment.getId());

        // Assert
        assertNotNull(result);
        assertEquals(testPayment, result);
        verify(paymentRepository).findById(testPayment.getId());
    }

    @Test
    void findById_WithInvalidId_ShouldThrowException() {
        // Arrange
        UUID invalidId = UUID.randomUUID();
        when(paymentRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                    () -> paymentService.findById(invalidId));
    }

    @Test
    void findByTransactionId_WithValidTransactionId_ShouldReturnPayment() {
        // Arrange
        String transactionId = "TXN-123456";
        testPayment.setTransactionId(transactionId);
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(testPayment));

        // Act
        Payment result = paymentService.findByTransactionId(transactionId);

        // Assert
        assertNotNull(result);
        assertEquals(testPayment, result);
        verify(paymentRepository).findByTransactionId(transactionId);
    }

    @Test
    void findByOrder_WithValidOrder_ShouldReturnPayment() {
        // Arrange
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.of(testPayment));

        // Act
        Payment result = paymentService.findByOrder(testOrder);

        // Assert
        assertNotNull(result);
        assertEquals(testPayment, result);
        verify(paymentRepository).findByOrder(testOrder);
    }

    @Test
    void findByOrderId_WithValidOrderId_ShouldReturnPayment() {
        // Arrange
        when(paymentRepository.findByOrderId(testOrder.getId())).thenReturn(Optional.of(testPayment));

        // Act
        Payment result = paymentService.findByOrderId(testOrder.getId());

        // Assert
        assertNotNull(result);
        assertEquals(testPayment, result);
        verify(paymentRepository).findByOrderId(testOrder.getId());
    } 
   // ==================== PAYMENT STATUS MANAGEMENT TESTS ====================

    @Test
    void markPaymentAsProcessing_WithValidPayment_ShouldUpdateStatus() {
        // Arrange
        testPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.markPaymentAsProcessing(testPayment.getId());

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.PROCESSING, result.getStatus());
        verify(paymentRepository).save(testPayment);
    }

    @Test
    void markPaymentAsCompleted_WithValidPayment_ShouldUpdateStatusAndProcessOrder() {
        // Arrange
        testPayment.setStatus(PaymentStatus.PROCESSING);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.markPaymentAsCompleted(testPayment.getId(), "TXN-123", "Gateway response");

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertEquals("TXN-123", result.getTransactionId());
        verify(paymentRepository).save(testPayment);
        verify(orderService).processOrderCompletion(testOrder.getId(), "TXN-123");
    }

    @Test
    void markPaymentAsFailed_WithValidPayment_ShouldUpdateStatusAndProcessOrder() {
        // Arrange
        testPayment.setStatus(PaymentStatus.PROCESSING);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.markPaymentAsFailed(testPayment.getId(), "Payment failed", "Gateway response");

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertEquals("Payment failed", result.getFailureReason());
        verify(paymentRepository).save(testPayment);
        verify(orderService).processOrderFailure(testOrder.getId(), "Payment failed");
    }

    @Test
    void cancelPayment_WithValidPayment_ShouldUpdateStatusAndCancelOrder() {
        // Arrange
        testPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.cancelPayment(testPayment.getId(), "User cancelled");

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.CANCELLED, result.getStatus());
        verify(paymentRepository).save(testPayment);
        verify(orderService).cancelOrder(testOrder.getId(), "User cancelled");
    }

    @Test
    void cancelPayment_WithCompletedPayment_ShouldThrowException() {
        // Arrange
        testPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(InvalidPaymentStatusException.class, 
                    () -> paymentService.cancelPayment(testPayment.getId(), "User cancelled"));
    }

    @Test
    void processRefund_WithValidPayment_ShouldProcessRefund() {
        // Arrange
        testPayment.setStatus(PaymentStatus.COMPLETED);
        BigDecimal refundAmount = BigDecimal.valueOf(50.00);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.processRefund(testPayment.getId(), refundAmount, "Partial refund");

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
        assertEquals(refundAmount, result.getRefundAmount());
        verify(paymentRepository).save(testPayment);
    }

    @Test
    void processRefund_WithInvalidAmount_ShouldThrowException() {
        // Arrange
        testPayment.setStatus(PaymentStatus.COMPLETED);
        BigDecimal invalidRefundAmount = BigDecimal.valueOf(150.00); // More than payment amount
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(InvalidRefundException.class, 
                    () -> paymentService.processRefund(testPayment.getId(), invalidRefundAmount, "Invalid refund"));
    }

    // ==================== PAYMENT GATEWAY INTEGRATION TESTS ====================

    @Test
    void processWebhook_WithCompletedStatus_ShouldMarkPaymentAsCompleted() {
        // Arrange
        String transactionId = "TXN-123456";
        testPayment.setTransactionId(transactionId);
        testPayment.setStatus(PaymentStatus.PROCESSING);
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.processWebhook(transactionId, "COMPLETED", "Gateway response");

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(orderService).processOrderCompletion(testOrder.getId(), transactionId);
    }

    @Test
    void processWebhook_WithFailedStatus_ShouldMarkPaymentAsFailed() {
        // Arrange
        String transactionId = "TXN-123456";
        testPayment.setTransactionId(transactionId);
        testPayment.setStatus(PaymentStatus.PROCESSING);
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.processWebhook(transactionId, "FAILED", "Gateway response");

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(orderService).processOrderFailure(testOrder.getId(), "Payment failed via webhook");
    }

    @Test
    void processWebhook_WithUnknownStatus_ShouldThrowException() {
        // Arrange
        String transactionId = "TXN-123456";
        testPayment.setTransactionId(transactionId);
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(WebhookProcessingException.class, 
                    () -> paymentService.processWebhook(transactionId, "UNKNOWN", "Gateway response"));
    }

    @Test
    void verifyPaymentWithGateway_WithValidPayment_ShouldVerifyAndUpdate() {
        // Arrange
        testPayment.setTransactionId("TXN-123456");
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findByTransactionId(testPayment.getTransactionId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.verifyPaymentWithGateway(testPayment.getId());

        // Assert
        assertNotNull(result);
        // Note: The mock implementation returns "COMPLETED" status
        verify(orderService).processOrderCompletion(testOrder.getId(), testPayment.getTransactionId());
    }

    @Test
    void checkPaymentStatusWithGateway_WithValidTransactionId_ShouldReturnStatus() {
        // Act
        String result = paymentService.checkPaymentStatusWithGateway("TXN-123456");

        // Assert
        assertEquals("COMPLETED", result); // Mock implementation returns COMPLETED
    }  
  // ==================== PAYMENT VALIDATION TESTS ====================

    @Test
    void validatePayment_WithValidInput_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> paymentService.validatePayment(testOrder, "QR_CODE"));
    }

    @Test
    void validatePayment_WithInvalidPaymentMethod_ShouldThrowException() {
        // Act & Assert
        assertThrows(InvalidPaymentException.class, 
                    () -> paymentService.validatePayment(testOrder, "INVALID_METHOD"));
    }

    @Test
    void validatePayment_WithNullPaymentMethod_ShouldThrowException() {
        // Act & Assert
        assertThrows(InvalidPaymentException.class, 
                    () -> paymentService.validatePayment(testOrder, null));
    }

    @Test
    void validatePayment_WithCompletedOrder_ShouldThrowException() {
        // Arrange
        testOrder.setStatus(com.accountselling.platform.enums.OrderStatus.COMPLETED);

        // Act & Assert
        assertThrows(InvalidPaymentException.class, 
                    () -> paymentService.validatePayment(testOrder, "QR_CODE"));
    }

    @Test
    void validatePayment_WithZeroAmount_ShouldThrowException() {
        // Arrange
        testOrder.setTotalAmount(BigDecimal.ZERO);

        // Act & Assert
        assertThrows(InvalidPaymentException.class, 
                    () -> paymentService.validatePayment(testOrder, "QR_CODE"));
    }

    @Test
    void isPaymentExpired_WithExpiredPayment_ShouldReturnTrue() {
        // Arrange
        testPayment.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act
        boolean result = paymentService.isPaymentExpired(testPayment.getId());

        // Assert
        assertTrue(result);
    }

    @Test
    void isPaymentExpired_WithValidPayment_ShouldReturnFalse() {
        // Arrange
        testPayment.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act
        boolean result = paymentService.isPaymentExpired(testPayment.getId());

        // Assert
        assertFalse(result);
    }

    @Test
    void canPaymentBeCancelled_WithPendingPayment_ShouldReturnTrue() {
        // Arrange
        testPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act
        boolean result = paymentService.canPaymentBeCancelled(testPayment.getId());

        // Assert
        assertTrue(result);
    }

    @Test
    void canPaymentBeRefunded_WithCompletedPayment_ShouldReturnTrue() {
        // Arrange
        testPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act
        boolean result = paymentService.canPaymentBeRefunded(testPayment.getId());

        // Assert
        assertTrue(result);
    }

    @Test
    void isRefundAmountValid_WithValidAmount_ShouldReturnTrue() {
        // Arrange
        testPayment.setStatus(PaymentStatus.COMPLETED);
        BigDecimal refundAmount = BigDecimal.valueOf(50.00);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act
        boolean result = paymentService.isRefundAmountValid(testPayment.getId(), refundAmount);

        // Assert
        assertTrue(result);
    }

    @Test
    void isRefundAmountValid_WithExcessiveAmount_ShouldReturnFalse() {
        // Arrange
        testPayment.setStatus(PaymentStatus.COMPLETED);
        BigDecimal refundAmount = BigDecimal.valueOf(150.00); // More than payment amount
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act
        boolean result = paymentService.isRefundAmountValid(testPayment.getId(), refundAmount);

        // Assert
        assertFalse(result);
    }

    // ==================== PAYMENT EXPIRATION MANAGEMENT TESTS ====================

    @Test
    void processExpiredPayments_WithExpiredPayments_ShouldMarkAsFailed() {
        // Arrange
        testPayment.setStatus(PaymentStatus.FAILED); // Set status to FAILED to simulate expired payment
        List<Payment> expiredPayments = List.of(testPayment);
        when(paymentRepository.markExpiredPaymentsAsFailed("Payment expired")).thenReturn(1);
        when(paymentRepository.findExpiredPayments()).thenReturn(expiredPayments);

        // Act
        int result = paymentService.processExpiredPayments();

        // Assert
        assertEquals(1, result);
        verify(paymentRepository).markExpiredPaymentsAsFailed("Payment expired");
        verify(orderService).processOrderFailure(testOrder.getId(), "Payment expired");
    }

    @Test
    void getPaymentsExpiringSoon_WithThreshold_ShouldReturnPayments() {
        // Arrange
        List<Payment> expiringSoonPayments = List.of(testPayment);
        when(paymentRepository.findPaymentsExpiringSoon(any(LocalDateTime.class))).thenReturn(expiringSoonPayments);

        // Act
        List<Payment> result = paymentService.getPaymentsExpiringSoon(15);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPayment, result.get(0));
    }

    @Test
    void extendPaymentExpiration_WithValidPayment_ShouldExtendExpiration() {
        // Arrange
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.extendPaymentExpiration(testPayment.getId(), 15);

        // Assert
        assertNotNull(result);
        verify(paymentRepository).save(testPayment);
    }

    @Test
    void extendPaymentExpiration_WithCompletedPayment_ShouldThrowException() {
        // Arrange
        testPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(testPayment.getId())).thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(InvalidPaymentStatusException.class, 
                    () -> paymentService.extendPaymentExpiration(testPayment.getId(), 15));
    }

    // ==================== PAYMENT SEARCH AND REPORTING TESTS ====================

    @Test
    void getPaymentsByUser_WithValidUser_ShouldReturnPayments() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Payment> payments = List.of(testPayment);
        Page<Payment> paymentPage = new PageImpl<>(payments, pageable, 1);
        
        when(paymentRepository.findByUser(testUser, pageable)).thenReturn(paymentPage);

        // Act
        Page<Payment> result = paymentService.getPaymentsByUser(testUser, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testPayment, result.getContent().get(0));
    }

    @Test
    void getPaymentsByUsername_WithValidUsername_ShouldReturnPayments() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Payment> payments = List.of(testPayment);
        Page<Payment> paymentPage = new PageImpl<>(payments, pageable, 1);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByUser(testUser, pageable)).thenReturn(paymentPage);

        // Act
        Page<Payment> result = paymentService.getPaymentsByUsername("testuser", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getPaymentsByStatus_WithValidStatus_ShouldReturnPayments() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Payment> payments = List.of(testPayment);
        Page<Payment> paymentPage = new PageImpl<>(payments, pageable, 1);
        
        when(paymentRepository.findByStatus(PaymentStatus.PENDING, pageable)).thenReturn(paymentPage);

        // Act
        Page<Payment> result = paymentService.getPaymentsByStatus(PaymentStatus.PENDING, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchPayments_WithCriteria_ShouldReturnMatchingPayments() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Payment> payments = List.of(testPayment);
        Page<Payment> paymentPage = new PageImpl<>(payments, pageable, 1);
        
        when(paymentRepository.searchPayments(anyString(), anyString(), anyString(),
                any(PaymentStatus.class), anyString(), any(LocalDateTime.class),
                any(LocalDateTime.class), any(BigDecimal.class), any(BigDecimal.class),
                eq(pageable))).thenReturn(paymentPage);

        // Act
        Page<Payment> result = paymentService.searchPayments("TXN", "PAY", "testuser",
                PaymentStatus.PENDING, "QR_CODE", LocalDateTime.now().minusDays(1),
                LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.valueOf(1000), pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPaymentStatistics_ShouldReturnStatistics() {
        // Arrange
        Object[] stats = {10L, 3L, 5L, 2L, 1L, 1L}; // total, pending, completed, failed, cancelled, refunded
        when(paymentRepository.getPaymentStatistics()).thenReturn(stats);
        when(paymentRepository.countByStatus(PaymentStatus.PROCESSING)).thenReturn(1L);
        when(paymentRepository.calculateTotalRevenue()).thenReturn(BigDecimal.valueOf(1000));

        // Act
        PaymentStatistics result = paymentService.getPaymentStatistics();

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.total());
        assertEquals(3L, result.pending());
        assertEquals(5L, result.completed());
        assertEquals(2L, result.failed());
    }

    @Test
    void getRecentPayments_ShouldReturnRecentPayments() {
        // Arrange
        List<Payment> recentPayments = List.of(testPayment);
        when(paymentRepository.findRecentPayments(10)).thenReturn(recentPayments);

        // Act
        List<Payment> result = paymentService.getRecentPayments(10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPayment, result.get(0));
    }

    // ==================== PAYMENT UTILITY METHODS TESTS ====================

    @Test
    void calculateTotalRevenue_ShouldReturnTotalRevenue() {
        // Arrange
        when(paymentRepository.calculateTotalRevenue()).thenReturn(BigDecimal.valueOf(5000));

        // Act
        BigDecimal result = paymentService.calculateTotalRevenue();

        // Assert
        assertEquals(BigDecimal.valueOf(5000), result);
        verify(paymentRepository).calculateTotalRevenue();
    }

    @Test
    void calculateRevenue_WithDateRange_ShouldReturnRevenue() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        when(paymentRepository.calculateRevenueBetweenDates(startDate, endDate)).thenReturn(BigDecimal.valueOf(1000));

        // Act
        BigDecimal result = paymentService.calculateRevenue(startDate, endDate);

        // Assert
        assertEquals(BigDecimal.valueOf(1000), result);
        verify(paymentRepository).calculateRevenueBetweenDates(startDate, endDate);
    }

    @Test
    void calculateTotalRefundedAmount_ShouldReturnRefundedAmount() {
        // Arrange
        when(paymentRepository.calculateTotalRefundedAmount()).thenReturn(BigDecimal.valueOf(200));

        // Act
        BigDecimal result = paymentService.calculateTotalRefundedAmount();

        // Assert
        assertEquals(BigDecimal.valueOf(200), result);
        verify(paymentRepository).calculateTotalRefundedAmount();
    }

    @Test
    void getSupportedPaymentMethods_ShouldReturnSupportedMethods() {
        // Act
        List<String> result = paymentService.getSupportedPaymentMethods();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("QR_CODE"));
        assertTrue(result.contains("BANK_TRANSFER"));
        assertTrue(result.contains("CREDIT_CARD"));
        assertTrue(result.contains("MOBILE_BANKING"));
    }

    @Test
    void getDefaultExpirationMinutes_ShouldReturnDefaultValue() {
        // Act
        int result = paymentService.getDefaultExpirationMinutes();

        // Assert
        assertEquals(30, result);
    }
}