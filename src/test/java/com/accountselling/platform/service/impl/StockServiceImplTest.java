package com.accountselling.platform.service.impl;

import com.accountselling.platform.exception.*;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.repository.ProductRepository;
import com.accountselling.platform.repository.StockRepository;
import com.accountselling.platform.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StockServiceImpl.
 * Tests comprehensive stock management operations including reservations,
 * sales tracking, and inventory management with mock dependencies.
 * 
 * การทดสอบ unit สำหรับ StockServiceImpl
 * ทดสอบการจัดการสต็อกครบครัน รวมถึงการจอง การติดตามการขาย 
 * และการจัดการสต็อกด้วย mock dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockService Implementation Tests")
class StockServiceImplTest {

    @Mock
    private StockRepository stockRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private StockServiceImpl stockService;
    
    private Product testProduct;
    private Stock testStock;
    private UUID productId;
    private UUID stockId;
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        // สร้างข้อมูลทดสอบ - Setup test data
        productId = UUID.randomUUID();
        stockId = UUID.randomUUID();
        
        testCategory = new Category();
        testCategory.setId(UUID.randomUUID());
        testCategory.setName("Test Category");
        testCategory.setDescription("Category for testing");
        
        testProduct = new Product("Test Product", BigDecimal.valueOf(100.00), testCategory);
        testProduct.setId(productId);
        testProduct.setDescription("Test product description");
        testProduct.setLowStockThreshold(5);
        
