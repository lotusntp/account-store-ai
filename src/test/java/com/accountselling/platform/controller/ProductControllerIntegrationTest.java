package com.accountselling.platform.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.config.TestRateLimitConfig;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.repository.CategoryRepository;
import com.accountselling.platform.repository.ProductRepository;
import com.accountselling.platform.repository.StockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for ProductController. Tests complete functionality with real database and
 * application context. Rate limiting is disabled for these tests using TestRateLimitConfig.
 *
 * <p>Integration tests สำหรับ ProductController ทดสอบฟังก์ชันการทำงานแบบครบถ้วนด้วยฐานข้อมูลจริงและ
 * application context มีการปิด rate limiting สำหรับ tests เหล่านี้โดยใช้ TestRateLimitConfig
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestRateLimitConfig.class)
@TestPropertySource(
    properties = {
      "spring.profiles.active=test",
      "logging.level.com.accountselling.platform.security.RateLimitingFilter=DEBUG"
    })
@Transactional
class ProductControllerIntegrationTest {

  @Autowired private WebApplicationContext context;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private ProductRepository productRepository;

  @Autowired private StockRepository stockRepository;

  private MockMvc mockMvc;

  private Category gamingCategory;
  private Category mmorpgCategory;
  private Category fpsCategory;
  private Product wowProduct;
  private Product csgoProduct;
  private Product inactiveProduct;
  private Stock wowStock1;
  private Stock wowStock2;
  private Stock csgoStock;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Clear existing data
    stockRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();

