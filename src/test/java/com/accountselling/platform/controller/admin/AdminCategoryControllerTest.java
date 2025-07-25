package com.accountselling.platform.controller.admin;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.config.TestSecurityConfig;
import com.accountselling.platform.dto.category.CategoryCreateRequestDto;
import com.accountselling.platform.dto.category.CategoryUpdateRequestDto;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Comprehensive unit tests for AdminCategoryController. Tests all CRUD operations, validation,
 * authorization, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(AdminCategoryController.class)
@Import(TestSecurityConfig.class)
class AdminCategoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CategoryService categoryService;

  @Autowired private ObjectMapper objectMapper;

  private Category testCategory;
  private Category parentCategory;
  private Category childCategory;
  private CategoryCreateRequestDto createRequest;
  private CategoryUpdateRequestDto updateRequest;

  @BeforeEach
  void setUp() {
    // Setup parent category
    parentCategory = new Category();
    parentCategory.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    parentCategory.setName("Gaming");
    parentCategory.setDescription("Gaming categories");
    parentCategory.setActive(true);
    parentCategory.setSortOrder(1);
    parentCategory.setCreatedAt(LocalDateTime.now().minusDays(10));
    parentCategory.setUpdatedAt(LocalDateTime.now().minusDays(5));

    // Setup test category
    testCategory = new Category();
    testCategory.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    testCategory.setName("Action Games");
    testCategory.setDescription("Action gaming accounts");
    testCategory.setActive(true);
    testCategory.setSortOrder(2);
    testCategory.setParentCategory(parentCategory);
    testCategory.setCreatedAt(LocalDateTime.now().minusDays(5));
    testCategory.setUpdatedAt(LocalDateTime.now().minusDays(1));

    // Setup child category
    childCategory = new Category();
    childCategory.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174002"));
    childCategory.setName("FPS Games");
    childCategory.setDescription("First-person shooter games");
    childCategory.setActive(true);
    childCategory.setSortOrder(1);
    childCategory.setParentCategory(testCategory);
    childCategory.setCreatedAt(LocalDateTime.now().minusDays(3));
    childCategory.setUpdatedAt(LocalDateTime.now().minusDays(1));

    // Setup DTOs
    createRequest = new CategoryCreateRequestDto();
    createRequest.setName("New Category");
    createRequest.setDescription("New category description");
    createRequest.setParentId(parentCategory.getId());
    createRequest.setSortOrder(3);

    updateRequest = new CategoryUpdateRequestDto();
    updateRequest.setName("Updated Category");
    updateRequest.setDescription("Updated description");
    updateRequest.setSortOrder(5);
    updateRequest.setActive(true);
  }

  // ==================== GET ALL CATEGORIES TESTS ====================

  @Test
  @DisplayName("Get all categories - Success")
  @WithMockUser(roles = "ADMIN")
  void getAllCategories_Success() throws Exception {
    // Arrange
    List<Category> categories = Arrays.asList(parentCategory, testCategory, childCategory);
    when(categoryService.findAllCategories()).thenReturn(categories);
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(5L);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/categories").param("activeOnly", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].id").value(parentCategory.getId().toString()))
        .andExpect(jsonPath("$[0].name").value("Gaming"))
        .andExpect(jsonPath("$[0].active").value(true))
        .andExpect(jsonPath("$[1].id").value(testCategory.getId().toString()))
        .andExpect(jsonPath("$[1].name").value("Action Games"))
        .andExpect(jsonPath("$[1].parent.id").value(parentCategory.getId().toString()));

    verify(categoryService).findAllCategories();
    verify(categoryService, times(3)).countProductsInCategory(any(UUID.class), eq(false));
    verify(categoryService, times(3)).countProductsInCategory(any(UUID.class), eq(true));
  }

  @Test
  @DisplayName("Get active categories only - Success")
  @WithMockUser(roles = "ADMIN")
  void getActiveCategoriesOnly_Success() throws Exception {
    // Arrange
    List<Category> activeCategories = Arrays.asList(parentCategory, testCategory);
    when(categoryService.findActiveCategories()).thenReturn(activeCategories);
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(3L);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/categories").param("activeOnly", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].active").value(true))
        .andExpect(jsonPath("$[1].active").value(true));

    verify(categoryService).findActiveCategories();
    verify(categoryService, never()).findAllCategories();
  }

  @Test
  @DisplayName("Get all categories - Unauthorized")
  void getAllCategories_Unauthorized() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/categories")).andExpect(status().isForbidden());

    verify(categoryService, never()).findAllCategories();
  }

  @Test
  @DisplayName("Get all categories - Forbidden for non-admin")
  @WithMockUser(roles = "USER")
  void getAllCategories_Forbidden() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/categories")).andExpect(status().isForbidden());

    verify(categoryService, never()).findAllCategories();
  }

  // ==================== GET CATEGORY BY ID TESTS ====================

  @Test
  @DisplayName("Get category by ID - Success")
  @WithMockUser(roles = "ADMIN")
  void getCategoryById_Success() throws Exception {
    // Arrange
    when(categoryService.findById(testCategory.getId()))
        .thenReturn(java.util.Optional.of(testCategory));
    when(categoryService.findSubcategories(testCategory.getId()))
        .thenReturn(Arrays.asList(childCategory));
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(2L);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/categories/{id}", testCategory.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testCategory.getId().toString()))
        .andExpect(jsonPath("$.name").value("Action Games"))
        .andExpect(jsonPath("$.description").value("Action gaming accounts"))
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.sortOrder").value(2))
        .andExpect(jsonPath("$.parent.id").value(parentCategory.getId().toString()))
        .andExpect(jsonPath("$.subcategories", hasSize(1)))
        .andExpect(jsonPath("$.subcategories[0].id").value(childCategory.getId().toString()));

    verify(categoryService).findById(testCategory.getId());
    verify(categoryService).findSubcategories(testCategory.getId());
  }

  @Test
  @DisplayName("Get category by ID - Not Found")
  @WithMockUser(roles = "ADMIN")
  void getCategoryById_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(categoryService.findById(nonExistentId)).thenReturn(java.util.Optional.empty());

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/categories/{id}", nonExistentId))
        .andExpect(status().isNotFound());

    verify(categoryService).findById(nonExistentId);
  }

  // ==================== CREATE CATEGORY TESTS ====================

  @Test
  @DisplayName("Create category - Success")
  @WithMockUser(roles = "ADMIN")
  void createCategory_Success() throws Exception {
    // Arrange
    when(categoryService.createCategory(
            createRequest.getName(),
            createRequest.getDescription(),
            createRequest.getParentId(),
            createRequest.getSortOrder()))
        .thenReturn(testCategory);
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(0L);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(testCategory.getId().toString()))
        .andExpect(jsonPath("$.name").value("Action Games"))
        .andExpect(jsonPath("$.active").value(true));

    verify(categoryService)
        .createCategory(
            createRequest.getName(),
            createRequest.getDescription(),
            createRequest.getParentId(),
            createRequest.getSortOrder());
  }

  @Test
  @DisplayName("Create category - Invalid input")
  @WithMockUser(roles = "ADMIN")
  void createCategory_InvalidInput() throws Exception {
    // Arrange
    CategoryCreateRequestDto invalidRequest = new CategoryCreateRequestDto();
    invalidRequest.setName(""); // Invalid: empty name
    invalidRequest.setDescription("Valid description");

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    verify(categoryService, never()).createCategory(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Create category - Name too long")
  @WithMockUser(roles = "ADMIN")
  void createCategory_NameTooLong() throws Exception {
    // Arrange
    CategoryCreateRequestDto invalidRequest = new CategoryCreateRequestDto();
    invalidRequest.setName("A".repeat(101)); // Invalid: name too long
    invalidRequest.setDescription("Valid description");

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    verify(categoryService, never()).createCategory(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Create category - Parent not found")
  @WithMockUser(roles = "ADMIN")
  void createCategory_ParentNotFound() throws Exception {
    // Arrange
    when(categoryService.createCategory(any(), any(), any(), any()))
        .thenThrow(
            new ResourceNotFoundException("Category", createRequest.getParentId().toString()));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isNotFound());

    verify(categoryService).createCategory(any(), any(), any(), any());
  }

  // ==================== UPDATE CATEGORY TESTS ====================

  @Test
  @DisplayName("Update category - Success")
  @WithMockUser(roles = "ADMIN")
  void updateCategory_Success() throws Exception {
    // Arrange
    Category updatedCategory = new Category();
    updatedCategory.setId(testCategory.getId());
    updatedCategory.setName("Updated Category");
    updatedCategory.setDescription("Updated description");
    updatedCategory.setSortOrder(5);
    updatedCategory.setActive(true);
    updatedCategory.setParentCategory(parentCategory);

    when(categoryService.updateCategory(
            testCategory.getId(),
            updateRequest.getName(),
            updateRequest.getDescription(),
            updateRequest.getSortOrder()))
        .thenReturn(updatedCategory);

    when(categoryService.setCategoryActive(testCategory.getId(), updateRequest.getActive()))
        .thenReturn(updatedCategory);

    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(3L);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/categories/{id}", testCategory.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testCategory.getId().toString()))
        .andExpect(jsonPath("$.name").value("Updated Category"))
        .andExpect(jsonPath("$.description").value("Updated description"))
        .andExpect(jsonPath("$.sortOrder").value(5))
        .andExpect(jsonPath("$.active").value(true));

    verify(categoryService)
        .updateCategory(
            testCategory.getId(),
            updateRequest.getName(),
            updateRequest.getDescription(),
            updateRequest.getSortOrder());
    verify(categoryService).setCategoryActive(testCategory.getId(), updateRequest.getActive());
  }

  @Test
  @DisplayName("Update category - Not found")
  @WithMockUser(roles = "ADMIN")
  void updateCategory_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(categoryService.updateCategory(any(), any(), any(), any()))
        .thenThrow(new ResourceNotFoundException("Category", nonExistentId.toString()));

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/categories/{id}", nonExistentId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());

    verify(categoryService).updateCategory(any(), any(), any(), any());
  }

  // ==================== MOVE CATEGORY TESTS ====================

  @Test
  @DisplayName("Move category - Success")
  @WithMockUser(roles = "ADMIN")
  void moveCategory_Success() throws Exception {
    // Arrange
    UUID newParentId = UUID.randomUUID();
    Category movedCategory = new Category();
    movedCategory.setId(testCategory.getId());
    movedCategory.setName(testCategory.getName());

    when(categoryService.moveCategory(testCategory.getId(), newParentId)).thenReturn(movedCategory);
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(2L);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/categories/{id}/move", testCategory.getId())
                .with(csrf())
                .param("parentId", newParentId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testCategory.getId().toString()));

    verify(categoryService).moveCategory(testCategory.getId(), newParentId);
  }

  @Test
  @DisplayName("Move category to root - Success")
  @WithMockUser(roles = "ADMIN")
  void moveCategoryToRoot_Success() throws Exception {
    // Arrange
    Category movedCategory = new Category();
    movedCategory.setId(testCategory.getId());
    movedCategory.setName(testCategory.getName());

    when(categoryService.moveCategory(testCategory.getId(), null)).thenReturn(movedCategory);
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(2L);

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/categories/{id}/move", testCategory.getId()).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testCategory.getId().toString()));

    verify(categoryService).moveCategory(testCategory.getId(), null);
  }

  @Test
  @DisplayName("Move category - Circular dependency")
  @WithMockUser(roles = "ADMIN")
  void moveCategory_CircularDependency() throws Exception {
    // Arrange
    when(categoryService.moveCategory(any(), any()))
        .thenThrow(new IllegalArgumentException("Moving would create circular dependency"));

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/categories/{id}/move", testCategory.getId())
                .with(csrf())
                .param("parentId", childCategory.getId().toString()))
        .andExpect(status().isBadRequest());

    verify(categoryService).moveCategory(testCategory.getId(), childCategory.getId());
  }

  // ==================== SET CATEGORY STATUS TESTS ====================

  @Test
  @DisplayName("Set category status - Success")
  @WithMockUser(roles = "ADMIN")
  void setCategoryStatus_Success() throws Exception {
    // Arrange
    Category updatedCategory = new Category();
    updatedCategory.setId(testCategory.getId());
    updatedCategory.setName(testCategory.getName());
    updatedCategory.setActive(false);

    when(categoryService.setCategoryActive(testCategory.getId(), false))
        .thenReturn(updatedCategory);
    when(categoryService.countProductsInCategory(any(UUID.class), anyBoolean())).thenReturn(2L);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/categories/{id}/status", testCategory.getId())
                .with(csrf())
                .param("active", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testCategory.getId().toString()))
        .andExpect(jsonPath("$.active").value(false));

    verify(categoryService).setCategoryActive(testCategory.getId(), false);
  }

  // ==================== DELETE CATEGORY TESTS ====================

  @Test
  @DisplayName("Delete category - Success")
  @WithMockUser(roles = "ADMIN")
  void deleteCategory_Success() throws Exception {
    // Arrange
    when(categoryService.findById(testCategory.getId()))
        .thenReturn(java.util.Optional.of(testCategory));
    doNothing().when(categoryService).deleteCategory(testCategory.getId());

    // Act & Assert
    mockMvc
        .perform(delete("/api/admin/categories/{id}", testCategory.getId()).with(csrf()))
        .andExpect(status().isNoContent());

    verify(categoryService).findById(testCategory.getId());
    verify(categoryService).deleteCategory(testCategory.getId());
  }

  @Test
  @DisplayName("Delete category - Not found")
  @WithMockUser(roles = "ADMIN")
  void deleteCategory_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(categoryService.findById(nonExistentId)).thenReturn(java.util.Optional.empty());

    // Act & Assert
    mockMvc
        .perform(delete("/api/admin/categories/{id}", nonExistentId).with(csrf()))
        .andExpect(status().isNotFound());

    verify(categoryService).findById(nonExistentId);
    verify(categoryService, never()).deleteCategory(any());
  }

  @Test
  @DisplayName("Delete category - Has products or subcategories")
  @WithMockUser(roles = "ADMIN")
  void deleteCategory_HasProductsOrSubcategories() throws Exception {
    // Arrange
    when(categoryService.findById(testCategory.getId()))
        .thenReturn(java.util.Optional.of(testCategory));
    doThrow(new IllegalStateException("Category has products or subcategories"))
        .when(categoryService)
        .deleteCategory(testCategory.getId());

    // Act & Assert
    mockMvc
        .perform(delete("/api/admin/categories/{id}", testCategory.getId()).with(csrf()))
        .andExpect(status().isBadRequest());

    verify(categoryService).deleteCategory(testCategory.getId());
  }

  // ==================== FORCE DELETE CATEGORY TESTS ====================

  @Test
  @DisplayName("Force delete category - Success")
  @WithMockUser(roles = "ADMIN")
  void forceDeleteCategory_Success() throws Exception {
    // Arrange
    UUID targetCategoryId = UUID.randomUUID();
    when(categoryService.findById(testCategory.getId()))
        .thenReturn(java.util.Optional.of(testCategory));
    doNothing()
        .when(categoryService)
        .deleteCategoryAndMoveProducts(testCategory.getId(), targetCategoryId);

    // Act & Assert
    mockMvc
        .perform(
            delete("/api/admin/categories/{id}/force", testCategory.getId())
                .with(csrf())
                .param("targetCategoryId", targetCategoryId.toString()))
        .andExpect(status().isNoContent());

    verify(categoryService).findById(testCategory.getId());
    verify(categoryService).deleteCategoryAndMoveProducts(testCategory.getId(), targetCategoryId);
  }

  // ==================== REORDER CATEGORIES TESTS ====================

  @Test
  @DisplayName("Reorder categories - Success")
  @WithMockUser(roles = "ADMIN")
  void reorderCategories_Success() throws Exception {
    // Arrange
    List<UUID> categoryIds =
        Arrays.asList(testCategory.getId(), childCategory.getId(), parentCategory.getId());
    doNothing().when(categoryService).reorderCategories(parentCategory.getId(), categoryIds);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/categories/reorder")
                .with(csrf())
                .param("parentId", parentCategory.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoryIds)))
        .andExpect(status().isOk());

    verify(categoryService).reorderCategories(parentCategory.getId(), categoryIds);
  }

  @Test
  @DisplayName("Reorder root categories - Success")
  @WithMockUser(roles = "ADMIN")
  void reorderRootCategories_Success() throws Exception {
    // Arrange
    List<UUID> categoryIds = Arrays.asList(parentCategory.getId(), testCategory.getId());
    doNothing().when(categoryService).reorderCategories(null, categoryIds);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/categories/reorder")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoryIds)))
        .andExpect(status().isOk());

    verify(categoryService).reorderCategories(null, categoryIds);
  }

  @Test
  @DisplayName("Reorder categories - Invalid request")
  @WithMockUser(roles = "ADMIN")
  void reorderCategories_InvalidRequest() throws Exception {
    // Arrange
    doThrow(new IllegalArgumentException("Invalid reorder request"))
        .when(categoryService)
        .reorderCategories(any(), any());

    List<UUID> categoryIds = Arrays.asList(testCategory.getId());

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/categories/reorder")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoryIds)))
        .andExpect(status().isBadRequest());

    verify(categoryService).reorderCategories(null, categoryIds);
  }
}
