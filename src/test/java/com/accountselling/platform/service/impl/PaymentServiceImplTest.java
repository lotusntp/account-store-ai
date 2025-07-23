package com.accountselling.platform.service.impl;

import com.accountselling.platform.dto.statistics.PaymentStatistics;
import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.enums.PaymentStatus;
import com.accountselling.platform.exception.*;
import com.accountselling.platform.model.*;
import com.accountselling.platform.repository.*;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.PaymentService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceImpl.
 * Tests comprehensive payment management operations including creation,
 * QR code generation, status management, payment gateway integration, and financial reporting
 * with mock dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Implementation Tests")
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
    private UUID testUserId;
    private UUID testOrderId;
    private UUID testPaymentId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
        testPaymentId = UUID.randomUUID();

        // Setup test user
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        // Setup test order
        testOrder = new Order();
        testOrder.setId(testOrderId);
        testOrder.setUser(testUser);
        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setOrderNumber("ORD-12345");

        // Setup test payment
        testPayment = new Payment();
        testPayment.setId(testPaymentId);
        testPayment.setOrder(testOrder);
        testPayment.setAmount(BigDecimal.valueOf(100.00));
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setPaymentMethod("QR_CODE");
        testPayment.setPaymentReference("PAY-12345");
        testPayment.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        // Set up configuration values
        ReflectionTestUtils.setField(paymentService, "defaultExpirationMinutes", 30);
        ReflectionTestUtils.setField(paymentService, "qrCodeBaseUrl", "https://payment-gateway.example.com/qr");
    }

    @Nested
    @DisplayName("Payment Creation Tests")
    class PaymentCreationTests {

        @Test
        @DisplayName("Create payment successfully with valid data")
        void createPayment_WithValidData_ShouldCreatePaymentSuccessfully() {
            // Given
            when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.createPayment(testOrder, "QR_CODE", 30);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrder()).isEqualTo(testOrder);
            assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
            assertThat(result.getPaymentMethod()).isEqualTo("QR_CODE");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);

            verify(paymentRepository).findByOrder(testOrder);
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Create payment with default expiration time")
        void createPayment_WithDefaultExpiration_ShouldUseDefaultTime() {
            // Given
            when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.createPayment(testOrder, "QR_CODE");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPaymentMethod()).isEqualTo("QR_CODE");

            verify(paymentRepository).findByOrder(testOrder);
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Create payment by order ID")
        void createPaymentByOrderId_WithValidOrderId_ShouldCreatePayment() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
            when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.createPaymentByOrderId(testOrderId, "QR_CODE", 30);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrder()).isEqualTo(testOrder);

            verify(orderRepository).findById(testOrderId);
            verify(paymentRepository).findByOrder(testOrder);
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Create payment with non-existent order should throw exception")
        void createPaymentByOrderId_WithNonExistentOrder_ShouldThrowException() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentService.createPaymentByOrderId(testOrderId, "QR_CODE", 30))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with ID");

            verify(orderRepository).findById(testOrderId);
            verifyNoInteractions(paymentRepository);
        }

        @Test
        @DisplayName("Create payment for order with existing payment should throw exception")
        void createPayment_WithExistingPayment_ShouldThrowException() {
            // Given
            when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.of(testPayment));

            // When & Then
            assertThatThrownBy(() -> paymentService.createPayment(testOrder, "QR_CODE", 30))
                .isInstanceOf(PaymentAlreadyExistsException.class)
                .hasMessageContaining("Payment already exists for order");

            verify(paymentRepository).findByOrder(testOrder);
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("Create payment with unsupported payment method should throw exception")
        void createPayment_WithUnsupportedPaymentMethod_ShouldThrowException() {
            // Given
            testOrder.setStatus(OrderStatus.PENDING);

            // When & Then
            assertThatThrownBy(() -> paymentService.createPayment(testOrder, "UNSUPPORTED_METHOD", 30))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Unsupported payment method");

            verifyNoInteractions(paymentRepository);
        }

        @Test
        @DisplayName("Create payment for completed order should throw exception")
        void createPayment_WithCompletedOrder_ShouldThrowException() {
            // Given
            testOrder.setStatus(OrderStatus.COMPLETED);

            // When & Then
            assertThatThrownBy(() -> paymentService.createPayment(testOrder, "QR_CODE", 30))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Cannot create payment for order in status");

            verifyNoInteractions(paymentRepository);
        }
    }

    @Nested
    @DisplayName("QR Code Management Tests")
    class QrCodeManagementTests {

        @Test
        @DisplayName("Generate QR code successfully")
        void generateQrCode_WithValidPayment_ShouldReturnQrCodeUrl() {
            // When
            String result = paymentService.generateQrCode(testPayment);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).contains("https://payment-gateway.example.com/qr");
            assertThat(result).contains("ref=" + testPayment.getPaymentReference());
            assertThat(result).contains("amount=100.00");
        }

        @Test
        @DisplayName("Regenerate QR code successfully")
        void regenerateQrCode_WithValidPayment_ShouldUpdateQrCode() {
            // Given
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.regenerateQrCode(testPaymentId);

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository).findById(testPaymentId);
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Regenerate QR code for completed payment should throw exception")
        void regenerateQrCode_WithCompletedPayment_ShouldThrowException() {
            // Given
            testPayment.setStatus(PaymentStatus.COMPLETED);
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When & Then
            assertThatThrownBy(() -> paymentService.regenerateQrCode(testPaymentId))
                .isInstanceOf(InvalidPaymentStatusException.class)
                .hasMessageContaining("Cannot regenerate QR code for payment in status");

            verify(paymentRepository).findById(testPaymentId);
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("Get QR code content successfully")
        void getQrCodeContent_WithValidPayment_ShouldReturnContent() {
            // Given
            testPayment.setQrCodeUrl("https://example.com/qr/test");
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When
            String result = paymentService.getQrCodeContent(testPaymentId);

            // Then
            assertThat(result).isEqualTo("https://example.com/qr/test");

            verify(paymentRepository).findById(testPaymentId);
        }

        @Test
        @DisplayName("Get QR code content for payment without QR code should throw exception")
        void getQrCodeContent_WithoutQrCode_ShouldThrowException() {
            // Given
            testPayment.setQrCodeUrl(null);
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When & Then
            assertThatThrownBy(() -> paymentService.getQrCodeContent(testPaymentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No QR code available for payment");

            verify(paymentRepository).findById(testPaymentId);
        }
    }

    @Nested
    @DisplayName("Payment Retrieval Tests")
    class PaymentRetrievalTests {

        @Test
        @DisplayName("Find payment by ID successfully")
        void findById_WithValidId_ShouldReturnPayment() {
            // Given
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When
            Payment result = paymentService.findById(testPaymentId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testPaymentId);

            verify(paymentRepository).findById(testPaymentId);
        }

        @Test
        @DisplayName("Find payment by non-existent ID should throw exception")
        void findById_WithNonExistentId_ShouldThrowException() {
            // Given
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentService.findById(testPaymentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found with ID");

            verify(paymentRepository).findById(testPaymentId);
        }

        @Test
        @DisplayName("Find payment by transaction ID successfully")
        void findByTransactionId_WithValidId_ShouldReturnPayment() {
            // Given
            String transactionId = "TXN-12345";
            when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(testPayment));

            // When
            Payment result = paymentService.findByTransactionId(transactionId);

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository).findByTransactionId(transactionId);
        }

        @Test
        @DisplayName("Find payment by payment reference successfully")
        void findByPaymentReference_WithValidReference_ShouldReturnPayment() {
            // Given
            when(paymentRepository.findByPaymentReference("PAY-12345")).thenReturn(Optional.of(testPayment));

            // When
            Payment result = paymentService.findByPaymentReference("PAY-12345");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPaymentReference()).isEqualTo("PAY-12345");

            verify(paymentRepository).findByPaymentReference("PAY-12345");
        }

        @Test
        @DisplayName("Find payment by order successfully")
        void findByOrder_WithValidOrder_ShouldReturnPayment() {
            // Given
            when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.of(testPayment));

            // When
            Payment result = paymentService.findByOrder(testOrder);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrder()).isEqualTo(testOrder);

            verify(paymentRepository).findByOrder(testOrder);
        }

        @Test
        @DisplayName("Find payment by order ID successfully")
        void findByOrderId_WithValidOrderId_ShouldReturnPayment() {
            // Given
            when(paymentRepository.findByOrderId(testOrderId)).thenReturn(Optional.of(testPayment));

            // When
            Payment result = paymentService.findByOrderId(testOrderId);

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository).findByOrderId(testOrderId);
        }
    }

    @Nested
    @DisplayName("Payment Status Management Tests")
    class PaymentStatusManagementTests {

        @Test
        @DisplayName("Mark payment as processing successfully")
        void markPaymentAsProcessing_WithValidPayment_ShouldUpdateStatus() {
            // Given
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.markPaymentAsProcessing(testPaymentId);

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository).findById(testPaymentId);
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Mark payment as completed successfully")
        void markPaymentAsCompleted_WithValidPayment_ShouldUpdateStatusAndCompleteOrder() {
            // Given
            testPayment.setStatus(PaymentStatus.PROCESSING);
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.markPaymentAsCompleted(testPaymentId, "TXN-12345", "Gateway response");

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository).findById(testPaymentId);
            verify(paymentRepository).save(any(Payment.class));
            verify(orderService).processOrderCompletion(testOrderId, "TXN-12345");
        }

        @Test
        @DisplayName("Mark payment as failed successfully")
        void markPaymentAsFailed_WithValidPayment_ShouldUpdateStatusAndFailOrder() {
            // Given
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.markPaymentAsFailed(testPaymentId, "Payment failed", "Gateway response");

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository).findById(testPaymentId);
            verify(paymentRepository).save(any(Payment.class));
            verify(orderService).processOrderFailure(testOrderId, "Payment failed");
        }

        @Test
        @DisplayName("Cancel payment successfully")
        void cancelPayment_WithValidPayment_ShouldUpdateStatusAndCancelOrder() {
            // Given
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.cancelPayment(testPaymentId, "User cancelled");

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository).findById(testPaymentId);
            verify(paymentRepository).save(any(Payment.class));
            verify(orderService).cancelOrder(testOrderId, "User cancelled");
        }

        @Test
        @DisplayName("Cancel payment with invalid status should throw exception")
        void cancelPayment_WithInvalidStatus_ShouldThrowException() {
            // Given
            testPayment.setStatus(PaymentStatus.COMPLETED);
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When & Then
            assertThatThrownBy(() -> paymentService.cancelPayment(testPaymentId, "Reason"))
                .isInstanceOf(InvalidPaymentStatusException.class)
                .hasMessageContaining("Payment cannot be cancelled in current status");

            verify(paymentRepository).findById(testPaymentId);
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("Process refund successfully")
        void processRefund_WithValidData_ShouldProcessRefund() {
            // Given
            testPayment.setStatus(PaymentStatus.COMPLETED);
            BigDecimal refundAmount = BigDecimal.valueOf(50.00);

            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.processRefund(testPaymentId, refundAmount, "Customer request");

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository, atLeastOnce()).findById(testPaymentId);
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Process refund with invalid status should throw exception")
        void processRefund_WithInvalidStatus_ShouldThrowException() {
            // Given
            testPayment.setStatus(PaymentStatus.PENDING);
            BigDecimal refundAmount = BigDecimal.valueOf(50.00);

            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When & Then
            assertThatThrownBy(() -> paymentService.processRefund(testPaymentId, refundAmount, "Reason"))
                .isInstanceOf(InvalidPaymentStatusException.class)
                .hasMessageContaining("Payment cannot be refunded in current status");

            verify(paymentRepository).findById(testPaymentId);
            verify(paymentRepository, never()).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("Payment Gateway Integration Tests")
    class PaymentGatewayIntegrationTests {

        @Test
        @DisplayName("Process webhook with completed status")
        void processWebhook_WithCompletedStatus_ShouldMarkAsCompleted() {
            // Given
            testPayment.setTransactionId("TXN-12345");
            when(paymentRepository.findByTransactionId("TXN-12345")).thenReturn(Optional.of(testPayment));
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.processWebhook("TXN-12345", "COMPLETED", "Gateway response");

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository).findByTransactionId("TXN-12345");
            verify(orderService).processOrderCompletion(testOrderId, "TXN-12345");
        }

        @Test
        @DisplayName("Process webhook with failed status")
        void processWebhook_WithFailedStatus_ShouldMarkAsFailed() {
            // Given
            testPayment.setTransactionId("TXN-12345");
            when(paymentRepository.findByTransactionId("TXN-12345")).thenReturn(Optional.of(testPayment));
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.processWebhook("TXN-12345", "FAILED", "Gateway response");

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository).findByTransactionId("TXN-12345");
            verify(orderService).processOrderFailure(testOrderId, "Payment failed via webhook");
        }

        @Test
        @DisplayName("Process webhook with unknown status should throw exception")
        void processWebhook_WithUnknownStatus_ShouldThrowException() {
            // Given
            testPayment.setTransactionId("TXN-12345");
            when(paymentRepository.findByTransactionId("TXN-12345")).thenReturn(Optional.of(testPayment));

            // When & Then
            assertThatThrownBy(() -> paymentService.processWebhook("TXN-12345", "UNKNOWN", "Gateway response"))
                .isInstanceOf(WebhookProcessingException.class)
                .hasMessageContaining("Unknown webhook status");

            verify(paymentRepository).findByTransactionId("TXN-12345");
        }

        @Test
        @DisplayName("Verify payment with gateway")
        void verifyPaymentWithGateway_WithValidPayment_ShouldVerifyAndUpdate() {
            // Given
            testPayment.setTransactionId("TXN-12345");
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.findByTransactionId("TXN-12345")).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.verifyPaymentWithGateway(testPaymentId);

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository, atLeastOnce()).findById(testPaymentId);
        }

        @Test
        @DisplayName("Check payment status with gateway")
        void checkPaymentStatusWithGateway_WithValidTransaction_ShouldReturnStatus() {
            // When
            String result = paymentService.checkPaymentStatusWithGateway("TXN-12345");

            // Then
            assertThat(result).isEqualTo("COMPLETED"); // Mocked response
        }
    }

    @Nested
    @DisplayName("Payment Validation Tests")
    class PaymentValidationTests {

        @Test
        @DisplayName("Validate payment with valid data should pass")
        void validatePayment_WithValidData_ShouldPass() {
            // Given
            testOrder.setStatus(OrderStatus.PENDING);

            // When & Then
            assertThatCode(() -> paymentService.validatePayment(testOrder, "QR_CODE"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Validate payment with null payment method should throw exception")
        void validatePayment_WithNullPaymentMethod_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> paymentService.validatePayment(testOrder, null))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Payment method cannot be null or empty");
        }

        @Test
        @DisplayName("Check if payment is expired")
        void isPaymentExpired_WithExpiredPayment_ShouldReturnTrue() {
            // Given
            testPayment.setExpiresAt(LocalDateTime.now().minusMinutes(10));
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When
            boolean result = paymentService.isPaymentExpired(testPaymentId);

            // Then
            assertThat(result).isTrue();

            verify(paymentRepository).findById(testPaymentId);
        }

        @Test
        @DisplayName("Check if payment can be cancelled")
        void canPaymentBeCancelled_WithPendingPayment_ShouldReturnTrue() {
            // Given
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When
            boolean result = paymentService.canPaymentBeCancelled(testPaymentId);

            // Then
            assertThat(result).isTrue();

            verify(paymentRepository).findById(testPaymentId);
        }

        @Test
        @DisplayName("Check if payment can be refunded")
        void canPaymentBeRefunded_WithCompletedPayment_ShouldReturnTrue() {
            // Given
            testPayment.setStatus(PaymentStatus.COMPLETED);
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When
            boolean result = paymentService.canPaymentBeRefunded(testPaymentId);

            // Then
            assertThat(result).isTrue();

            verify(paymentRepository).findById(testPaymentId);
        }

        @Test
        @DisplayName("Validate refund amount")
        void isRefundAmountValid_WithValidAmount_ShouldReturnTrue() {
            // Given
            testPayment.setStatus(PaymentStatus.COMPLETED);
            BigDecimal refundAmount = BigDecimal.valueOf(50.00);

            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When
            boolean result = paymentService.isRefundAmountValid(testPaymentId, refundAmount);

            // Then
            assertThat(result).isTrue();

            verify(paymentRepository).findById(testPaymentId);
        }

        @Test
        @DisplayName("Validate refund amount with invalid amount should return false")
        void isRefundAmountValid_WithInvalidAmount_ShouldReturnFalse() {
            // Given
            testPayment.setStatus(PaymentStatus.COMPLETED);
            BigDecimal refundAmount = BigDecimal.valueOf(150.00); // More than payment amount

            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When
            boolean result = paymentService.isRefundAmountValid(testPaymentId, refundAmount);

            // Then
            assertThat(result).isFalse();

            verify(paymentRepository).findById(testPaymentId);
        }
    }

    @Nested
    @DisplayName("Payment Expiration Management Tests")
    class PaymentExpirationManagementTests {

        @Test
        @DisplayName("Process expired payments")
        void processExpiredPayments_ShouldMarkExpiredPaymentsAsFailed() {
            // Given
            when(paymentRepository.markExpiredPaymentsAsFailed("Payment expired")).thenReturn(5);
            when(paymentRepository.findExpiredPayments()).thenReturn(Collections.emptyList());

            // When
            int result = paymentService.processExpiredPayments();

            // Then
            assertThat(result).isEqualTo(5);

            verify(paymentRepository).markExpiredPaymentsAsFailed("Payment expired");
            verify(paymentRepository).findExpiredPayments();
        }

        @Test
        @DisplayName("Get payments expiring soon")
        void getPaymentsExpiringSoon_WithValidThreshold_ShouldReturnPayments() {
            // Given
            List<Payment> expiringPayments = List.of(testPayment);
            when(paymentRepository.findPaymentsExpiringSoon(any(LocalDateTime.class))).thenReturn(expiringPayments);

            // When
            List<Payment> result = paymentService.getPaymentsExpiringSoon(10);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testPayment);

            verify(paymentRepository).findPaymentsExpiringSoon(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Extend payment expiration successfully")
        void extendPaymentExpiration_WithValidPayment_ShouldExtendExpiration() {
            // Given
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            // When
            Payment result = paymentService.extendPaymentExpiration(testPaymentId, 15);

            // Then
            assertThat(result).isNotNull();

            verify(paymentRepository).findById(testPaymentId);
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Extend payment expiration with invalid status should throw exception")
        void extendPaymentExpiration_WithInvalidStatus_ShouldThrowException() {
            // Given
            testPayment.setStatus(PaymentStatus.COMPLETED);
            when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));

            // When & Then
            assertThatThrownBy(() -> paymentService.extendPaymentExpiration(testPaymentId, 15))
                .isInstanceOf(InvalidPaymentStatusException.class)
                .hasMessageContaining("Cannot extend expiration for payment in status");

            verify(paymentRepository).findById(testPaymentId);
            verify(paymentRepository, never()).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("Payment Search and Reporting Tests")
    class PaymentSearchAndReportingTests {

        @Test
        @DisplayName("Get payments by user with pagination")
        void getPaymentsByUser_WithValidUser_ShouldReturnPagedPayments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Payment> payments = List.of(testPayment);
            Page<Payment> page = new PageImpl<>(payments, pageable, 1);

            when(paymentRepository.findByUser(testUser, pageable)).thenReturn(page);

            // When
            Page<Payment> result = paymentService.getPaymentsByUser(testUser, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testPayment);

            verify(paymentRepository).findByUser(testUser, pageable);
        }

        @Test
        @DisplayName("Get payments by username with pagination")
        void getPaymentsByUsername_WithValidUsername_ShouldReturnPagedPayments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Payment> payments = List.of(testPayment);
            Page<Payment> page = new PageImpl<>(payments, pageable, 1);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(paymentRepository.findByUser(testUser, pageable)).thenReturn(page);

            // When
            Page<Payment> result = paymentService.getPaymentsByUsername("testuser", pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(userRepository).findByUsername("testuser");
            verify(paymentRepository).findByUser(testUser, pageable);
        }

        @Test
        @DisplayName("Search payments with criteria")
        void searchPayments_WithCriteria_ShouldReturnFilteredPayments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Payment> payments = List.of(testPayment);
            Page<Payment> page = new PageImpl<>(payments, pageable, 1);

            when(paymentRepository.searchPayments(any(), any(), any(),
                any(), any(), any(),
                any(), any(), any(),
                eq(pageable))).thenReturn(page);

            // When
            Page<Payment> result = paymentService.searchPayments("TXN", "PAY", "testuser",
                PaymentStatus.PENDING, "QR_CODE", null, null, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(paymentRepository).searchPayments(any(), any(), any(),
                any(), any(), any(),
                any(), any(), any(),
                eq(pageable));
        }

        @Test
        @DisplayName("Get payment statistics")
        void getPaymentStatistics_ShouldReturnStatistics() {
            // Given
            Object[] stats = {10L, 5L, 3L, 1L, 1L, 0L}; // total, pending, completed, failed, cancelled, refunded
            when(paymentRepository.getPaymentStatistics()).thenReturn(stats);
            when(paymentRepository.countByStatus(PaymentStatus.PROCESSING)).thenReturn(2L);
            when(paymentRepository.calculateTotalRevenue()).thenReturn(BigDecimal.valueOf(1000.00));
            when(paymentRepository.calculateTotalRefundedAmount()).thenReturn(BigDecimal.valueOf(50.00));
            when(paymentRepository.calculateAveragePaymentAmount()).thenReturn(BigDecimal.valueOf(100.00));

            // When
            PaymentStatistics result = paymentService.getPaymentStatistics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(10L);
            assertThat(result.pending()).isEqualTo(5L);
            assertThat(result.completed()).isEqualTo(3L);

            verify(paymentRepository).getPaymentStatistics();
            verify(paymentRepository).calculateTotalRevenue();
        }

        @Test
        @DisplayName("Get recent payments")
        void getRecentPayments_WithLimit_ShouldReturnRecentPayments() {
            // Given
            List<Payment> payments = List.of(testPayment);
            when(paymentRepository.findRecentPayments(5)).thenReturn(payments);

            // When
            List<Payment> result = paymentService.getRecentPayments(5);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testPayment);

            verify(paymentRepository).findRecentPayments(5);
        }
    }

    @Nested
    @DisplayName("Payment Utility Methods Tests")
    class PaymentUtilityMethodsTests {

        @Test
        @DisplayName("Calculate total revenue")
        void calculateTotalRevenue_ShouldReturnTotalRevenue() {
            // Given
            BigDecimal expectedRevenue = BigDecimal.valueOf(5000.00);
            when(paymentRepository.calculateTotalRevenue()).thenReturn(expectedRevenue);

            // When
            BigDecimal result = paymentService.calculateTotalRevenue();

            // Then
            assertThat(result).isEqualByComparingTo(expectedRevenue);

            verify(paymentRepository).calculateTotalRevenue();
        }

        @Test
        @DisplayName("Calculate revenue within date range")
        void calculateRevenue_WithDateRange_ShouldReturnRevenueForPeriod() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);
            LocalDateTime endDate = LocalDateTime.now();
            BigDecimal expectedRevenue = BigDecimal.valueOf(1000.00);

            when(paymentRepository.calculateRevenueBetweenDates(startDate, endDate)).thenReturn(expectedRevenue);

            // When
            BigDecimal result = paymentService.calculateRevenue(startDate, endDate);

            // Then
            assertThat(result).isEqualByComparingTo(expectedRevenue);

            verify(paymentRepository).calculateRevenueBetweenDates(startDate, endDate);
        }

        @Test
        @DisplayName("Calculate total refunded amount")
        void calculateTotalRefundedAmount_ShouldReturnTotalRefunded() {
            // Given
            BigDecimal expectedRefunded = BigDecimal.valueOf(200.00);
            when(paymentRepository.calculateTotalRefundedAmount()).thenReturn(expectedRefunded);

            // When
            BigDecimal result = paymentService.calculateTotalRefundedAmount();

            // Then
            assertThat(result).isEqualByComparingTo(expectedRefunded);

            verify(paymentRepository).calculateTotalRefundedAmount();
        }

        @Test
        @DisplayName("Get supported payment methods")
        void getSupportedPaymentMethods_ShouldReturnSupportedMethods() {
            // When
            List<String> result = paymentService.getSupportedPaymentMethods();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).containsExactly("QR_CODE", "BANK_TRANSFER", "CREDIT_CARD", "MOBILE_BANKING");
        }

        @Test
        @DisplayName("Get default expiration minutes")
        void getDefaultExpirationMinutes_ShouldReturnDefaultValue() {
            // When
            int result = paymentService.getDefaultExpirationMinutes();

            // Then
            assertThat(result).isEqualTo(30);
        }
    }
}