    // Create test data
    createTestCategories();
    createTestProducts();
    createTestStock();
  }

  private void createTestCategories() {
    // Create root gaming category
    gamingCategory = new Category();
    gamingCategory.setName("Gaming");
    gamingCategory.setDescription("Gaming accounts and items");
    gamingCategory.setActive(true);
    gamingCategory.setSortOrder(1);
    gamingCategory = categoryRepository.save(gamingCategory);

    // Create MMORPG subcategory
    mmorpgCategory = new Category();
    mmorpgCategory.setName("MMORPG");
    mmorpgCategory.setDescription("Massively Multiplayer Online Role-Playing Games");
    mmorpgCategory.setActive(true);
    mmorpgCategory.setSortOrder(1);
    mmorpgCategory.setParentCategory(gamingCategory);
    mmorpgCategory = categoryRepository.save(mmorpgCategory);

    // Create FPS subcategory
    fpsCategory = new Category();
    fpsCategory.setName("FPS");
    fpsCategory.setDescription("First Person Shooter Games");
    fpsCategory.setActive(true);
    fpsCategory.setSortOrder(2);
    fpsCategory.setParentCategory(gamingCategory);
    fpsCategory = categoryRepository.save(fpsCategory);
  }

  private void createTestProducts() {
    // Create WoW product
    wowProduct = new Product();
    wowProduct.setName("World of Warcraft Account");
    wowProduct.setDescription("High-level WoW account with rare items and achievements");
    wowProduct.setPrice(new BigDecimal("99.99"));
    wowProduct.setServer("Stormrage");
    wowProduct.setImageUrl("http://example.com/wow.jpg");
    wowProduct.setCategory(mmorpgCategory);
    wowProduct.setActive(true);
    wowProduct.setSortOrder(1);
    wowProduct.setLowStockThreshold(5);
    wowProduct = productRepository.save(wowProduct);

    // Create CS:GO product
    csgoProduct = new Product();
    csgoProduct.setName("CS:GO Prime Account");
    csgoProduct.setDescription("Counter-Strike Global Offensive Prime account");
    csgoProduct.setPrice(new BigDecimal("29.99"));
    csgoProduct.setServer("Global");
    csgoProduct.setImageUrl("http://example.com/csgo.jpg");
    csgoProduct.setCategory(fpsCategory);
    csgoProduct.setActive(true);
    csgoProduct.setSortOrder(2);
    csgoProduct.setLowStockThreshold(10);
    csgoProduct = productRepository.save(csgoProduct);

    // Create inactive product
    inactiveProduct = new Product();
    inactiveProduct.setName("Discontinued Game Account");
    inactiveProduct.setDescription("Account for a discontinued game");
    inactiveProduct.setPrice(new BigDecimal("19.99"));
    inactiveProduct.setServer("Offline");
    inactiveProduct.setImageUrl("http://example.com/inactive.jpg");
    inactiveProduct.setCategory(gamingCategory);
    inactiveProduct.setActive(false);
    inactiveProduct.setSortOrder(3);
    inactiveProduct = productRepository.save(inactiveProduct);
  }

  private void createTestStock() {
    // Create stock for WoW product
    wowStock1 = new Stock();
    wowStock1.setProduct(wowProduct);
    wowStock1.setCredentials("wow_user1:password123");
    wowStock1.setSold(false);
    wowStock1 = stockRepository.save(wowStock1);

    wowStock2 = new Stock();
    wowStock2.setProduct(wowProduct);
    wowStock2.setCredentials("wow_user2:password456");
    wowStock2.setSold(true);
    wowStock2 = stockRepository.save(wowStock2);

    // Create stock for CS:GO product
    csgoStock = new Stock();
    csgoStock.setProduct(csgoProduct);
    csgoStock.setCredentials("csgo_user1:password789");
    csgoStock.setSold(false);
    csgoStock = stockRepository.save(csgoStock);
  }

  @Test
  @DisplayName("Should get all active products successfully")
  void shouldGetAllActiveProductsSuccessfully() throws Exception {
    mockMvc
        .perform(
            get("/api/products")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))) // Only active products
        .andExpect(
            jsonPath(
                "$.content[*].name", hasItems("World of Warcraft Account", "CS:GO Prime Account")))
        .andExpect(jsonPath("$.content[*].active", everyItem(is(true))))
        .andExpect(jsonPath("$.totalElements", is(2)))
        .andExpect(jsonPath("$.size", is(20)))
        .andExpect(jsonPath("$.number", is(0)));
  }

  @Test
  @DisplayName("Should get all products including inactive when activeOnly is false")
  void shouldGetAllProductsIncludingInactive() throws Exception {
    mockMvc
        .perform(
            get("/api/products")
                .param("activeOnly", "false")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3))) // Including inactive product
        .andExpect(
            jsonPath(
                "$.content[*].name",
                hasItems(
                    "World of Warcraft Account",
                    "CS:GO Prime Account",
                    "Discontinued Game Account")))
        .andExpect(
            jsonPath(
                "$.content[?(@.name == 'Discontinued Game Account')].active", contains(false)));
  }

  @Test
  @DisplayName("Should get products with available stock when inStockOnly is true")
  void shouldGetProductsWithAvailableStockWhenInStockOnly() throws Exception {
    mockMvc
        .perform(
            get("/api/products")
                .param("inStockOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))) // Products with available stock
        .andExpect(jsonPath("$.content[*].stock.inStock", everyItem(is(true))))
        .andExpect(jsonPath("$.content[*].stock.availableStock", everyItem(greaterThan(0))));
  }

  @Test
  @DisplayName("Should get product by ID successfully")
  void shouldGetProductByIdSuccessfully() throws Exception {
    mockMvc
        .perform(
            get("/api/products/{id}", wowProduct.getId()).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(wowProduct.getId().toString())))
        .andExpect(jsonPath("$.name", is("World of Warcraft Account")))
        .andExpect(
            jsonPath(
                "$.description", is("High-level WoW account with rare items and achievements")))
        .andExpect(jsonPath("$.price", is(99.99)))
        .andExpect(jsonPath("$.formattedPrice", is("99.99")))
        .andExpect(jsonPath("$.server", is("Stormrage")))
        .andExpect(jsonPath("$.active", is(true)))
        .andExpect(jsonPath("$.category.id", is(mmorpgCategory.getId().toString())))
        .andExpect(jsonPath("$.category.name", is("MMORPG")))
        .andExpect(jsonPath("$.category.fullPath", is("Gaming > MMORPG")))
        .andExpect(jsonPath("$.stock.totalStock", is(2)))
        .andExpect(jsonPath("$.stock.availableStock", is(1)))
        .andExpect(jsonPath("$.stock.soldStock", is(1)))
        .andExpect(jsonPath("$.stock.inStock", is(true)))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.updatedAt", notNullValue()));
  }

  @Test
  @DisplayName("Should return 404 when product not found")
  void shouldReturn404WhenProductNotFound() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/products/{id}", nonExistentId).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)))
        .andExpect(jsonPath("$.error", is("Not Found")))
        .andExpect(jsonPath("$.message", containsString("not found")));
  }

  @Test
  @DisplayName("Should get products by category successfully")
  void shouldGetProductsByCategorySuccessfully() throws Exception {
    mockMvc
        .perform(
            get("/api/products/category/{categoryId}", mmorpgCategory.getId())
                .param("includeSubcategories", "false")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("World of Warcraft Account")))
        .andExpect(jsonPath("$.content[0].category.id", is(mmorpgCategory.getId().toString())))
        .andExpect(jsonPath("$.content[0].category.name", is("MMORPG")));
  }

  @Test
  @DisplayName("Should get products by category with subcategories")
  void shouldGetProductsByCategoryWithSubcategories() throws Exception {
    mockMvc
        .perform(
            get("/api/products/category/{categoryId}", gamingCategory.getId())
                .param("includeSubcategories", "true")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))) // Products from subcategories
        .andExpect(
            jsonPath(
                "$.content[*].name", hasItems("World of Warcraft Account", "CS:GO Prime Account")));
  }

  @Test
  @DisplayName("Should return empty list when category has no products")
  void shouldReturnEmptyListWhenCategoryHasNoProducts() throws Exception {
    // Create category without products
    Category emptyCategory = new Category();
    emptyCategory.setName("Empty Category");
    emptyCategory.setDescription("Category with no products");
    emptyCategory.setActive(true);
    emptyCategory = categoryRepository.save(emptyCategory);

    mockMvc
        .perform(
            get("/api/products/category/{categoryId}", emptyCategory.getId())
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)))
        .andExpect(jsonPath("$.totalElements", is(0)));
  }

  @Test
  @DisplayName("Should search products successfully")
  void shouldSearchProductsSuccessfully() throws Exception {
    mockMvc
        .perform(
            get("/api/products/search")
                .param("name", "warcraft")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", containsStringIgnoringCase("warcraft")));
  }

  @Test
  @DisplayName("Should search products with multiple criteria")
  void shouldSearchProductsWithMultipleCriteria() throws Exception {
    mockMvc
        .perform(
            get("/api/products/search")
                .param("categoryId", mmorpgCategory.getId().toString())
                .param("minPrice", "50.00")
                .param("maxPrice", "150.00")
                .param("server", "Stormrage")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("World of Warcraft Account")))
        .andExpect(jsonPath("$.content[0].server", is("Stormrage")))
        .andExpect(
            jsonPath(
                "$.content[0].price", allOf(greaterThanOrEqualTo(50.0), lessThanOrEqualTo(150.0))));
  }

  @Test
  @DisplayName("Should return 400 when search has invalid price range")
  void shouldReturn400WhenSearchHasInvalidPriceRange() throws Exception {
    mockMvc
        .perform(
            get("/api/products/search")
                .param("minPrice", "100.00")
                .param("maxPrice", "50.00")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(
            jsonPath(
                "$.message",
                containsString("Maximum price must be greater than or equal to minimum price")));
  }

  @Test
  @DisplayName("Should search products by name successfully")
  void shouldSearchProductsByNameSuccessfully() throws Exception {
    mockMvc
        .perform(
            get("/api/products/search/name")
                .param("name", "CS")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", containsStringIgnoringCase("CS")));
  }

  @Test
  @DisplayName("Should search products case-insensitively")
  void shouldSearchProductsCaseInsensitively() throws Exception {
    mockMvc
        .perform(
            get("/api/products/search/name")
                .param("name", "WARCRAFT")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", containsStringIgnoringCase("warcraft")));
  }

  @Test
  @DisplayName("Should return empty results when no products match search")
  void shouldReturnEmptyResultsWhenNoProductsMatchSearch() throws Exception {
    mockMvc
        .perform(
            get("/api/products/search/name")
                .param("name", "nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)))
        .andExpect(jsonPath("$.totalElements", is(0)));
  }

  @Test
  @DisplayName("Should return 400 when search name is too short")
  void shouldReturn400WhenSearchNameTooShort() throws Exception {
    mockMvc
        .perform(
            get("/api/products/search/name")
                .param("name", "a")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.message", containsString("at least 2 characters")));
  }

  @Test
  @DisplayName("Should get products by server successfully")
  void shouldGetProductsByServerSuccessfully() throws Exception {
    mockMvc
        .perform(
            get("/api/products/server/{server}", "Stormrage")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].server", is("Stormrage")))
        .andExpect(jsonPath("$.content[0].name", is("World of Warcraft Account")));
  }

  @Test
  @DisplayName("Should get products by price range successfully")
  void shouldGetProductsByPriceRangeSuccessfully() throws Exception {
    mockMvc
        .perform(
            get("/api/products/price-range")
                .param("minPrice", "20.00")
                .param("maxPrice", "50.00")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("CS:GO Prime Account")))
        .andExpect(jsonPath("$.content[0].price", is(29.99)));
  }

  @Test
  @DisplayName("Should return 400 when price range is invalid")
  void shouldReturn400WhenPriceRangeInvalid() throws Exception {
    mockMvc
        .perform(
            get("/api/products/price-range")
                .param("minPrice", "100.00")
                .param("maxPrice", "50.00")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(
            jsonPath(
                "$.message",
                containsString("Maximum price must be greater than or equal to minimum price")));
  }

  @Test
  @DisplayName("Should get available servers successfully")
  void shouldGetAvailableServersSuccessfully() throws Exception {
    mockMvc
        .perform(get("/api/products/servers").contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThan(0))))
        .andExpect(jsonPath("$", hasItems("Stormrage", "Global")));
  }

  @Test
  @DisplayName("Should handle pagination correctly")
  void shouldHandlePaginationCorrectly() throws Exception {
    mockMvc
        .perform(
            get("/api/products")
                .param("page", "0")
                .param("size", "1")
                .param("sortBy", "name")
                .param("sortDirection", "asc")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.size", is(1)))
        .andExpect(jsonPath("$.number", is(0)))
        .andExpect(jsonPath("$.totalElements", is(2)))
        .andExpect(jsonPath("$.totalPages", is(2)));
  }

  @Test
  @DisplayName("Should sort products correctly")
  void shouldSortProductsCorrectly() throws Exception {
    // Sort by price ascending
    mockMvc
        .perform(
            get("/api/products")
                .param("sortBy", "price")
                .param("sortDirection", "asc")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].price", lessThan(100.0)))
        .andExpect(jsonPath("$.content[0].name", is("CS:GO Prime Account")))
        .andExpect(jsonPath("$.content[1].name", is("World of Warcraft Account")));
  }

  @Test
  @DisplayName("Should return 400 for invalid pagination parameters")
  void shouldReturn400ForInvalidPaginationParameters() throws Exception {
    // Negative page number
    mockMvc
        .perform(get("/api/products").param("page", "-1").contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.message", containsString("Page number cannot be negative")));

    // Page size too large
    mockMvc
        .perform(get("/api/products").param("size", "101").contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.message", containsString("Page size must be between 1 and 100")));
  }

  @Test
  @DisplayName("Should include correct stock information")
  void shouldIncludeCorrectStockInformation() throws Exception {
    mockMvc
        .perform(
            get("/api/products/{id}", wowProduct.getId()).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock.totalStock", is(2))) // 2 stock items total
        .andExpect(jsonPath("$.stock.availableStock", is(1))) // 1 not sold
        .andExpect(jsonPath("$.stock.soldStock", is(1))) // 1 sold
        .andExpect(jsonPath("$.stock.inStock", is(true))) // has available stock
        .andExpect(jsonPath("$.stock.lowStockThreshold", is(5)));

    mockMvc
        .perform(
            get("/api/products/{id}", csgoProduct.getId()).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock.totalStock", is(1)))
        .andExpect(jsonPath("$.stock.availableStock", is(1)))
        .andExpect(jsonPath("$.stock.soldStock", is(0)))
        .andExpect(jsonPath("$.stock.inStock", is(true)));
  }

  @Test
  @DisplayName("Should handle concurrent requests gracefully")
  void shouldHandleConcurrentRequestsGracefully() throws Exception {
    // Simulate multiple concurrent requests
    for (int i = 0; i < 5; i++) {
      mockMvc
          .perform(get("/api/products").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
    }
  }

  @Test
  @DisplayName("Should maintain data consistency across multiple operations")
  void shouldMaintainDataConsistencyAcrossMultipleOperations() throws Exception {
    // First request - get all products
    mockMvc
        .perform(
            get("/api/products")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", is(2)));

    // Second request - get specific product
    mockMvc
        .perform(
            get("/api/products/{id}", wowProduct.getId()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("World of Warcraft Account")));

    // Third request - search products
    mockMvc
        .perform(
            get("/api/products/search/name")
                .param("name", "warcraft")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)));

    // Fourth request - get products by category
    mockMvc
        .perform(
            get("/api/products/category/{categoryId}", mmorpgCategory.getId())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)));
  }
}
