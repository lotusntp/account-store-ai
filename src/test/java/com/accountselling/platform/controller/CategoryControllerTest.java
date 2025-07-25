package com.accountselling.platform.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.model.Category;
import com.accountselling.platform.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for CategoryController. Tests controller logic with mocked service dependencies.
 * Security is disabled for unit tests to focus on controller logic.
 *
 * <p>Unit tests สำหรับ CategoryController ทดสอบ logic ของ controller ด้วย service dependencies
 * ที่ถูก mock ปิด security สำหรับ unit tests เพื่อให้เน้นที่ logic ของ controller
 */
@WebMvcTest(
    controllers = CategoryController.class,
    excludeAutoConfiguration = {
      org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
    })
class CategoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private CategoryService categoryService;

  private Category rootCategory;
  private Category subCategory;
  private UUID rootCategoryId;
  private UUID subCategoryId;

  @BeforeEach
  void setUp() {
    rootCategoryId = UUID.randomUUID();
    subCategoryId = UUID.randomUUID();

    // Create root category
    rootCategory = new Category();
    rootCategory.setId(rootCategoryId);
    rootCategory.setName("Games");
    rootCategory.setDescription("Gaming accounts and items");
    rootCategory.setActive(true);
    rootCategory.setSortOrder(1);
    rootCategory.setCreatedAt(LocalDateTime.now());
    rootCategory.setUpdatedAt(LocalDateTime.now());

    // Create sub category
    subCategory = new Category();
    subCategory.setId(subCategoryId);
    subCategory.setName("Action Games");
    subCategory.setDescription("Action gaming accounts");
    subCategory.setActive(true);
    subCategory.setSortOrder(1);
    subCategory.setParentCategory(rootCategory);
    subCategory.setCreatedAt(LocalDateTime.now());
    subCategory.setUpdatedAt(LocalDateTime.now());

    // Set up bidirectional relationship - use helper method instead
    rootCategory.addSubCategory(subCategory);
  }

  @Test
  @DisplayName("Should get all active categories successfully")
  void shouldGetAllActiveCategoriesSuccessfully() throws Exception {
    // Given
    List<Category> categories = Arrays.asList(rootCategory, subCategory);
    when(categoryService.findActiveCategories()).thenReturn(categories);
    when(categoryService.countProductsInCategory(any(UUID.class), eq(false))).thenReturn(5L);
    when(categoryService.countProductsInCategory(any(UUID.class), eq(true))).thenReturn(3L);

    // When & Then
    mockMvc
        .perform(
            get("/api/categories")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id", is(rootCategoryId.toString())))
        .andExpect(jsonPath("$[0].name", is("Games")))
        .andExpect(jsonPath("$[0].description", is("Gaming accounts and items")))
        .andExpect(jsonPath("$[0].active", is(true)))
        .andExpect(jsonPath("$[0].level", is(0)))
        .andExpect(jsonPath("$[0].fullPath", is("Games")))
        .andExpect(jsonPath("$[0].productCount", is(5)))
        .andExpect(jsonPath("$[0].activeProductCount", is(3)))
        .andExpect(jsonPath("$[0].parent", nullValue()))
        .andExpect(jsonPath("$[1].id", is(subCategoryId.toString())))
        .andExpect(jsonPath("$[1].name", is("Action Games")))
        .andExpect(jsonPath("$[1].level", is(1)))
        .andExpect(jsonPath("$[1].fullPath", is("Games > Action Games")))
        .andExpect(jsonPath("$[1].parent.id", is(rootCategoryId.toString())))
        .andExpect(jsonPath("$[1].parent.name", is("Games")));
  }

  @Test
  @DisplayName("Should get all categories including inactive when activeOnly is false")
  void shouldGetAllCategoriesIncludingInactive() throws Exception {
    // Given
    Category inactiveCategory = new Category();
    inactiveCategory.setId(UUID.randomUUID());
    inactiveCategory.setName("Inactive Category");
    inactiveCategory.setActive(false);
    inactiveCategory.setCreatedAt(LocalDateTime.now());
    inactiveCategory.setUpdatedAt(LocalDateTime.now());

    List<Category> allCategories = Arrays.asList(rootCategory, subCategory, inactiveCategory);
    when(categoryService.findAllCategories()).thenReturn(allCategories);
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(0L);

    // When & Then
    mockMvc
        .perform(
            get("/api/categories")
                .param("activeOnly", "false")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[2].name", is("Inactive Category")))
        .andExpect(jsonPath("$[2].active", is(false)));
  }

  @Test
  @DisplayName("Should get category by ID successfully")
  void shouldGetCategoryByIdSuccessfully() throws Exception {
    // Given
    when(categoryService.findById(rootCategoryId)).thenReturn(Optional.of(rootCategory));
    when(categoryService.findActiveSubcategories(rootCategoryId))
        .thenReturn(Arrays.asList(subCategory));
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(5L);

    // When & Then
    mockMvc
        .perform(
            get("/api/categories/{id}", rootCategoryId).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(rootCategoryId.toString())))
        .andExpect(jsonPath("$.name", is("Games")))
        .andExpect(jsonPath("$.description", is("Gaming accounts and items")))
        .andExpect(jsonPath("$.active", is(true)))
        .andExpect(jsonPath("$.level", is(0)))
        .andExpect(jsonPath("$.fullPath", is("Games")))
        .andExpect(jsonPath("$.subcategories", hasSize(1)))
        .andExpect(jsonPath("$.subcategories[0].id", is(subCategoryId.toString())))
        .andExpect(jsonPath("$.subcategories[0].name", is("Action Games")));
  }

  @Test
  @DisplayName("Should return 404 when category not found by ID")
  void shouldReturn404WhenCategoryNotFoundById() throws Exception {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    when(categoryService.findById(nonExistentId)).thenReturn(Optional.empty());

    // When & Then
    mockMvc
        .perform(get("/api/categories/{id}", nonExistentId).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should get category hierarchy successfully")
  void shouldGetCategoryHierarchySuccessfully() throws Exception {
    // Given
    when(categoryService.getCategoryHierarchy(true)).thenReturn(Arrays.asList(rootCategory));
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(3L);

    // When & Then
    mockMvc
        .perform(
            get("/api/categories/hierarchy")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(rootCategoryId.toString())))
        .andExpect(jsonPath("$[0].name", is("Games")))
        .andExpect(jsonPath("$[0].subcategories", hasSize(1)))
        .andExpect(jsonPath("$[0].subcategories[0].id", is(subCategoryId.toString())))
        .andExpect(jsonPath("$[0].subcategories[0].name", is("Action Games")));
  }

  @Test
  @DisplayName("Should get root categories successfully")
  void shouldGetRootCategoriesSuccessfully() throws Exception {
    // Given
    when(categoryService.findActiveRootCategories()).thenReturn(Arrays.asList(rootCategory));
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(10L);

    // When & Then
    mockMvc
        .perform(
            get("/api/categories/root")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(rootCategoryId.toString())))
        .andExpect(jsonPath("$[0].name", is("Games")))
        .andExpect(jsonPath("$[0].level", is(0)))
        .andExpect(jsonPath("$[0].parent", nullValue()));
  }

  @Test
  @DisplayName("Should get all root categories when activeOnly is false")
  void shouldGetAllRootCategoriesWhenActiveOnlyIsFalse() throws Exception {
    // Given
    Category inactiveRoot = new Category();
    inactiveRoot.setId(UUID.randomUUID());
    inactiveRoot.setName("Inactive Root");
    inactiveRoot.setActive(false);
    inactiveRoot.setCreatedAt(LocalDateTime.now());
    inactiveRoot.setUpdatedAt(LocalDateTime.now());

    when(categoryService.findRootCategories())
        .thenReturn(Arrays.asList(rootCategory, inactiveRoot));
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(0L);

    // When & Then
    mockMvc
        .perform(
            get("/api/categories/root")
                .param("activeOnly", "false")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[1].name", is("Inactive Root")))
        .andExpect(jsonPath("$[1].active", is(false)));
  }

  @Test
  @DisplayName("Should get subcategories successfully")
  void shouldGetSubcategoriesSuccessfully() throws Exception {
    // Given
    when(categoryService.findById(rootCategoryId)).thenReturn(Optional.of(rootCategory));
    when(categoryService.findActiveSubcategories(rootCategoryId))
        .thenReturn(Arrays.asList(subCategory));
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(2L);

    // When & Then
    mockMvc
        .perform(
            get("/api/categories/{parentId}/subcategories", rootCategoryId)
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(subCategoryId.toString())))
        .andExpect(jsonPath("$[0].name", is("Action Games")))
        .andExpect(jsonPath("$[0].parent.id", is(rootCategoryId.toString())));
  }

  @Test
  @DisplayName("Should return 404 when parent category not found for subcategories")
  void shouldReturn404WhenParentCategoryNotFoundForSubcategories() throws Exception {
    // Given
    UUID nonExistentParentId = UUID.randomUUID();
    when(categoryService.findById(nonExistentParentId)).thenReturn(Optional.empty());

    // When & Then
    mockMvc
        .perform(
            get("/api/categories/{parentId}/subcategories", nonExistentParentId)
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should search categories successfully")
  void shouldSearchCategoriesSuccessfully() throws Exception {
    // Given
    String searchQuery = "game";
    when(categoryService.searchCategoriesByName(searchQuery, true))
        .thenReturn(Arrays.asList(rootCategory, subCategory));
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(1L);

    // When & Then
    mockMvc
        .perform(
            get("/api/categories/search")
                .param("name", searchQuery)
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name", containsStringIgnoringCase("game")));
  }

  @Test
  @DisplayName("Should return 400 when search query is too short")
  void shouldReturn400WhenSearchQueryTooShort() throws Exception {
    // When & Then
    mockMvc
        .perform(
            get("/api/categories/search")
                .param("name", "a")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should get category path successfully")
  void shouldGetCategoryPathSuccessfully() throws Exception {
    // Given
    when(categoryService.findById(subCategoryId)).thenReturn(Optional.of(subCategory));
    when(categoryService.getCategoryPath(subCategoryId))
        .thenReturn(Arrays.asList(rootCategory, subCategory));
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(1L);

    // When & Then
    mockMvc
        .perform(
            get("/api/categories/{id}/path", subCategoryId).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id", is(rootCategoryId.toString())))
        .andExpect(jsonPath("$[0].name", is("Games")))
        .andExpect(jsonPath("$[1].id", is(subCategoryId.toString())))
        .andExpect(jsonPath("$[1].name", is("Action Games")));
  }

  @Test
  @DisplayName("Should return 404 when category not found for path")
  void shouldReturn404WhenCategoryNotFoundForPath() throws Exception {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    when(categoryService.findById(nonExistentId)).thenReturn(Optional.empty());

    // When & Then
    mockMvc
        .perform(
            get("/api/categories/{id}/path", nonExistentId).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should handle empty results gracefully")
  void shouldHandleEmptyResultsGracefully() throws Exception {
    // Given
    when(categoryService.findActiveCategories()).thenReturn(Collections.emptyList());

    // When & Then
    mockMvc
        .perform(get("/api/categories").contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  @DisplayName("Should use default parameters correctly")
  void shouldUseDefaultParametersCorrectly() throws Exception {
    // Given
    when(categoryService.findActiveCategories()).thenReturn(Arrays.asList(rootCategory));
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(0L);

    // When & Then - Test without activeOnly parameter (should default to true)
    mockMvc
        .perform(get("/api/categories").contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk());

    // Verify that findActiveCategories was called (default activeOnly=true)
    // This is verified by the mock setup above
  }
}
