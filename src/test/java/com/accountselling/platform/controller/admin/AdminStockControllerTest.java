package com.accountselling.platform.controller.admin;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.config.TestSecurityConfig;
import com.accountselling.platform.dto.stock.*;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.service.ProductService;
import com.accountselling.platform.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Comprehensive unit tests for AdminStockController. Tests all stock management operations,
 * validation, authorization, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(AdminStockController.class)
@Import(TestSecurityConfig.class)
class AdminStockControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private StockService stockService;

  @MockBean private ProductService productService;

  @Autowired private ObjectMapper objectMapper;

  private Stock testStock;
  private Product testProduct;
  private Category testCategory;
  private StockCreateRequestDto createRequest;
  private StockBulkCreateRequestDto bulkCreateRequest;
  private StockUpdateRequestDto updateRequest;
  private StockStatistics stockStatistics;

  @BeforeEach
  void setUp() {
    // Setup test category
    testCategory = new Category();
    testCategory.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    testCategory.setName("Gaming");
    testCategory.setActive(true);

    // Setup test product
    testProduct = new Product();
    testProduct.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    testProduct.setName("Premium Game Account");
    testProduct.setCategory(testCategory);
    testProduct.setServer("US-West");
    testProduct.setActive(true);

    // Setup test stock
    testStock = new Stock();
    testStock.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174002"));
    testStock.setProduct(testProduct);
    testStock.setCredentials("username:player123\npassword:secret456\nemail:player@example.com");
    testStock.setAdditionalInfo("Level 80 character with rare items");
    testStock.setSold(false);
    testStock.setReservedUntil(null);
    testStock.setCreatedAt(LocalDateTime.now().minusDays(5));
    testStock.setUpdatedAt(LocalDateTime.now().minusDays(1));

    // Setup DTOs
    createRequest = new StockCreateRequestDto();
    createRequest.setProductId(testProduct.getId());
    createRequest.setCredentials("username:newuser\npassword:newpass\nemail:newuser@example.com");
    createRequest.setAdditionalInfo("New account with premium features");

    List<String> credentialsList =
        Arrays.asList(
            "username:user1\npassword:pass1",
            "username:user2\npassword:pass2",
            "username:user3\npassword:pass3");
    bulkCreateRequest = new StockBulkCreateRequestDto();
    bulkCreateRequest.setProductId(testProduct.getId());
    bulkCreateRequest.setCredentialsList(credentialsList);

    updateRequest = new StockUpdateRequestDto();
    updateRequest.setAdditionalInfo("Updated account information");

    // Setup stock statistics
    stockStatistics =
        new StockStatistics(
            100L, // total
            85L, // available
            10L, // sold
            5L // reserved
            );
  }

  // ==================== GET STOCK BY PRODUCT TESTS ====================

  @Test
  @DisplayName("Get stock by product - Success")
  @WithMockUser(roles = "ADMIN")
  void getStockByProduct_Success() throws Exception {
    // Arrange
    List<Stock> stockList = Arrays.asList(testStock);
    Page<Stock> stockPage = new PageImpl<>(stockList, PageRequest.of(0, 20), 1);

    when(productService.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    when(stockService.getStockByProduct(eq(testProduct.getId()), any(Pageable.class)))
        .thenReturn(stockPage);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/stock/product/{productId}", testProduct.getId())
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(testStock.getId().toString()))
        .andExpect(jsonPath("$.content[0].product.id").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.content[0].product.name").value("Premium Game Account"))
        .andExpect(jsonPath("$.content[0].maskedCredentials").value(containsString("***")))
        .andExpect(
            jsonPath("$.content[0].additionalInfo").value("Level 80 character with rare items"))
        .andExpect(jsonPath("$.content[0].sold").value(false))
        .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(productService).findById(testProduct.getId());
    verify(stockService).getStockByProduct(eq(testProduct.getId()), any(Pageable.class));
  }

  @Test
  @DisplayName("Get stock by product - Product not found")
  @WithMockUser(roles = "ADMIN")
  void getStockByProduct_ProductNotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(productService.findById(nonExistentId)).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/stock/product/{productId}", nonExistentId))
        .andExpect(status().isNotFound());

    verify(productService).findById(nonExistentId);
    verify(stockService, never()).getStockByProduct(any(), any());
  }

  @Test
  @DisplayName("Get stock by product - Invalid pagination")
  @WithMockUser(roles = "ADMIN")
  void getStockByProduct_InvalidPagination() throws Exception {
    // Arrange - Even with invalid pagination, the controller tries to find the product
    when(productService.findById(testProduct.getId())).thenReturn(Optional.empty());

    // Act & Assert - Invalid pagination with non-existent product returns 404
    mockMvc
        .perform(
            get("/api/admin/stock/product/{productId}", testProduct.getId())
                .param("page", "-1")
                .param("size", "200"))
        .andExpect(status().isNotFound());

    verify(productService).findById(testProduct.getId());
    verify(stockService, never()).getStockByProduct(any(), any());
  }

  @Test
  @DisplayName("Get stock by product - Unauthorized")
  void getStockByProduct_Unauthorized() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/admin/stock/product/{productId}", testProduct.getId()))
        .andExpect(status().isForbidden());

    verify(stockService, never()).getStockByProduct(any(), any());
  }

  @Test
  @DisplayName("Get stock by product - Forbidden for non-admin")
  @WithMockUser(roles = "USER")
  void getStockByProduct_Forbidden() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/admin/stock/product/{productId}", testProduct.getId()))
        .andExpect(status().isForbidden());

    verify(stockService, never()).getStockByProduct(any(), any());
  }

  // ==================== GET STOCK BY ID TESTS ====================

  @Test
  @DisplayName("Get stock by ID - Success")
  @WithMockUser(roles = "ADMIN")
  void getStockById_Success() throws Exception {
    // Arrange
    when(stockService.getStockById(testStock.getId())).thenReturn(Optional.of(testStock));

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/stock/{id}", testStock.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testStock.getId().toString()))
        .andExpect(jsonPath("$.product.id").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.product.name").value("Premium Game Account"))
        .andExpect(jsonPath("$.product.server").value("US-West"))
        .andExpect(jsonPath("$.product.categoryName").value("Gaming"))
        .andExpect(jsonPath("$.maskedCredentials").value(containsString("***")))
        .andExpect(jsonPath("$.additionalInfo").value("Level 80 character with rare items"))
        .andExpect(jsonPath("$.sold").value(false))
        .andExpect(jsonPath("$.status").value("AVAILABLE"));

    verify(stockService).getStockById(testStock.getId());
  }

  @Test
  @DisplayName("Get stock by ID - Not found")
  @WithMockUser(roles = "ADMIN")
  void getStockById_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(stockService.getStockById(nonExistentId)).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc.perform(get("/api/admin/stock/{id}", nonExistentId)).andExpect(status().isNotFound());

    verify(stockService).getStockById(nonExistentId);
  }

  // ==================== CREATE STOCK TESTS ====================

  @Test
  @DisplayName("Create stock - Success")
  @WithMockUser(roles = "ADMIN")
  void createStock_Success() throws Exception {
    // Arrange
    when(stockService.createStock(
            createRequest.getProductId(),
            createRequest.getCredentials(),
            createRequest.getAdditionalInfo()))
        .thenReturn(testStock);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(testStock.getId().toString()))
        .andExpect(jsonPath("$.product.id").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.additionalInfo").value("Level 80 character with rare items"))
        .andExpect(jsonPath("$.sold").value(false));

    verify(stockService)
        .createStock(
            createRequest.getProductId(),
            createRequest.getCredentials(),
            createRequest.getAdditionalInfo());
  }

  @Test
  @DisplayName("Create stock - Invalid input")
  @WithMockUser(roles = "ADMIN")
  void createStock_InvalidInput() throws Exception {
    // Arrange
    StockCreateRequestDto invalidRequest = new StockCreateRequestDto();
    invalidRequest.setProductId(null); // Invalid: null product ID
    invalidRequest.setCredentials(""); // Invalid: empty credentials

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    verify(stockService, never()).createStock(any(), any(), any());
  }

  @Test
  @DisplayName("Create stock - Product not found")
  @WithMockUser(roles = "ADMIN")
  void createStock_ProductNotFound() throws Exception {
    // Arrange
    when(stockService.createStock(any(), any(), any()))
        .thenThrow(
            new ResourceNotFoundException("Product", createRequest.getProductId().toString()));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isNotFound());

    verify(stockService).createStock(any(), any(), any());
  }

  @Test
  @DisplayName("Create stock - Credentials already exist")
  @WithMockUser(roles = "ADMIN")
  void createStock_CredentialsAlreadyExist() throws Exception {
    // Arrange
    when(stockService.createStock(any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Credentials already exist"));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/stock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());

    verify(stockService).createStock(any(), any(), any());
  }

  // ==================== CREATE BULK STOCK TESTS ====================

  @Test
  @DisplayName("Create bulk stock - Success")
  @WithMockUser(roles = "ADMIN")
  void createBulkStock_Success() throws Exception {
    // Arrange
    Stock stock1 = new Stock();
    stock1.setId(UUID.randomUUID());
    stock1.setProduct(testProduct);

    Stock stock2 = new Stock();
    stock2.setId(UUID.randomUUID());
    stock2.setProduct(testProduct);

    Stock stock3 = new Stock();
    stock3.setId(UUID.randomUUID());
    stock3.setProduct(testProduct);

    List<Stock> createdStocks = Arrays.asList(stock1, stock2, stock3);

    when(stockService.createBulkStock(
            bulkCreateRequest.getProductId(), bulkCreateRequest.getCredentialsList()))
        .thenReturn(createdStocks);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/stock/bulk")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkCreateRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].id").value(stock1.getId().toString()))
        .andExpect(jsonPath("$[1].id").value(stock2.getId().toString()))
        .andExpect(jsonPath("$[2].id").value(stock3.getId().toString()));

    verify(stockService)
        .createBulkStock(bulkCreateRequest.getProductId(), bulkCreateRequest.getCredentialsList());
  }

  @Test
  @DisplayName("Create bulk stock - Invalid input")
  @WithMockUser(roles = "ADMIN")
  void createBulkStock_InvalidInput() throws Exception {
    // Arrange
    StockBulkCreateRequestDto invalidRequest = new StockBulkCreateRequestDto();
    invalidRequest.setProductId(testProduct.getId());
    invalidRequest.setCredentialsList(Arrays.asList()); // Invalid: empty list

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/stock/bulk")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    verify(stockService, never()).createBulkStock(any(), any());
  }

  @Test
  @DisplayName("Create bulk stock - Too many items")
  @WithMockUser(roles = "ADMIN")
  void createBulkStock_TooManyItems() throws Exception {
    // Arrange
    List<String> tooManyCredentials = new java.util.ArrayList<>();
    for (int i = 0; i < 101; i++) {
      tooManyCredentials.add("username:user" + i + "\npassword:pass" + i);
    }

    StockBulkCreateRequestDto invalidRequest = new StockBulkCreateRequestDto();
    invalidRequest.setProductId(testProduct.getId());
    invalidRequest.setCredentialsList(tooManyCredentials);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/stock/bulk")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    verify(stockService, never()).createBulkStock(any(), any());
  }

  // ==================== UPDATE STOCK TESTS ====================

  @Test
  @DisplayName("Update stock - Success")
  @WithMockUser(roles = "ADMIN")
  void updateStock_Success() throws Exception {
    // Arrange
    Stock updatedStock = new Stock();
    updatedStock.setId(testStock.getId());
    updatedStock.setProduct(testProduct);
    updatedStock.setAdditionalInfo("Updated account information");

    when(stockService.updateStockAdditionalInfo(
            testStock.getId(), updateRequest.getAdditionalInfo()))
        .thenReturn(updatedStock);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/stock/{id}", testStock.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testStock.getId().toString()))
        .andExpect(jsonPath("$.additionalInfo").value("Updated account information"));

    verify(stockService)
        .updateStockAdditionalInfo(testStock.getId(), updateRequest.getAdditionalInfo());
  }

  @Test
  @DisplayName("Update stock - Not found")
  @WithMockUser(roles = "ADMIN")
  void updateStock_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(stockService.updateStockAdditionalInfo(nonExistentId, updateRequest.getAdditionalInfo()))
        .thenThrow(new ResourceNotFoundException("Stock", nonExistentId.toString()));

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/stock/{id}", nonExistentId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());

    verify(stockService)
        .updateStockAdditionalInfo(nonExistentId, updateRequest.getAdditionalInfo());
  }

  // ==================== MARK AS SOLD TESTS ====================

  @Test
  @DisplayName("Mark stock as sold - Success")
  @WithMockUser(roles = "ADMIN")
  void markAsSold_Success() throws Exception {
    // Arrange
    Stock soldStock = new Stock();
    soldStock.setId(testStock.getId());
    soldStock.setProduct(testProduct);
    soldStock.setSold(true);
    soldStock.setSoldAt(LocalDateTime.now());

    when(stockService.markAsSold(testStock.getId())).thenReturn(soldStock);

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/stock/{id}/sold", testStock.getId()).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testStock.getId().toString()))
        .andExpect(jsonPath("$.sold").value(true))
        .andExpect(jsonPath("$.soldAt").exists())
        .andExpect(jsonPath("$.status").value("SOLD"));

    verify(stockService).markAsSold(testStock.getId());
  }

  @Test
  @DisplayName("Mark as sold - Already sold")
  @WithMockUser(roles = "ADMIN")
  void markAsSold_AlreadySold() throws Exception {
    // Arrange
    when(stockService.markAsSold(testStock.getId()))
        .thenThrow(new IllegalStateException("Stock is already sold"));

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/stock/{id}/sold", testStock.getId()).with(csrf()))
        .andExpect(status().isBadRequest());

    verify(stockService).markAsSold(testStock.getId());
  }

  // ==================== RELEASE RESERVATION TESTS ====================

  @Test
  @DisplayName("Release reservation - Success")
  @WithMockUser(roles = "ADMIN")
  void releaseReservation_Success() throws Exception {
    // Arrange
    when(stockService.releaseReservation(testStock.getId())).thenReturn(true);

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/stock/{id}/release", testStock.getId()).with(csrf()))
        .andExpect(status().isOk());

    verify(stockService).releaseReservation(testStock.getId());
  }

  @Test
  @DisplayName("Release reservation - Not reserved")
  @WithMockUser(roles = "ADMIN")
  void releaseReservation_NotReserved() throws Exception {
    // Arrange
    when(stockService.releaseReservation(testStock.getId())).thenReturn(false);

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/stock/{id}/release", testStock.getId()).with(csrf()))
        .andExpect(status().isOk()); // Still return OK as the end state is achieved

    verify(stockService).releaseReservation(testStock.getId());
  }

  // ==================== DELETE STOCK TESTS ====================

  @Test
  @DisplayName("Delete stock - Success")
  @WithMockUser(roles = "ADMIN")
  void deleteStock_Success() throws Exception {
    // Arrange
    when(stockService.getStockById(testStock.getId())).thenReturn(Optional.of(testStock));
    doNothing().when(stockService).deleteStock(testStock.getId());

    // Act & Assert
    mockMvc
        .perform(delete("/api/admin/stock/{id}", testStock.getId()).with(csrf()))
        .andExpect(status().isNoContent());

    verify(stockService).getStockById(testStock.getId());
    verify(stockService).deleteStock(testStock.getId());
  }

  @Test
  @DisplayName("Delete stock - Not found")
  @WithMockUser(roles = "ADMIN")
  void deleteStock_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(stockService.getStockById(nonExistentId)).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc
        .perform(delete("/api/admin/stock/{id}", nonExistentId).with(csrf()))
        .andExpect(status().isNotFound());

    verify(stockService).getStockById(nonExistentId);
    verify(stockService, never()).deleteStock(any());
  }

  @Test
  @DisplayName("Delete stock - Cannot delete sold stock")
  @WithMockUser(roles = "ADMIN")
  void deleteStock_CannotDeleteSoldStock() throws Exception {
    // Arrange
    when(stockService.getStockById(testStock.getId())).thenReturn(Optional.of(testStock));
    doThrow(new IllegalStateException("Stock item is sold or reserved"))
        .when(stockService)
        .deleteStock(testStock.getId());

    // Act & Assert
    mockMvc
        .perform(delete("/api/admin/stock/{id}", testStock.getId()).with(csrf()))
        .andExpect(status().isBadRequest());

    verify(stockService).deleteStock(testStock.getId());
  }

  // ==================== GET STOCK STATISTICS TESTS ====================

  @Test
  @DisplayName("Get stock statistics - Success")
  @WithMockUser(roles = "ADMIN")
  void getStockStatistics_Success() throws Exception {
    // Arrange
    when(productService.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    when(stockService.getStockStatistics(testProduct.getId())).thenReturn(stockStatistics);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/stock/product/{productId}/statistics", testProduct.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(100))
        .andExpect(jsonPath("$.available").value(85))
        .andExpect(jsonPath("$.reserved").value(5))
        .andExpect(jsonPath("$.sold").value(10));

    verify(productService).findById(testProduct.getId());
    verify(stockService).getStockStatistics(testProduct.getId());
  }

  // ==================== GET LOW STOCK PRODUCTS TESTS ====================

  @Test
  @DisplayName("Get products with low stock - Success")
  @WithMockUser(roles = "ADMIN")
  void getProductsWithLowStock_Success() throws Exception {
    // Arrange
    List<Product> lowStockProducts = Arrays.asList(testProduct);
    when(stockService.getProductsWithLowStock(null)).thenReturn(lowStockProducts);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/stock/low-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0]").value(testProduct.getId().toString()));

    verify(stockService).getProductsWithLowStock(null);
  }

  @Test
  @DisplayName("Get products with low stock - Custom threshold")
  @WithMockUser(roles = "ADMIN")
  void getProductsWithLowStock_CustomThreshold() throws Exception {
    // Arrange
    List<Product> lowStockProducts = Arrays.asList(testProduct);
    when(stockService.getProductsWithLowStock(20)).thenReturn(lowStockProducts);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/stock/low-stock").param("threshold", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0]").value(testProduct.getId().toString()));

    verify(stockService).getProductsWithLowStock(20);
  }

  // ==================== GET EXPIRED RESERVATIONS TESTS ====================

  @Test
  @DisplayName("Get expired reservations - Success")
  @WithMockUser(roles = "ADMIN")
  void getExpiredReservations_Success() throws Exception {
    // Arrange
    Stock expiredStock = new Stock();
    expiredStock.setId(UUID.randomUUID());
    expiredStock.setProduct(testProduct);
    expiredStock.setReservedUntil(LocalDateTime.now().minusHours(1));

    List<Stock> expiredReservations = Arrays.asList(expiredStock);
    when(stockService.getExpiredReservations()).thenReturn(expiredReservations);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/stock/expired-reservations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(expiredStock.getId().toString()))
        .andExpect(
            jsonPath("$[0].status")
                .value("AVAILABLE")); // Expired reservation should show as available

    verify(stockService).getExpiredReservations();
  }

  // ==================== CLEANUP EXPIRED RESERVATIONS TESTS ====================

  @Test
  @DisplayName("Cleanup expired reservations - Success")
  @WithMockUser(roles = "ADMIN")
  void cleanupExpiredReservations_Success() throws Exception {
    // Arrange
    when(stockService.cleanupExpiredReservations()).thenReturn(5);

    // Act & Assert
    mockMvc
        .perform(post("/api/admin/stock/cleanup-expired").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("5"));

    verify(stockService).cleanupExpiredReservations();
  }

  // ==================== GET SOLD STOCK TESTS ====================

  @Test
  @DisplayName("Get sold stock - Success")
  @WithMockUser(roles = "ADMIN")
  void getSoldStock_Success() throws Exception {
    // Arrange
    Stock soldStock = new Stock();
    soldStock.setId(UUID.randomUUID());
    soldStock.setProduct(testProduct);
    soldStock.setSold(true);
    soldStock.setSoldAt(LocalDateTime.now().minusDays(1));

    List<Stock> soldStockList = Arrays.asList(soldStock);
    when(stockService.getSoldStock(null, null, null)).thenReturn(soldStockList);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/stock/sold"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(soldStock.getId().toString()))
        .andExpect(jsonPath("$[0].sold").value(true))
        .andExpect(jsonPath("$[0].status").value("SOLD"));

    verify(stockService).getSoldStock(null, null, null);
  }

  @Test
  @DisplayName("Get sold stock - With product filter")
  @WithMockUser(roles = "ADMIN")
  void getSoldStock_WithProductFilter() throws Exception {
    // Arrange
    Stock soldStock = new Stock();
    soldStock.setId(UUID.randomUUID());
    soldStock.setProduct(testProduct);
    soldStock.setSold(true);

    List<Stock> soldStockList = Arrays.asList(soldStock);
    when(stockService.getSoldStock(eq(testProduct.getId()), any(), any()))
        .thenReturn(soldStockList);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/stock/sold").param("productId", testProduct.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].product.id").value(testProduct.getId().toString()));

    verify(stockService).getSoldStock(eq(testProduct.getId()), any(), any());
  }

  @Test
  @DisplayName("Get sold stock - With date filters")
  @WithMockUser(roles = "ADMIN")
  void getSoldStock_WithDateFilters() throws Exception {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(7);
    LocalDateTime endDate = LocalDateTime.now();

    Stock soldStock = new Stock();
    soldStock.setId(UUID.randomUUID());
    soldStock.setProduct(testProduct);
    soldStock.setSold(true);

    List<Stock> soldStockList = Arrays.asList(soldStock);
    when(stockService.getSoldStock(any(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(soldStockList);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/stock/sold")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));

    verify(stockService).getSoldStock(any(), any(LocalDateTime.class), any(LocalDateTime.class));
  }
}
