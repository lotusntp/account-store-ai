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
 * Unit testing for StockServiceImpl
 * Tests comprehensive stock management including reservations, sales tracking
 * and inventory management with mock dependencies
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
        // Setup test data
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
    @DisplayName("Should create new stock successfully")
    void shouldCreateStockSuccessfully() {
        // Given - Prepare data
        String credentials = "test_credentials";
        String additionalInfo = "test_additional_info";
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.existsByProductIdAndCredentials(productId, credentials)).thenReturn(false);
        when(stockRepository.save(any(Stock.class))).thenReturn(testStock);
        
        // When - Execute method
        Stock result = stockService.createStock(productId, credentials, additionalInfo);
        
        // Then - Verify results
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(stockId);
        assertThat(result.getCredentials()).isEqualTo("encrypted_credentials_123");
        
        verify(productRepository).findById(productId);
        verify(stockRepository).existsByProductIdAndCredentials(productId, credentials);
        verify(stockRepository).save(any(Stock.class));
    }
    
    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        // Given - Prepare data
        String credentials = "test_credentials";
        
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        
        // When & Then - Execute and verify exception
        assertThatThrownBy(() -> stockService.createStock(productId, credentials, null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with ID: " + productId);
        
        verify(productRepository).findById(productId);
        verify(stockRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when credentials duplicate")
    void shouldThrowExceptionWhenCredentialsDuplicate() {
        // Given - Prepare data
        String credentials = "duplicate_credentials";
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.existsByProductIdAndCredentials(productId, credentials)).thenReturn(true);
        
        // When & Then - Execute and verify exception
        assertThatThrownBy(() -> stockService.createStock(productId, credentials, null))
            .isInstanceOf(ResourceAlreadyExistsException.class)
            .hasMessageContaining("Credentials already exist for this product");
        
        verify(productRepository).findById(productId);
        verify(stockRepository).existsByProductIdAndCredentials(productId, credentials);
        verify(stockRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should create bulk stock successfully")
    void shouldCreateBulkStockSuccessfully() {
        // Given - Prepare data
        List<String> credentialsList = Arrays.asList("cred1", "cred2", "cred3");
        List<Stock> expectedStocks = Arrays.asList(
            new Stock(testProduct, "cred1"),
            new Stock(testProduct, "cred2"),
            new Stock(testProduct, "cred3")
        );
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.existsByProductIdAndCredentials(eq(productId), anyString())).thenReturn(false);
        when(stockRepository.saveAll(anyList())).thenReturn(expectedStocks);
        
        // When - Execute method
        List<Stock> result = stockService.createBulkStock(productId, credentialsList);
        
        // Then - Verify results
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Stock::getCredentials).containsExactly("cred1", "cred2", "cred3");
        
        verify(productRepository).findById(productId);
        verify(stockRepository, times(3)).existsByProductIdAndCredentials(eq(productId), anyString());
        verify(stockRepository).saveAll(anyList());
    }
    
    // ==================== STOCK AVAILABILITY TESTS ====================
    
    @Test
    @DisplayName("Should return correct available stock count")
    void shouldReturnCorrectAvailableStockCount() {
        // Given - Prepare data
        long expectedCount = 10L;
        
        when(stockRepository.countAvailableByProductId(productId)).thenReturn(expectedCount);
        
        // When - Execute method
        long result = stockService.getAvailableStockCount(productId);
        
        // Then - Verify results
        assertThat(result).isEqualTo(expectedCount);
        
        verify(stockRepository).countAvailableByProductId(productId);
    }
    
    @Test
    @DisplayName("Should return true when in stock")
    void shouldReturnTrueWhenInStock() {
        // Given - Prepare data
        when(stockRepository.countAvailableByProductId(productId)).thenReturn(5L);
        
        // When - Execute method
        boolean result = stockService.isInStock(productId);
        
        // Then - Verify results
        assertThat(result).isTrue();
        
        verify(stockRepository).countAvailableByProductId(productId);
    }
    
    @Test
    @DisplayName("Should return false when out of stock")
    void shouldReturnFalseWhenOutOfStock() {
        // Given - Prepare data
        when(stockRepository.countAvailableByProductId(productId)).thenReturn(0L);
        
        // When - Execute method
        boolean result = stockService.isInStock(productId);
        
        // Then - Verify results
        assertThat(result).isFalse();
        
        verify(stockRepository).countAvailableByProductId(productId);
    }
    
    @Test
    @DisplayName("Should get first available stock")
    void shouldGetFirstAvailableStock() {
        // Given - Prepare data
        when(stockRepository.findFirstAvailableByProductId(productId)).thenReturn(Optional.of(testStock));
        
        // When - Execute method
        Optional<Stock> result = stockService.getFirstAvailableStock(productId);
        
        // Then - Verify results
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testStock);
        
        verify(stockRepository).findFirstAvailableByProductId(productId);
    }
    
    // ==================== STOCK RESERVATION TESTS ====================
    
    @Test
    @DisplayName("Should reserve stock successfully")
    void shouldReserveStockSuccessfully() {
        // Given - Prepare data
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
        
        // When - Execute method
        List<Stock> result = stockService.reserveStock(productId, quantity, reservationMinutes);
        
        // Then - Verify results
        assertThat(result).hasSize(quantity);
        assertThat(result).allMatch(stock -> stock.getReservedUntil() != null);
        
        verify(productRepository).findById(productId);
        verify(stockRepository).clearExpiredReservations();
        verify(stockRepository).findAvailableStockByProductId(productId);
        verify(stockRepository, times(quantity)).save(any(Stock.class));
    }
    
    @Test
    @DisplayName("Should throw OutOfStockException when insufficient stock")
    void shouldThrowOutOfStockExceptionWhenInsufficientStock() {
        // Given - Prepare data
        int quantity = 5;
        int reservationMinutes = 15;
        List<Stock> availableStocks = Arrays.asList(
            new Stock(testProduct, "cred1"),
            new Stock(testProduct, "cred2")
        ); // Only 2 items available but 5 requested
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.clearExpiredReservations()).thenReturn(0);
        when(stockRepository.findAvailableStockByProductId(productId)).thenReturn(availableStocks);
        
        // When & Then - Execute and verify exception
        assertThatThrownBy(() -> stockService.reserveStock(productId, quantity, reservationMinutes))
            .isInstanceOf(OutOfStockException.class)
            .hasMessageContaining("Insufficient stock available. Required: 5, Available: 2");
        
        verify(productRepository).findById(productId);
        verify(stockRepository).clearExpiredReservations();
        verify(stockRepository).findAvailableStockByProductId(productId);
        verify(stockRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should release reservation successfully")
    void shouldReleaseReservationSuccessfully() {
        // Given - Prepare data
        testStock.reserve(15); // Reserve stock first
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        when(stockRepository.save(testStock)).thenReturn(testStock);
        
        // When - Execute method
        boolean result = stockService.releaseReservation(stockId);
        
        // Then - Verify results
        assertThat(result).isTrue();
        assertThat(testStock.getReservedUntil()).isNull();
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository).save(testStock);
    }
    
    @Test
    @DisplayName("Should return false when releasing non-reserved stock")
    void shouldReturnFalseWhenReleasingNonReservedStock() {
        // Given - Prepare data (stock that is not reserved)
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        
        // When - Execute method
        boolean result = stockService.releaseReservation(stockId);
        
        // Then - Verify results
        assertThat(result).isFalse();
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository, never()).save(any());
    }
    
    // ==================== STOCK SALES TESTS ====================
    
    @Test
    @DisplayName("Should mark as sold successfully")
    void shouldMarkAsSoldSuccessfully() {
        // Given - Prepare data
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        when(stockRepository.save(testStock)).thenReturn(testStock);
        
        // When - Execute method
        Stock result = stockService.markAsSold(stockId);
        
        // Then - Verify results
        assertThat(result).isNotNull();
        assertThat(result.getSold()).isTrue();
        assertThat(result.getSoldAt()).isNotNull();
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository).save(testStock);
    }
    
    @Test
    @DisplayName("Should throw StockException when marking already sold stock")
    void shouldThrowExceptionWhenMarkingAlreadySoldStock() {
        // Given - Prepare data (stock that is already sold)
        testStock.markAsSold();
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        
        // When & Then - Execute and verify exception
        assertThatThrownBy(() -> stockService.markAsSold(stockId))
            .isInstanceOf(StockException.class)
            .hasMessageContaining("Cannot mark stock as sold");
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository, never()).save(any());
    }
    
    // ==================== INVENTORY MANAGEMENT TESTS ====================
    
    @Test
    @DisplayName("Should return correct stock statistics")
    void shouldReturnCorrectStockStatistics() {
        // Given - Prepare data
        Object[] mockStats = {10L, 7L, 2L, 1L}; // total, available, sold, reserved
        
        when(stockRepository.getStockStatisticsByProductId(productId)).thenReturn(mockStats);
        
        // When - Execute method
        StockService.StockStatistics result = stockService.getStockStatistics(productId);
        
        // Then - Verify results
        assertThat(result.total()).isEqualTo(10L);
        assertThat(result.available()).isEqualTo(7L);
        assertThat(result.sold()).isEqualTo(2L);
        assertThat(result.reserved()).isEqualTo(1L);
        
        verify(stockRepository).getStockStatisticsByProductId(productId);
    }
    
    @Test
    @DisplayName("Should correctly identify low stock products")
    void shouldCorrectlyIdentifyLowStockProducts() {
        // Given - Prepare data
        List<Product> lowStockProducts = Arrays.asList(testProduct);
        
        when(stockRepository.findProductsWithLowStock(5L)).thenReturn(lowStockProducts);
        
        // When - Execute method
        List<Product> result = stockService.getProductsWithLowStock(5);
        
        // Then - Verify results
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testProduct);
        
        verify(stockRepository).findProductsWithLowStock(5L);
    }
    
    @Test
    @DisplayName("Should correctly check if stock is low")
    void shouldCorrectlyCheckIfStockIsLow() {
        // Given - Prepare data
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.countAvailableByProductId(productId)).thenReturn(3L); // Less than threshold (5)
        
        // When - Execute method
        boolean result = stockService.isLowStock(productId);
        
        // Then - Verify results
        assertThat(result).isTrue();
        
        verify(productRepository).findById(productId);
        verify(stockRepository).countAvailableByProductId(productId);
    }
    
    @Test
    @DisplayName("Should cleanup expired reservations successfully")
    void shouldCleanupExpiredReservationsSuccessfully() {
        // Given - Prepare data
        int expectedCleanedCount = 5;
        
        when(stockRepository.clearExpiredReservations()).thenReturn(expectedCleanedCount);
        
        // When - Execute method
        int result = stockService.cleanupExpiredReservations();
        
        // Then - Verify results
        assertThat(result).isEqualTo(expectedCleanedCount);
        
        verify(stockRepository).clearExpiredReservations();
    }
    
    // ==================== UTILITY OPERATIONS TESTS ====================
    
    @Test
    @DisplayName("Should find duplicate credentials")
    void shouldFindDuplicateCredentials() {
        // Given - Prepare data
        List<String> duplicates = Arrays.asList("duplicate_cred1", "duplicate_cred2");
        
        when(stockRepository.findDuplicateCredentialsByProductId(productId)).thenReturn(duplicates);
        
        // When - Execute method
        List<String> result = stockService.findDuplicateCredentials(productId);
        
        // Then - Verify results
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("duplicate_cred1", "duplicate_cred2");
        
        verify(stockRepository).findDuplicateCredentialsByProductId(productId);
    }
    
    @Test
    @DisplayName("Should check credentials existence")
    void shouldCheckCredentialsExistence() {
        // Given - Prepare data
        String credentials = "test_credentials";
        
        when(stockRepository.existsByProductIdAndCredentials(productId, credentials)).thenReturn(true);
        
        // When - Execute method
        boolean result = stockService.credentialsExist(productId, credentials);
        
        // Then - Verify results
        assertThat(result).isTrue();
        
        verify(stockRepository).existsByProductIdAndCredentials(productId, credentials);
    }
    
    @Test
    @DisplayName("Should update low stock threshold successfully")
    void shouldUpdateLowStockThresholdSuccessfully() {
        // Given - Prepare data
        int newThreshold = 10;
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(testProduct)).thenReturn(testProduct);
        
        // When - Execute method
        stockService.updateLowStockThreshold(productId, newThreshold);
        
        // Then - Verify results
        assertThat(testProduct.getLowStockThreshold()).isEqualTo(newThreshold);
        
        verify(productRepository).findById(productId);
        verify(productRepository).save(testProduct);
    }
    
    // ==================== ERROR HANDLING TESTS ====================
    
    @Test
    @DisplayName("Should throw ResourceNotFoundException when stock not found for update")
    void shouldThrowExceptionWhenStockNotFoundForUpdate() {
        // Given - Prepare data
        when(stockRepository.findById(stockId)).thenReturn(Optional.empty());
        
        // When & Then - Execute and verify exception
        assertThatThrownBy(() -> stockService.updateStockAdditionalInfo(stockId, "new info"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Stock not found with ID: " + stockId);
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw StockException when deleting sold stock")
    void shouldThrowExceptionWhenDeletingSoldStock() {
        // Given - Prepare data (stock that is already sold)
        testStock.markAsSold();
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        
        // When & Then - Execute and verify exception
        assertThatThrownBy(() -> stockService.deleteStock(stockId))
            .isInstanceOf(StockException.class)
            .hasMessageContaining("Cannot delete sold stock item");
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository, never()).delete(any());
    }
    
    @Test
    @DisplayName("Should throw StockException when deleting reserved stock")
    void shouldThrowExceptionWhenDeletingReservedStock() {
        // Given - Prepare data (stock that is reserved)
        testStock.reserve(15);
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        
        // When & Then - Execute and verify exception
        assertThatThrownBy(() -> stockService.deleteStock(stockId))
            .isInstanceOf(StockException.class)
            .hasMessageContaining("Cannot delete reserved stock item");
        
        verify(stockRepository).findById(stockId);
        verify(stockRepository, never()).delete(any());
    }
    
    // ==================== PAGEABLE TESTS ====================
    
    @Test
    @DisplayName("Should get paginated stock correctly")
    void shouldGetPaginatedStockCorrectly() {
        // Given - Prepare data
        Pageable pageable = PageRequest.of(0, 10);
        List<Stock> stockList = Arrays.asList(testStock);
        Page<Stock> stockPage = new PageImpl<>(stockList, pageable, 1);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(stockRepository.findByProduct(testProduct, pageable)).thenReturn(stockPage);
        
        // When - Execute method
        Page<Stock> result = stockService.getStockByProduct(productId, pageable);
        
        // Then - Verify results
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testStock);
        assertThat(result.getTotalElements()).isEqualTo(1);
        
        verify(productRepository).findById(productId);
        verify(stockRepository).findByProduct(testProduct, pageable);
    }
}