package com.accountselling.platform.service.impl;

import com.accountselling.platform.exception.ResourceAlreadyExistsException;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryServiceImpl.
 * Tests category management functionality including hierarchy operations, search, and validation.
 * 
 * CategoryServiceImpl testing
 * Tests category management functions including hierarchy management, search, and validation
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category rootCategory;
    private Category subCategory;
    private Category parentCategory;

    @BeforeEach
    void setUp() {
        rootCategory = new Category("Gaming");
        rootCategory.setId(UUID.randomUUID());
        rootCategory.setActive(true);
        rootCategory.setSortOrder(0);

        parentCategory = new Category("MMO Games");
        parentCategory.setId(UUID.randomUUID());
        parentCategory.setActive(true);
        parentCategory.setSortOrder(1);

        subCategory = new Category("MMORPG", "Massively Multiplayer Online Role-Playing Games", parentCategory);
        subCategory.setId(UUID.randomUUID());
        subCategory.setActive(true);
        subCategory.setSortOrder(0);
    }

    // ========== Read Operations Tests ==========

    @Test
    void findById_WithExistingCategory_ShouldReturnCategory() {
        UUID categoryId = rootCategory.getId();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(rootCategory));

        Optional<Category> result = categoryService.findById(categoryId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(rootCategory);
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void findById_WithNonExistentCategory_ShouldReturnEmpty() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        Optional<Category> result = categoryService.findById(categoryId);

        assertThat(result).isEmpty();
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void findByName_WithExistingCategory_ShouldReturnCategory() {
        String categoryName = "Gaming";
        when(categoryRepository.findByName(categoryName)).thenReturn(Optional.of(rootCategory));

        Optional<Category> result = categoryService.findByName(categoryName);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(categoryName);
        verify(categoryRepository).findByName(categoryName);
    }

    @Test
    void findAllCategories_ShouldReturnOrderedCategories() {
        List<Category> categories = Arrays.asList(rootCategory, parentCategory, subCategory);
        when(categoryRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(categories);

        List<Category> result = categoryService.findAllCategories();

        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(categories);
        verify(categoryRepository).findAllByOrderBySortOrderAscNameAsc();
    }

    @Test
    void findActiveCategories_ShouldReturnOnlyActiveCategories() {
        List<Category> activeCategories = Arrays.asList(rootCategory, parentCategory);
        when(categoryRepository.findActiveOrderedBySort()).thenReturn(activeCategories);

        List<Category> result = categoryService.findActiveCategories();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(activeCategories);
        verify(categoryRepository).findActiveOrderedBySort();
    }

    @Test
    void findRootCategories_ShouldReturnRootCategories() {
        List<Category> rootCategories = Arrays.asList(rootCategory);
        when(categoryRepository.findRootCategories()).thenReturn(rootCategories);

        List<Category> result = categoryService.findRootCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(rootCategory);
        verify(categoryRepository).findRootCategories();
    }

    @Test
    void findActiveRootCategories_ShouldReturnActiveRootCategories() {
        List<Category> activeRootCategories = Arrays.asList(rootCategory);
        when(categoryRepository.findActiveRootCategories()).thenReturn(activeRootCategories);

        List<Category> result = categoryService.findActiveRootCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(rootCategory);
        verify(categoryRepository).findActiveRootCategories();
    }

    @Test
    void findSubcategories_WithParentId_ShouldReturnSubcategories() {
        UUID parentId = parentCategory.getId();
        List<Category> subcategories = Arrays.asList(subCategory);
        when(categoryRepository.findByParentCategoryId(parentId)).thenReturn(subcategories);

        List<Category> result = categoryService.findSubcategories(parentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(subCategory);
        verify(categoryRepository).findByParentCategoryId(parentId);
    }

    @Test
    void searchCategoriesByName_WithValidName_ShouldReturnMatchingCategories() {
        String searchName = "Gaming";
        List<Category> matchingCategories = Arrays.asList(rootCategory);
        when(categoryRepository.findByNameContainingIgnoreCaseAndActive(searchName, true))
            .thenReturn(matchingCategories);

        List<Category> result = categoryService.searchCategoriesByName(searchName, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).containsIgnoringCase(searchName);
        verify(categoryRepository).findByNameContainingIgnoreCaseAndActive(searchName, true);
    }

    @Test
    void searchCategoriesByName_WithEmptyName_ShouldReturnEmptyList() {
        List<Category> result = categoryService.searchCategoriesByName("", true);

        assertThat(result).isEmpty();
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void existsByName_WithExistingName_ShouldReturnTrue() {
        String categoryName = "Gaming";
        when(categoryRepository.existsByName(categoryName)).thenReturn(true);

        boolean result = categoryService.existsByName(categoryName);

        assertThat(result).isTrue();
        verify(categoryRepository).existsByName(categoryName);
    }

    @Test
    void countProductsInCategory_WithActiveOnly_ShouldReturnActiveCount() {
        UUID categoryId = rootCategory.getId();
        when(categoryRepository.countActiveProductsByCategoryId(categoryId)).thenReturn(5L);

        long result = categoryService.countProductsInCategory(categoryId, true);

        assertThat(result).isEqualTo(5L);
        verify(categoryRepository).countActiveProductsByCategoryId(categoryId);
    }

    @Test
    void countProductsInCategory_WithAllProducts_ShouldReturnTotalCount() {
        UUID categoryId = rootCategory.getId();
        when(categoryRepository.countProductsByCategoryId(categoryId)).thenReturn(8L);

        long result = categoryService.countProductsInCategory(categoryId, false);

        assertThat(result).isEqualTo(8L);
        verify(categoryRepository).countProductsByCategoryId(categoryId);
    }

    // ========== Write Operations Tests ==========

    @Test
    void createCategory_WithValidData_ShouldCreateCategory() {
        String name = "New Category";
        String description = "Test category";
        UUID parentId = rootCategory.getId();

        when(categoryRepository.existsByName(name)).thenReturn(false);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(UUID.randomUUID());
            return category;
        });

        Category result = categoryService.createCategory(name, description, parentId, 1);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getParentCategory()).isEqualTo(rootCategory);
        assertThat(result.getSortOrder()).isEqualTo(1);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_WithExistingName_ShouldThrowException() {
        String name = "Existing Category";
        when(categoryRepository.existsByName(name)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(name, "description", null))
            .isInstanceOf(ResourceAlreadyExistsException.class)
            .hasMessageContaining("Category name already exists: " + name);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_WithNonExistentParent_ShouldThrowException() {
        String name = "New Category";
        UUID parentId = UUID.randomUUID();
        
        when(categoryRepository.existsByName(name)).thenReturn(false);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(name, "description", parentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Parent category not found with ID: " + parentId);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_WithValidData_ShouldUpdateCategory() {
        UUID categoryId = rootCategory.getId();
        String newName = "Updated Gaming";
        String newDescription = "Updated description";
        Integer newSortOrder = 5;

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.existsByName(newName)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(rootCategory);

        Category result = categoryService.updateCategory(categoryId, newName, newDescription, newSortOrder);

        assertThat(result).isNotNull();
        verify(categoryRepository).save(rootCategory);
        assertThat(rootCategory.getName()).isEqualTo(newName);
        assertThat(rootCategory.getDescription()).isEqualTo(newDescription);
        assertThat(rootCategory.getSortOrder()).isEqualTo(newSortOrder);
    }

    @Test
    void updateCategory_WithNonExistentCategory_ShouldThrowException() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, "New Name", "description", 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Category not found with ID: " + categoryId);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void moveCategory_WithValidParent_ShouldMoveCategory() {
        UUID categoryId = subCategory.getId();
        UUID newParentId = rootCategory.getId();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(subCategory));
        when(categoryRepository.findById(newParentId)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(subCategory);

        Category result = categoryService.moveCategory(categoryId, newParentId);

        assertThat(result).isNotNull();
        assertThat(result.getParentCategory()).isEqualTo(rootCategory);
        verify(categoryRepository).save(subCategory);
    }

    @Test
    void setCategoryActive_WithValidCategory_ShouldUpdateActiveStatus() {
        UUID categoryId = rootCategory.getId();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(rootCategory);

        Category result = categoryService.setCategoryActive(categoryId, false);

        assertThat(result).isNotNull();
        assertThat(rootCategory.getActive()).isFalse();
        verify(categoryRepository).save(rootCategory);
    }

    @Test
    void deleteCategory_WithDeletableCategory_ShouldDeleteCategory() {
        UUID categoryId = rootCategory.getId();
        rootCategory.getProducts().clear();
        rootCategory.getSubCategories().clear();
        
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(rootCategory));

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository).delete(rootCategory);
    }

    @Test
    void deleteCategory_WithProductsOrSubcategories_ShouldThrowException() {
        UUID categoryId = rootCategory.getId();
        rootCategory.getSubCategories().add(subCategory);
        
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(rootCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot delete category that has products or subcategories");

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    // ========== Validation & Utility Tests ==========

    @Test
    void isValidHierarchy_WithValidParent_ShouldReturnTrue() {
        UUID categoryId = subCategory.getId();
        UUID proposedParentId = rootCategory.getId();

        when(categoryRepository.findById(proposedParentId)).thenReturn(Optional.of(rootCategory));

        boolean result = categoryService.isValidHierarchy(categoryId, proposedParentId);

        assertThat(result).isTrue();
    }

    @Test
    void isValidHierarchy_WithSelfAsParent_ShouldReturnFalse() {
        UUID categoryId = rootCategory.getId();

        boolean result = categoryService.isValidHierarchy(categoryId, categoryId);

        assertThat(result).isFalse();
    }

    @Test
    void isValidHierarchy_WithNullParent_ShouldReturnTrue() {
        UUID categoryId = subCategory.getId();

        boolean result = categoryService.isValidHierarchy(categoryId, null);

        assertThat(result).isTrue();
    }

    @Test
    void canDeleteCategory_WithEmptyCategory_ShouldReturnTrue() {
        UUID categoryId = rootCategory.getId();
        rootCategory.getProducts().clear();
        rootCategory.getSubCategories().clear();
        
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(rootCategory));

        boolean result = categoryService.canDeleteCategory(categoryId);

        assertThat(result).isTrue();
    }

    @Test
    void canDeleteCategory_WithSubcategories_ShouldReturnFalse() {
        UUID categoryId = parentCategory.getId();
        parentCategory.getSubCategories().add(subCategory);
        
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(parentCategory));

        boolean result = categoryService.canDeleteCategory(categoryId);

        assertThat(result).isFalse();
    }

    @Test
    void getCategoryPath_WithNestedCategory_ShouldReturnPath() {
        UUID categoryId = subCategory.getId();
        subCategory.setParentCategory(parentCategory);
        parentCategory.setParentCategory(rootCategory);
        
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(subCategory));

        List<Category> result = categoryService.getCategoryPath(categoryId);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(rootCategory);
        assertThat(result.get(1)).isEqualTo(parentCategory);
        assertThat(result.get(2)).isEqualTo(subCategory);
    }

    @Test
    void reorderCategories_WithValidCategories_ShouldUpdateSortOrder() {
        UUID parentId = rootCategory.getId();
        List<UUID> categoryIds = Arrays.asList(subCategory.getId(), parentCategory.getId());
        
        subCategory.setParentCategory(rootCategory);
        parentCategory.setParentCategory(rootCategory);
        
        when(categoryRepository.findById(subCategory.getId())).thenReturn(Optional.of(subCategory));
        when(categoryRepository.findById(parentCategory.getId())).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.saveAll(anyList())).thenReturn(Arrays.asList(subCategory, parentCategory));

        categoryService.reorderCategories(parentId, categoryIds);

        assertThat(subCategory.getSortOrder()).isEqualTo(0);
        assertThat(parentCategory.getSortOrder()).isEqualTo(1);
        verify(categoryRepository).saveAll(anyList());
    }
}