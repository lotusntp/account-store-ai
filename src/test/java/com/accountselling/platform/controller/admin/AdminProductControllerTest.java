package com.accountselling.platform.controller.admin;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.config.TestSecurityConfig;
import com.accountselling.platform.dto.product.ProductCreateRequestDto;
import com.accountselling.platform.dto.product.ProductUpdateRequestDto;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
 * Comprehensive unit tests for AdminProductController. Tests all CRUD operations, validation,
 * authorization, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(AdminProductController.class)
@Import(TestSecurityConfig.class)
class AdminProductControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ProductService productService;

  @Autowired private ObjectMapper objectMapper;

  private Product testProduct;
  private Category testCategory;
  private ProductCreateRequestDto createRequest;
  private ProductUpdateRequestDto updateRequest;
  private ProductService.ProductStockInfo stockInfo;

  @BeforeEach
  void setUp() {
    // Setup test category
    testCategory = new Category();
    testCategory.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    testCategory.setName("Gaming");
    testCategory.setDescription("Gaming accounts");
    testCategory.setActive(true);

    // Setup test product
    testProduct = new Product();
    testProduct.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    testProduct.setName("Premium Game Account");
    testProduct.setDescription("Level 80 premium gaming account");
    testProduct.setPrice(new BigDecimal("99.99"));
    testProduct.setCategory(testCategory);
    testProduct.setServer("US-West");
    testProduct.setImageUrl("https://example.com/image.jpg");
    testProduct.setActive(true);
    testProduct.setSortOrder(1);
    testProduct.setLowStockThreshold(5);
    testProduct.setCreatedAt(LocalDateTime.now().minusDays(5));
    testProduct.setUpdatedAt(LocalDateTime.now().minusDays(1));

    // Setup stock info
    stockInfo =
        new ProductService.ProductStockInfo(
            testProduct.getId(),
            10L, // totalStock
            8L, // availableStock
            2L, // soldStock
            0L, // reservedStock
            true, // inStock
            false // lowStock
            );

    // Setup DTOs
    createRequest =
        ProductCreateRequestDto.builder()
            .name("New Product")
            .description("New product description")
            .price(new BigDecimal("49.99"))
            .categoryId(testCategory.getId())
            .server("EU-West")
            .imageUrl("https://example.com/new-image.jpg")
            .sortOrder(2)
            .lowStockThreshold(3)
            .active(true)
            .build();

    updateRequest =
        ProductUpdateRequestDto.builder()
            .name("Updated Product")
            .description("Updated description")
            .price(new BigDecimal("79.99"))
            .server("Asia-Pacific")
            .sortOrder(3)
            .active(false)
            .build();
  }

  // ==================== GET ALL PRODUCTS TESTS ====================

  @Test
  @DisplayName("Get all products - Success")
  @WithMockUser(roles = "ADMIN")
  void getAllProducts_Success() throws Exception {
    // Arrange
    List<Product> products = Arrays.asList(testProduct);
    Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);

    when(productService.findAllProducts(any(Pageable.class))).thenReturn(productPage);
    when(productService.getProductStockInfo(testProduct.getId())).thenReturn(stockInfo);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/products")
                .param("page", "0")
                .param("size", "20")
                .param("activeOnly", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.content[0].name").value("Premium Game Account"))
        .andExpect(jsonPath("$.content[0].price").value(99.99))
        .andExpect(jsonPath("$.content[0].active").value(true))
        .andExpect(jsonPath("$.content[0].category.id").value(testCategory.getId().toString()))
        .andExpect(jsonPath("$.content[0].stock.totalStock").value(10))
        .andExpect(jsonPath("$.content[0].stock.availableStock").value(8))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(productService).findAllProducts(any(Pageable.class));
    verify(productService).getProductStockInfo(testProduct.getId());
  }

  @Test
  @DisplayName("Get active products only - Success")
  @WithMockUser(roles = "ADMIN")
  void getActiveProductsOnly_Success() throws Exception {
    // Arrange
    List<Product> products = Arrays.asList(testProduct);
    Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);

    when(productService.findActiveProducts(any(Pageable.class))).thenReturn(productPage);
    when(productService.getProductStockInfo(testProduct.getId())).thenReturn(stockInfo);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/products").param("activeOnly", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].active").value(true));

    verify(productService).findActiveProducts(any(Pageable.class));
    verify(productService, never()).findAllProducts(any(Pageable.class));
  }

  @Test
  @DisplayName("Get all products - Invalid pagination")
  @WithMockUser(roles = "ADMIN")
  void getAllProducts_InvalidPagination() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/admin/products").param("page", "-1").param("size", "200"))
        .andExpect(status().isBadRequest());

    verify(productService, never()).findAllProducts(any(Pageable.class));
  }

  @Test
  @DisplayName("Get all products - Unauthorized")
  void getAllProducts_Unauthorized() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/products")).andExpect(status().isForbidden());

    verify(productService, never()).findAllProducts(any(Pageable.class));
  }

  @Test
  @DisplayName("Get all products - Forbidden for non-admin")
  @WithMockUser(roles = "USER")
  void getAllProducts_Forbidden() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/products")).andExpect(status().isForbidden());

    verify(productService, never()).findAllProducts(any(Pageable.class));
  }

  // ==================== GET PRODUCT BY ID TESTS ====================

  @Test
  @DisplayName("Get product by ID - Success")
  @WithMockUser(roles = "ADMIN")
  void getProductById_Success() throws Exception {
    // Arrange
    when(productService.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    when(productService.getProductStockInfo(testProduct.getId())).thenReturn(stockInfo);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/products/{id}", testProduct.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.name").value("Premium Game Account"))
        .andExpect(jsonPath("$.description").value("Level 80 premium gaming account"))
        .andExpect(jsonPath("$.price").value(99.99))
        .andExpect(jsonPath("$.server").value("US-West"))
        .andExpect(jsonPath("$.category.name").value("Gaming"))
        .andExpect(jsonPath("$.stock.totalStock").value(10));

    verify(productService).findById(testProduct.getId());
    verify(productService).getProductStockInfo(testProduct.getId());
  }

  @Test
  @DisplayName("Get product by ID - Not Found")
  @WithMockUser(roles = "ADMIN")
  void getProductById_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(productService.findById(nonExistentId)).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/products/{id}", nonExistentId))
        .andExpect(status().isNotFound());

    verify(productService).findById(nonExistentId);
    verify(productService, never()).getProductStockInfo(any());
  }

  // ==================== CREATE PRODUCT TESTS ====================

  @Test
  @DisplayName("Create product - Success")
  @WithMockUser(roles = "ADMIN")
  void createProduct_Success() throws Exception {
    // Arrange
    when(productService.createProduct(
            createRequest.getName(),
            createRequest.getDescription(),
            createRequest.getPrice(),
            createRequest.getCategoryId(),
            createRequest.getServer(),
            createRequest.getImageUrl(),
            createRequest.getSortOrder(),
            createRequest.getLowStockThreshold()))
        .thenReturn(testProduct);
    when(productService.getProductStockInfo(testProduct.getId())).thenReturn(stockInfo);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.name").value("Premium Game Account"))
        .andExpect(jsonPath("$.active").value(true));

    verify(productService)
        .createProduct(
            createRequest.getName(),
            createRequest.getDescription(),
            createRequest.getPrice(),
            createRequest.getCategoryId(),
            createRequest.getServer(),
            createRequest.getImageUrl(),
            createRequest.getSortOrder(),
            createRequest.getLowStockThreshold());
  }

  @Test
  @DisplayName("Create product - Invalid input")
  @WithMockUser(roles = "ADMIN")
  void createProduct_InvalidInput() throws Exception {
    // Arrange
    ProductCreateRequestDto invalidRequest =
        ProductCreateRequestDto.builder()
            .name("") // Invalid: empty name
            .price(new BigDecimal("-10")) // Invalid: negative price
            .build();

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    verify(productService, never())
        .createProduct(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Create product - Category not found")
  @WithMockUser(roles = "ADMIN")
  void createProduct_CategoryNotFound() throws Exception {
    // Arrange
    when(productService.createProduct(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(
            new ResourceNotFoundException("Category", createRequest.getCategoryId().toString()));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isNotFound());

    verify(productService).createProduct(any(), any(), any(), any(), any(), any(), any(), any());
  }

  // ==================== UPDATE PRODUCT TESTS ====================

  @Test
  @DisplayName("Update product - Success")
  @WithMockUser(roles = "ADMIN")
  void updateProduct_Success() throws Exception {
    // Arrange
    Product updatedProduct = new Product();
    updatedProduct.setId(testProduct.getId());
    updatedProduct.setName("Updated Product");
    updatedProduct.setDescription("Updated description");
    updatedProduct.setPrice(new BigDecimal("79.99"));
    updatedProduct.setCategory(testCategory);
    updatedProduct.setActive(false);

    when(productService.updateProduct(
            testProduct.getId(),
            updateRequest.getName(),
            updateRequest.getDescription(),
            updateRequest.getPrice(),
            updateRequest.getCategoryId(),
            updateRequest.getServer(),
            updateRequest.getImageUrl()))
        .thenReturn(updatedProduct);
    when(productService.getProductStockInfo(testProduct.getId())).thenReturn(stockInfo);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/products/{id}", testProduct.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.name").value("Updated Product"))
        .andExpect(jsonPath("$.price").value(79.99))
        .andExpect(jsonPath("$.active").value(false));

    verify(productService)
        .updateProduct(
            testProduct.getId(),
            updateRequest.getName(),
            updateRequest.getDescription(),
            updateRequest.getPrice(),
            updateRequest.getCategoryId(),
            updateRequest.getServer(),
            updateRequest.getImageUrl());
  }

  @Test
  @DisplayName("Update product - No fields to update")
  @WithMockUser(roles = "ADMIN")
  void updateProduct_NoFieldsToUpdate() throws Exception {
    // Arrange
    ProductUpdateRequestDto emptyRequest = ProductUpdateRequestDto.builder().build();

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/products/{id}", testProduct.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
        .andExpect(status().isBadRequest());

    verify(productService, never()).updateProduct(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Update product - Not found")
  @WithMockUser(roles = "ADMIN")
  void updateProduct_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(productService.updateProduct(
            nonExistentId,
            updateRequest.getName(),
            updateRequest.getDescription(),
            updateRequest.getPrice(),
            updateRequest.getCategoryId(),
            updateRequest.getServer(),
            updateRequest.getImageUrl()))
        .thenThrow(new ResourceNotFoundException("Product", nonExistentId.toString()));

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/products/{id}", nonExistentId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());

    verify(productService)
        .updateProduct(eq(nonExistentId), any(), any(), any(), any(), any(), any());
  }

  // ==================== SET PRODUCT STATUS TESTS ====================

  @Test
  @DisplayName("Set product status - Success")
  @WithMockUser(roles = "ADMIN")
  void setProductStatus_Success() throws Exception {
    // Arrange
    Product updatedProduct = new Product();
    updatedProduct.setId(testProduct.getId());
    updatedProduct.setName(testProduct.getName());
    updatedProduct.setActive(false);
    updatedProduct.setCategory(testCategory);

    when(productService.setProductActive(testProduct.getId(), false)).thenReturn(updatedProduct);
    when(productService.getProductStockInfo(testProduct.getId())).thenReturn(stockInfo);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/products/{id}/status", testProduct.getId())
                .with(csrf())
                .param("active", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$.active").value(false));

    verify(productService).setProductActive(testProduct.getId(), false);
  }

  // ==================== MOVE PRODUCT TO CATEGORY TESTS ====================

  @Test
  @DisplayName("Move product to category - Success")
  @WithMockUser(roles = "ADMIN")
  void moveProductToCategory_Success() throws Exception {
    // Arrange
    UUID newCategoryId = UUID.randomUUID();
    Product movedProduct = new Product();
    movedProduct.setId(testProduct.getId());
    movedProduct.setName(testProduct.getName());
    movedProduct.setCategory(testCategory);

    when(productService.moveProductToCategory(testProduct.getId(), newCategoryId))
        .thenReturn(movedProduct);
    when(productService.getProductStockInfo(testProduct.getId())).thenReturn(stockInfo);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/products/{id}/category", testProduct.getId())
                .with(csrf())
                .param("categoryId", newCategoryId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testProduct.getId().toString()));

    verify(productService).moveProductToCategory(testProduct.getId(), newCategoryId);
  }

  // ==================== DELETE PRODUCT TESTS ====================

  @Test
  @DisplayName("Delete product - Success")
  @WithMockUser(roles = "ADMIN")
  void deleteProduct_Success() throws Exception {
    // Arrange
    when(productService.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    doNothing().when(productService).deleteProduct(testProduct.getId());

    // Act & Assert
    mockMvc
        .perform(delete("/api/admin/products/{id}", testProduct.getId()).with(csrf()))
        .andExpect(status().isNoContent());

    verify(productService).findById(testProduct.getId());
    verify(productService).deleteProduct(testProduct.getId());
  }

  @Test
  @DisplayName("Delete product - Not found")
  @WithMockUser(roles = "ADMIN")
  void deleteProduct_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(productService.findById(nonExistentId)).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc
        .perform(delete("/api/admin/products/{id}", nonExistentId).with(csrf()))
        .andExpect(status().isNotFound());

    verify(productService).findById(nonExistentId);
    verify(productService, never()).deleteProduct(any());
  }

  @Test
  @DisplayName("Delete product - Has pending orders")
  @WithMockUser(roles = "ADMIN")
  void deleteProduct_HasPendingOrders() throws Exception {
    // Arrange
    when(productService.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    doThrow(new IllegalStateException("Product has pending orders or stock"))
        .when(productService)
        .deleteProduct(testProduct.getId());

    // Act & Assert
    mockMvc
        .perform(delete("/api/admin/products/{id}", testProduct.getId()).with(csrf()))
        .andExpect(status().isBadRequest());

    verify(productService).deleteProduct(testProduct.getId());
  }

  // ==================== FORCE DELETE PRODUCT TESTS ====================

  @Test
  @DisplayName("Force delete product - Success")
  @WithMockUser(roles = "ADMIN")
  void forceDeleteProduct_Success() throws Exception {
    // Arrange
    when(productService.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
    doNothing().when(productService).forceDeleteProduct(testProduct.getId());

    // Act & Assert
    mockMvc
        .perform(delete("/api/admin/products/{id}/force", testProduct.getId()).with(csrf()))
        .andExpect(status().isNoContent());

    verify(productService).findById(testProduct.getId());
    verify(productService).forceDeleteProduct(testProduct.getId());
  }

  // ==================== GET PRODUCTS BY CATEGORY TESTS ====================

  @Test
  @DisplayName("Get products by category - Success")
  @WithMockUser(roles = "ADMIN")
  void getProductsByCategory_Success() throws Exception {
    // Arrange
    List<Product> products = Arrays.asList(testProduct);
    Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);

    when(productService.findProductsByCategory(
            eq(testCategory.getId()), eq(false), eq(false), any(Pageable.class)))
        .thenReturn(productPage);
    when(productService.getProductStockInfo(testProduct.getId())).thenReturn(stockInfo);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/products/category/{categoryId}", testCategory.getId())
                .param("includeSubcategories", "false")
                .param("activeOnly", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(testProduct.getId().toString()));

    verify(productService)
        .findProductsByCategory(
            eq(testCategory.getId()), eq(false), eq(false), any(Pageable.class));
  }

  // ==================== GET AVAILABLE SERVERS TESTS ====================

  @Test
  @DisplayName("Get available servers - Success")
  @WithMockUser(roles = "ADMIN")
  void getAvailableServers_Success() throws Exception {
    // Arrange
    List<String> servers = Arrays.asList("US-West", "EU-West", "Asia-Pacific");
    when(productService.getAvailableServers()).thenReturn(servers);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/products/servers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0]").value("US-West"))
        .andExpect(jsonPath("$[1]").value("EU-West"))
        .andExpect(jsonPath("$[2]").value("Asia-Pacific"));

    verify(productService).getAvailableServers();
  }
}
