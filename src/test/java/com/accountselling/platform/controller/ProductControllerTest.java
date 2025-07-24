package com.accountselling.platform.controller;

import com.accountselling.platform.dto.product.ProductResponseDto;
import com.accountselling.platform.dto.product.ProductSearchRequestDto;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProductController.
 * Tests controller logic with mocked service dependencies.
 * Security is disabled for unit tests to focus on controller logic.
 * 
 * Unit tests สำหรับ ProductController
 * ทดสอบ logic ของ controller ด้วย service dependencies ที่ถูก mock
 * ปิด security สำหรับ unit tests เพื่อให้เน้นที่ logic ของ controller
 */
@WebMvcTest(controllers = ProductController.class, 
           excludeAutoConfiguration = {
               org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
           })
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private Product testProduct;
    private Category testCategory;
    private UUID productId;
    private UUID categoryId;
    private ProductService.ProductStockInfo stockInfo;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        // Create test category
        testCategory = new Category();
        testCategory.setId(categoryId);
        testCategory.setName("Gaming Accounts");
        testCategory.setDescription("Gaming accounts category");
        testCategory.setActive(true);

        // Create test product
        testProduct = new Product();
        testProduct.setId(productId);
        testProduct.setName("World of Warcraft Account");
        testProduct.setDescription("High-level WoW account with rare items");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setImageUrl("http://example.com/wow.jpg");
        testProduct.setServer("Stormrage");
        testProduct.setActive(true);
        testProduct.setSortOrder(1);
        testProduct.setCategory(testCategory);
        testProduct.setLowStockThreshold(5);
        testProduct.setCreatedAt(LocalDateTime.now());
        testProduct.setUpdatedAt(LocalDateTime.now());

        // Create stock info
        stockInfo = new ProductService.ProductStockInfo(productId, 10L, 8L, 2L, 0L, true, false);
    }

    @Test
    @DisplayName("Should get all active products successfully")
    void shouldGetAllActiveProductsSuccessfully() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.findActiveProducts(any(Pageable.class))).thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products")
                        .param("activeOnly", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(productId.toString())))
                .andExpect(jsonPath("$.content[0].name", is("World of Warcraft Account")))
                .andExpect(jsonPath("$.content[0].description", is("High-level WoW account with rare items")))
                .andExpect(jsonPath("$.content[0].price", is(99.99)))
                .andExpect(jsonPath("$.content[0].formattedPrice", is("99.99")))
                .andExpect(jsonPath("$.content[0].server", is("Stormrage")))
                .andExpect(jsonPath("$.content[0].active", is(true)))
                .andExpect(jsonPath("$.content[0].category.id", is(categoryId.toString())))
                .andExpect(jsonPath("$.content[0].category.name", is("Gaming Accounts")))
                .andExpect(jsonPath("$.content[0].stock.totalStock", is(10)))
                .andExpect(jsonPath("$.content[0].stock.availableStock", is(8)))
                .andExpect(jsonPath("$.content[0].stock.inStock", is(true)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    @DisplayName("Should get all products including inactive when activeOnly is false")
    void shouldGetAllProductsIncludingInactive() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.findAllProducts(any(Pageable.class))).thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products")
                        .param("activeOnly", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Should get products with available stock when inStockOnly is true")
    void shouldGetProductsWithAvailableStockWhenInStockOnly() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.findProductsWithAvailableStock(any(Pageable.class))).thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products")
                        .param("inStockOnly", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].stock.inStock", is(true)));
    }

    @Test
    @DisplayName("Should get product by ID successfully")
    void shouldGetProductByIdSuccessfully() throws Exception {
        // Given
        when(productService.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(productId.toString())))
                .andExpect(jsonPath("$.name", is("World of Warcraft Account")))
                .andExpect(jsonPath("$.description", is("High-level WoW account with rare items")))
                .andExpect(jsonPath("$.price", is(99.99)))
                .andExpect(jsonPath("$.server", is("Stormrage")))
                .andExpect(jsonPath("$.category.name", is("Gaming Accounts")))
                .andExpect(jsonPath("$.stock.totalStock", is(10)));
    }

    @Test
    @DisplayName("Should return 404 when product not found by ID")
    void shouldReturn404WhenProductNotFoundById() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(productService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/products/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get products by category successfully")
    void shouldGetProductsByCategorySuccessfully() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.findProductsByCategory(eq(categoryId), eq(false), eq(true), any(Pageable.class)))
                .thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products/category/{categoryId}", categoryId)
                        .param("includeSubcategories", "false")
                        .param("activeOnly", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].category.id", is(categoryId.toString())));
    }

    @Test
    @DisplayName("Should get products by category with subcategories")
    void shouldGetProductsByCategoryWithSubcategories() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.findProductsByCategory(eq(categoryId), eq(true), eq(true), any(Pageable.class)))
                .thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products/category/{categoryId}", categoryId)
                        .param("includeSubcategories", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Should search products successfully")
    void shouldSearchProductsSuccessfully() throws Exception {
        // Given
        ProductSearchRequestDto searchRequest = ProductSearchRequestDto.builder()
                .name("warcraft")
                .minPrice(new BigDecimal("50.00"))
                .maxPrice(new BigDecimal("150.00"))
                .activeOnly(true)
                .page(0)
                .size(20)
                .sortBy("name")
                .sortDirection("asc")
                .build();

        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.searchProducts(any(ProductService.ProductSearchCriteria.class), any(Pageable.class)))
                .thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products/search")
                        .param("name", "warcraft")
                        .param("minPrice", "50.00")
                        .param("maxPrice", "150.00")
                        .param("activeOnly", "true")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "name")
                        .param("sortDirection", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", containsStringIgnoringCase("warcraft")));
    }

    @Test
    @DisplayName("Should return 400 when search has invalid price range")
    void shouldReturn400WhenSearchHasInvalidPriceRange() throws Exception {
        // When & Then - maxPrice less than minPrice
        mockMvc.perform(get("/api/products/search")
                        .param("minPrice", "100.00")
                        .param("maxPrice", "50.00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should search products by name successfully")
    void shouldSearchProductsByNameSuccessfully() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.searchProductsByName(eq("warcraft"), eq(true), any(Pageable.class)))
                .thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products/search/name")
                        .param("name", "warcraft")
                        .param("activeOnly", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", containsStringIgnoringCase("warcraft")));
    }

    @Test
    @DisplayName("Should return 400 when search name is too short")
    void shouldReturn400WhenSearchNameTooShort() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/search/name")
                        .param("name", "a")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get products by server successfully")
    void shouldGetProductsByServerSuccessfully() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.findProductsByServer(eq("Stormrage"), eq(true), any(Pageable.class)))
                .thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products/server/{server}", "Stormrage")
                        .param("activeOnly", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].server", is("Stormrage")));
    }

    @Test
    @DisplayName("Should get products by price range successfully")
    void shouldGetProductsByPriceRangeSuccessfully() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.findProductsByPriceRange(
                eq(new BigDecimal("50.00")), eq(new BigDecimal("150.00")), eq(true), any(Pageable.class)))
                .thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products/price-range")
                        .param("minPrice", "50.00")
                        .param("maxPrice", "150.00")
                        .param("activeOnly", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].price", allOf(
                        greaterThanOrEqualTo(50.0),
                        lessThanOrEqualTo(150.0))));
    }

    @Test
    @DisplayName("Should return 400 when price range is invalid")
    void shouldReturn400WhenPriceRangeInvalid() throws Exception {
        // When & Then - maxPrice less than minPrice
        mockMvc.perform(get("/api/products/price-range")
                        .param("minPrice", "100.00")
                        .param("maxPrice", "50.00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when price is zero or negative")
    void shouldReturn400WhenPriceZeroOrNegative() throws Exception {
        // When & Then - negative minPrice
        mockMvc.perform(get("/api/products/price-range")
                        .param("minPrice", "-10.00")
                        .param("maxPrice", "100.00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get available servers successfully")
    void shouldGetAvailableServersSuccessfully() throws Exception {
        // Given
        List<String> servers = Arrays.asList("Stormrage", "Area-52", "Tichondrius");
        when(productService.getAvailableServers()).thenReturn(servers);

        // When & Then
        mockMvc.perform(get("/api/products/servers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$", hasItems("Stormrage", "Area-52", "Tichondrius")));
    }

    @Test
    @DisplayName("Should handle pagination parameters correctly")
    void shouldHandlePaginationParametersCorrectly() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(1, 10), 25);
        
        when(productService.findActiveProducts(any(Pageable.class))).thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("sortDirection", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(1))) // page number
                .andExpect(jsonPath("$.size", is(10))) // page size
                .andExpect(jsonPath("$.totalElements", is(25))); // total elements
    }

    @Test
    @DisplayName("Should return 400 for invalid pagination parameters")
    void shouldReturn400ForInvalidPaginationParameters() throws Exception {
        // When & Then - negative page number
        mockMvc.perform(get("/api/products")
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // When & Then - page size too large
        mockMvc.perform(get("/api/products")
                        .param("size", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // When & Then - page size too small
        mockMvc.perform(get("/api/products")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle empty results gracefully")
    void shouldHandleEmptyResultsGracefully() throws Exception {
        // Given
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(productService.findActiveProducts(any(Pageable.class))).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.totalPages", is(0)));
    }

    @Test
    @DisplayName("Should use default parameters correctly")
    void shouldUseDefaultParametersCorrectly() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.findActiveProducts(any(Pageable.class))).thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then - Test without any parameters (should use defaults)
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(0))) // default page
                .andExpect(jsonPath("$.size", is(20))); // default size
    }

    @Test
    @DisplayName("Should handle special characters in search correctly")
    void shouldHandleSpecialCharactersInSearchCorrectly() throws Exception {
        // Given
        String searchTerm = "World of Warcraft";
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productService.searchProductsByName(eq(searchTerm), eq(true), any(Pageable.class)))
                .thenReturn(productPage);
        when(productService.getProductStockInfo(productId)).thenReturn(stockInfo);

        // When & Then
        mockMvc.perform(get("/api/products/search/name")
                        .param("name", searchTerm)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }
}