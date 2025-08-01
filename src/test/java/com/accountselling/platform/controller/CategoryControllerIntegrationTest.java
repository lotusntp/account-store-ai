package com.accountselling.platform.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.model.Category;
import com.accountselling.platform.service.CategoryService;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for CategoryController. Tests controller logic with mocked service
 * dependencies.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CategoryService categoryService;

  private Category rootCategory;
  private Category subCategory;
  private UUID rootCategoryId;
  private UUID subCategoryId;

  @BeforeEach
  void setUp() {
    rootCategoryId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    subCategoryId = UUID.fromString("22222222-2222-2222-2222-222222222222");

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

    // Set up bidirectional relationship
    rootCategory.addSubCategory(subCategory);
  }

  @Test
  @DisplayName("Should get all active categories successfully")
  @WithMockUser
  void shouldGetAllActiveCategoriesSuccessfully() throws Exception {
    // Given
    List<Category> categories = Arrays.asList(rootCategory, subCategory);
    when(categoryService.findActiveCategories()).thenReturn(categories);
    when(categoryService.countProductsInCategory(rootCategoryId, false)).thenReturn(5L);
    when(categoryService.countProductsInCategory(rootCategoryId, true)).thenReturn(3L);
    when(categoryService.countProductsInCategory(subCategoryId, false)).thenReturn(2L);
    when(categoryService.countProductsInCategory(subCategoryId, true)).thenReturn(1L);

    // When & Then
    mockMvc
        .perform(
            get("/api/categories")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id", is(rootCategoryId.toString())))
        .andExpect(jsonPath("$[0].name", is("Games")))
        .andExpect(jsonPath("$[0].description", is("Gaming accounts and items")))
        .andExpect(jsonPath("$[0].active", is(true)))
        .andExpect(jsonPath("$[0].level", is(0)))
        .andExpect(jsonPath("$[0].fullPath", is("Games")))
        .andExpect(jsonPath("$[0].productCount", is(5)))
        .andExpect(jsonPath("$[0].activeProductCount", is(3)))
        .andExpect(jsonPath("$[0].parent", nullValue()));
  }

  @Test
  @DisplayName("Should get category by ID successfully")
  @WithMockUser
  void shouldGetCategoryByIdSuccessfully() throws Exception {
    // Given
    when(categoryService.findById(rootCategoryId)).thenReturn(Optional.of(rootCategory));
    when(categoryService.countProductsInCategory(rootCategoryId, false)).thenReturn(5L);
    when(categoryService.countProductsInCategory(rootCategoryId, true)).thenReturn(3L);
    when(categoryService.findActiveSubcategories(rootCategoryId))
        .thenReturn(Arrays.asList(subCategory));

    // When & Then
    mockMvc
        .perform(
            get("/api/categories/{id}", rootCategoryId).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(rootCategoryId.toString())))
        .andExpect(jsonPath("$.name", is("Games")))
        .andExpect(jsonPath("$.description", is("Gaming accounts and items")))
        .andExpect(jsonPath("$.active", is(true)));
  }

  @Test
  @DisplayName("Should return 404 when category not found by ID")
  @WithMockUser
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
  @DisplayName("Should handle empty results gracefully")
  @WithMockUser
  void shouldHandleEmptyResultsGracefully() throws Exception {
    // Given
    when(categoryService.findActiveCategories()).thenReturn(Collections.emptyList());

    // When & Then
    mockMvc
        .perform(
            get("/api/categories")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