        testStock = new Stock(testProduct, "encrypted_credentials_123");
        testStock.setId(stockId);
        testStock.setAdditionalInfo("Test additional info");
    }
    
    // ==================== CREATE STOCK TESTS ====================
    
    @Test
    @DisplayName("ควรสร้างสต็อกใหม่ได้สำเร็จ - Should create new stock successfully")
    void shouldCreateStockSuccessfully() {
        // Given - เตรียมข้อมูล
        String credentials = "test_credentials";
        String additionalInfo = "test_additional_info";
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.existsByProductIdAndCredentials(productId, credentials)).thenReturn(false);
        when(stockRepository.save(any(Stock.class))).thenReturn(testStock);
        
        // When - เรียกใช้เมธอด
        Stock result = stockService.createStock(productId, credentials, additionalInfo);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(stockId);
        assertThat(result.getCredentials()).isEqualTo("encrypted_credentials_123");
        
        verify(productRepository).findById(productId);
        verify(stockRepository).existsByProductIdAndCredentials(productId, credentials);
        verify(stockRepository).save(any(Stock.class));
    }
    
    @Test
    @DisplayName("ควรโยน ResourceNotFoundException เมื่อไม่พบสินค้า - Should throw ResourceNotFoundException when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        // Given - เตรียมข้อมูล
        String credentials = "test_credentials";
        
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        
        // When & Then - เรียกใช้และตรวจสอบ exception
        assertThatThrownBy(() -> stockService.createStock(productId, credentials, null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with ID: " + productId);
        
        verify(productRepository).findById(productId);
        verify(stockRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("ควรโยน ResourceAlreadyExistsException เมื่อ credentials ซ้ำ - Should throw ResourceAlreadyExistsException when credentials duplicate")
    void shouldThrowExceptionWhenCredentialsDuplicate() {
        // Given - เตรียมข้อมูล
        String credentials = "duplicate_credentials";
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.existsByProductIdAndCredentials(productId, credentials)).thenReturn(true);
        
        // When & Then - เรียกใช้และตรวจสอบ exception
        assertThatThrownBy(() -> stockService.createStock(productId, credentials, null))
            .isInstanceOf(ResourceAlreadyExistsException.class)
            .hasMessageContaining("Credentials already exist for this product");
        
        verify(productRepository).findById(productId);
        verify(stockRepository).existsByProductIdAndCredentials(productId, credentials);
        verify(stockRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("ควรสร้างสต็อกจำนวนมากได้สำเร็จ - Should create bulk stock successfully")
    void shouldCreateBulkStockSuccessfully() {
        // Given - เตรียมข้อมูล
        List<String> credentialsList = Arrays.asList("cred1", "cred2", "cred3");
        List<Stock> expectedStocks = Arrays.asList(
            new Stock(testProduct, "cred1"),
            new Stock(testProduct, "cred2"),
            new Stock(testProduct, "cred3")
        );
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.existsByProductIdAndCredentials(eq(productId), anyString())).thenReturn(false);
        when(stockRepository.saveAll(anyList())).thenReturn(expectedStocks);
        
        // When - เรียกใช้เมธอด
        List<Stock> result = stockService.createBulkStock(productId, credentialsList);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Stock::getCredentials).containsExactly("cred1", "cred2", "cred3");
        
        verify(productRepository).findById(productId);
        verify(stockRepository, times(3)).existsByProductIdAndCredentials(eq(productId), anyString());
        verify(stockRepository).saveAll(anyList());
    }
    
    // ==================== STOCK AVAILABILITY TESTS ====================
    
    @Test
    @DisplayName("ควรคืนจำนวนสต็อกที่มีจำหน่ายได้ถูกต้อง - Should return correct available stock count")
    void shouldReturnCorrectAvailableStockCount() {
        // Given - เตรียมข้อมูล
        long expectedCount = 10L;
        
        when(stockRepository.countAvailableByProductId(productId)).thenReturn(expectedCount);
        
        // When - เรียกใช้เมธอด
        long result = stockService.getAvailableStockCount(productId);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isEqualTo(expectedCount);
        
        verify(stockRepository).countAvailableByProductId(productId);
    }
    
    @Test
    @DisplayName("ควรคืน true เมื่อมีสต็อก - Should return true when in stock")
    void shouldReturnTrueWhenInStock() {
        // Given - เตรียมข้อมูล
        when(stockRepository.countAvailableByProductId(productId)).thenReturn(5L);
        
        // When - เรียกใช้เมธอด
        boolean result = stockService.isInStock(productId);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isTrue();
        
        verify(stockRepository).countAvailableByProductId(productId);
    }
    
    @Test
    @DisplayName("ควรคืน false เมื่อหมดสต็อก - Should return false when out of stock")
    void shouldReturnFalseWhenOutOfStock() {
        // Given - เตรียมข้อมูล
        when(stockRepository.countAvailableByProductId(productId)).thenReturn(0L);
        
        // When - เรียกใช้เมธอด
        boolean result = stockService.isInStock(productId);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isFalse();
        
        verify(stockRepository).countAvailableByProductId(productId);
    }
    
    @Test
    @DisplayName("ควรดึงสต็อกรายการแรกที่มีจำหน่ายได้ - Should get first available stock")
    void shouldGetFirstAvailableStock() {
        // Given - เตรียมข้อมูล
        when(stockRepository.findFirstAvailableByProductId(productId)).thenReturn(Optional.of(testStock));
        
        // When - เรียกใช้เมธอด
        Optional<Stock> result = stockService.getFirstAvailableStock(productId);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testStock);
        
        verify(stockRepository).findFirstAvailableByProductId(productId);
    }
    
    // ==================== STOCK RESERVATION TESTS ====================
    
    @Test
    @DisplayName("ควรจองสต็อกได้สำเร็จ - Should reserve stock successfully")
    void shouldReserveStockSuccessfully() {
        // Given - เตรียมข้อมูล
        int quantity = 2;
        int reservationMinutes = 15;
        List<Stock> availableStocks = Arrays.asList(
            new Stock(testProduct, "cred1"),
            new Stock(testProduct, "cred2"),
            new Stock(testProduct, "cred3")
        );
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.clearExpiredReservations()).thenReturn(0);
        when(stockRepository.findAvailableStockByProductId(productId)).thenReturn(availableStocks);
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When - เรียกใช้เมธอด
        List<Stock> result = stockService.reserveStock(productId, quantity, reservationMinutes);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).hasSize(quantity);
        assertThat(result).allMatch(stock -> stock.getReservedUntil() != null);
        
        verify(productRepository).findById(productId);
        verify(stockRepository).clearExpiredReservations();
        verify(stockRepository).findAvailableStockByProductId(productId);
        verify(stockRepository, times(quantity)).save(any(Stock.class));
    }
    
    @Test
    @DisplayName("ควรโยน OutOfStockException เมื่อสต็อกไม่เพียงพอ - Should throw OutOfStockException when insufficient stock")
    void shouldThrowOutOfStockExceptionWhenInsufficientStock() {
        // Given - เตรียมข้อมูล
        int quantity = 5;
        int reservationMinutes = 15;
        List<Stock> availableStocks = Arrays.asList(
            new Stock(testProduct, "cred1"),
            new Stock(testProduct, "cred2")
        ); // มีเพียง 2 รายการ แต่ต้องการ 5 รายการ
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.clearExpiredReservations()).thenReturn(0);
        when(stockRepository.findAvailableStockByProductId(productId)).thenReturn(availableStocks);
        
        // When & Then - เรียกใช้และตรวจสอบ exception
        assertThatThrownBy(() -> stockService.reserveStock(productId, quantity, reservationMinutes))
            .isInstanceOf(OutOfStockException.class)
            .hasMessageContaining("Insufficient stock available. Required: 5, Available: 2");
        
        verify(productRepository).findById(productId);
        verify(stockRepository).clearExpiredReservations();
        verify(stockRepository).findAvailableStockByProductId(productId);
        verify(stockRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("ควรยกเลิกการจองได้สำเร็จ - Should release reservation successfully")
    void shouldReleaseReservationSuccessfully() {
        // Given - เตรียมข้อมูล
        testStock.reserve(15); // จองสต็อกไว้ก่อน
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        when(stockRepository.save(testStock)).thenReturn(testStock);
        
        // When - เรียกใช้เมธอด
        boolean result = stockService.releaseReservation(stockId);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isTrue();
        assertThat(testStock.getReservedUntil()).isNull();
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository).save(testStock);
    }
    
    @Test
    @DisplayName("ควรคืน false เมื่อยกเลิกการจองที่ไม่ได้จองอยู่ - Should return false when releasing non-reserved stock")
    void shouldReturnFalseWhenReleasingNonReservedStock() {
        // Given - เตรียมข้อมูล (สต็อกที่ไม่ได้จอง)
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        
        // When - เรียกใช้เมธอด
        boolean result = stockService.releaseReservation(stockId);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isFalse();
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository, never()).save(any());
    }
    
    // ==================== STOCK SALES TESTS ====================
    
    @Test
    @DisplayName("ควรทำเครื่องหมายขายแล้วได้สำเร็จ - Should mark as sold successfully")
    void shouldMarkAsSoldSuccessfully() {
        // Given - เตรียมข้อมูล
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        when(stockRepository.save(testStock)).thenReturn(testStock);
        
        // When - เรียกใช้เมธอด
        Stock result = stockService.markAsSold(stockId);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isNotNull();
        assertThat(result.getSold()).isTrue();
        assertThat(result.getSoldAt()).isNotNull();
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository).save(testStock);
    }
    
    @Test
    @DisplayName("ควรโยน StockException เมื่อทำเครื่องหมายขายแล้วซ้ำ - Should throw StockException when marking already sold stock")
    void shouldThrowExceptionWhenMarkingAlreadySoldStock() {
        // Given - เตรียมข้อมูล (สต็อกที่ขายแล้ว)
        testStock.markAsSold();
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        
        // When & Then - เรียกใช้และตรวจสอบ exception
        assertThatThrownBy(() -> stockService.markAsSold(stockId))
            .isInstanceOf(StockException.class)
            .hasMessageContaining("Cannot mark stock as sold");
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository, never()).save(any());
    }
    
    // ==================== INVENTORY MANAGEMENT TESTS ====================
    
    @Test
    @DisplayName("ควรคืนสถิติสต็อกที่ถูกต้อง - Should return correct stock statistics")
    void shouldReturnCorrectStockStatistics() {
        // Given - เตรียมข้อมูล
        Object[] mockStats = {10L, 7L, 2L, 1L}; // total, available, sold, reserved
        
        when(stockRepository.getStockStatisticsByProductId(productId)).thenReturn(mockStats);
        
        // When - เรียกใช้เมธอด
        StockService.StockStatistics result = stockService.getStockStatistics(productId);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result.total()).isEqualTo(10L);
        assertThat(result.available()).isEqualTo(7L);
        assertThat(result.sold()).isEqualTo(2L);
        assertThat(result.reserved()).isEqualTo(1L);
        
        verify(stockRepository).getStockStatisticsByProductId(productId);
    }
    
    @Test
    @DisplayName("ควรตรวจสอบสินค้าสต็อกต่ำได้ถูกต้อง - Should correctly identify low stock products")
    void shouldCorrectlyIdentifyLowStockProducts() {
        // Given - เตรียมข้อมูล
        List<Product> lowStockProducts = Arrays.asList(testProduct);
        
        when(stockRepository.findProductsWithLowStock(5L)).thenReturn(lowStockProducts);
        
        // When - เรียกใช้เมธอด
        List<Product> result = stockService.getProductsWithLowStock(5);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testProduct);
        
        verify(stockRepository).findProductsWithLowStock(5L);
    }
    
    @Test
    @DisplayName("ควรตรวจสอบสต็อกต่ำได้ถูกต้อง - Should correctly check if stock is low")
    void shouldCorrectlyCheckIfStockIsLow() {
        // Given - เตรียมข้อมูล
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.countAvailableByProductId(productId)).thenReturn(3L); // น้อยกว่า threshold (5)
        
        // When - เรียกใช้เมธอด
        boolean result = stockService.isLowStock(productId);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isTrue();
        
        verify(productRepository).findById(productId);
        verify(stockRepository).countAvailableByProductId(productId);
    }
    
    @Test
    @DisplayName("ควรล้างการจองที่หมดอายุได้สำเร็จ - Should cleanup expired reservations successfully")
    void shouldCleanupExpiredReservationsSuccessfully() {
        // Given - เตรียมข้อมูล
        int expectedCleanedCount = 5;
        
        when(stockRepository.clearExpiredReservations()).thenReturn(expectedCleanedCount);
        
        // When - เรียกใช้เมธอด
        int result = stockService.cleanupExpiredReservations();
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isEqualTo(expectedCleanedCount);
        
        verify(stockRepository).clearExpiredReservations();
    }
    
    // ==================== UTILITY OPERATIONS TESTS ====================
    
    @Test
    @DisplayName("ควรหาข้อมูลบัญชีที่ซ้ำได้ - Should find duplicate credentials")
    void shouldFindDuplicateCredentials() {
        // Given - เตรียมข้อมูล
        List<String> duplicates = Arrays.asList("duplicate_cred1", "duplicate_cred2");
        
        when(stockRepository.findDuplicateCredentialsByProductId(productId)).thenReturn(duplicates);
        
        // When - เรียกใช้เมธอด
        List<String> result = stockService.findDuplicateCredentials(productId);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("duplicate_cred1", "duplicate_cred2");
        
        verify(stockRepository).findDuplicateCredentialsByProductId(productId);
    }
    
    @Test
    @DisplayName("ควรตรวจสอบการมีอยู่ของข้อมูลบัญชีได้ - Should check credentials existence")
    void shouldCheckCredentialsExistence() {
        // Given - เตรียมข้อมูล
        String credentials = "test_credentials";
        
        when(stockRepository.existsByProductIdAndCredentials(productId, credentials)).thenReturn(true);
        
        // When - เรียกใช้เมธอด
        boolean result = stockService.credentialsExist(productId, credentials);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isTrue();
        
        verify(stockRepository).existsByProductIdAndCredentials(productId, credentials);
    }
    
    @Test
    @DisplayName("ควรอัปเดตเกณฑ์สต็อกต่ำได้สำเร็จ - Should update low stock threshold successfully")
    void shouldUpdateLowStockThresholdSuccessfully() {
        // Given - เตรียมข้อมูล
        int newThreshold = 10;
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(testProduct)).thenReturn(testProduct);
        
        // When - เรียกใช้เมธอด
        stockService.updateLowStockThreshold(productId, newThreshold);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(testProduct.getLowStockThreshold()).isEqualTo(newThreshold);
        
        verify(productRepository).findById(productId);
        verify(productRepository).save(testProduct);
    }
    
    // ==================== ERROR HANDLING TESTS ====================
    
    @Test
    @DisplayName("ควรโยน ResourceNotFoundException เมื่อไม่พบสต็อกในการอัปเดต - Should throw ResourceNotFoundException when stock not found for update")
    void shouldThrowExceptionWhenStockNotFoundForUpdate() {
        // Given - เตรียมข้อมูล
        when(stockRepository.findById(stockId)).thenReturn(Optional.empty());
        
        // When & Then - เรียกใช้และตรวจสอบ exception
        assertThatThrownBy(() -> stockService.updateStockAdditionalInfo(stockId, "new info"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Stock not found with ID: " + stockId);
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("ควรโยน StockException เมื่อพยายามลบสต็อกที่ขายแล้ว - Should throw StockException when deleting sold stock")
    void shouldThrowExceptionWhenDeletingSoldStock() {
        // Given - เตรียมข้อมูล (สต็อกที่ขายแล้ว)
        testStock.markAsSold();
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        
        // When & Then - เรียกใช้และตรวจสอบ exception
        assertThatThrownBy(() -> stockService.deleteStock(stockId))
            .isInstanceOf(StockException.class)
            .hasMessageContaining("Cannot delete sold stock item");
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository, never()).delete(any());
    }
    
    @Test
    @DisplayName("ควรโยน StockException เมื่อพยายามลบสต็อกที่ถูกจองอยู่ - Should throw StockException when deleting reserved stock")
    void shouldThrowExceptionWhenDeletingReservedStock() {
        // Given - เตรียมข้อมูล (สต็อกที่ถูกจอง)
        testStock.reserve(15);
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        
        // When & Then - เรียกใช้และตรวจสอบ exception
        assertThatThrownBy(() -> stockService.deleteStock(stockId))
            .isInstanceOf(StockException.class)
            .hasMessageContaining("Cannot delete reserved stock item");
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository, never()).delete(any());
    }
    
    // ==================== PAGEABLE TESTS ====================
    
    @Test
    @DisplayName("ควรดึงสต็อกแบบแบ่งหน้าได้ถูกต้อง - Should get paginated stock correctly")
    void shouldGetPaginatedStockCorrectly() {
        // Given - เตรียมข้อมูล
        Pageable pageable = PageRequest.of(0, 10);
        List<Stock> stockList = Arrays.asList(testStock);
        Page<Stock> stockPage = new PageImpl<>(stockList, pageable, 1);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.findByProduct(testProduct, pageable)).thenReturn(stockPage);
        
        // When - เรียกใช้เมธอด
        Page<Stock> result = stockService.getStockByProduct(productId, pageable);
        
        // Then - ตรวจสอบผลลัพธ์
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testStock);
        assertThat(result.getTotalElements()).isEqualTo(1);
        
        verify(productRepository).findById(productId);
        verify(stockRepository).findByProduct(testProduct, pageable);
    }
}