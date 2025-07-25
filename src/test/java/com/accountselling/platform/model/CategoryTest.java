package com.accountselling.platform.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Category entity. Tests category creation, hierarchical relationships, and business
 * logic.
 */
@DisplayName("Category Entity Tests")
class CategoryTest {

  private Category rootCategory;
  private Category subCategory;
  private Category subSubCategory;
  private Product product;

  @BeforeEach
  void setUp() {
    rootCategory = new Category("Games");
    subCategory = new Category("RPG", "Role Playing Games");
    subSubCategory = new Category("MMORPG", "Massively Multiplayer Online RPG");
    product = new Product();
  }

  @Test
  @DisplayName("Should create category with default constructor")
  void shouldCreateCategoryWithDefaultConstructor() {
    Category category = new Category();

    assertNotNull(category);
    assertNull(category.getName());
    assertNull(category.getDescription());
    assertTrue(category.getActive());
    assertEquals(0, category.getSortOrder());
    assertNotNull(category.getSubCategories());
    assertNotNull(category.getProducts());
    assertTrue(category.getSubCategories().isEmpty());
    assertTrue(category.getProducts().isEmpty());
  }

  @Test
  @DisplayName("Should create category with name constructor")
  void shouldCreateCategoryWithNameConstructor() {
    Category category = new Category("Test Category");

    assertEquals("Test Category", category.getName());
    assertNull(category.getDescription());
    assertTrue(category.getActive());
    assertEquals(0, category.getSortOrder());
  }

  @Test
  @DisplayName("Should create category with name and description constructor")
  void shouldCreateCategoryWithNameAndDescriptionConstructor() {
    Category category = new Category("Test Category", "Test Description");

    assertEquals("Test Category", category.getName());
    assertEquals("Test Description", category.getDescription());
    assertTrue(category.getActive());
  }

  @Test
  @DisplayName("Should create category with parent constructor")
  void shouldCreateCategoryWithParentConstructor() {
    Category category = new Category("Child", "Child Category", rootCategory);

    assertEquals("Child", category.getName());
    assertEquals("Child Category", category.getDescription());
    assertEquals(rootCategory, category.getParentCategory());
  }

  @Test
  @DisplayName("Should set and get category properties")
  void shouldSetAndGetCategoryProperties() {
    UUID id = UUID.randomUUID();
    rootCategory.setId(id);
    rootCategory.setName("Updated Games");
    rootCategory.setDescription("Updated Description");
    rootCategory.setActive(false);
    rootCategory.setSortOrder(10);

    assertEquals(id, rootCategory.getId());
    assertEquals("Updated Games", rootCategory.getName());
    assertEquals("Updated Description", rootCategory.getDescription());
    assertFalse(rootCategory.getActive());
    assertEquals(10, rootCategory.getSortOrder());
  }

  @Test
  @DisplayName("Should add and remove subcategories")
  void shouldAddAndRemoveSubcategories() {
    rootCategory.addSubCategory(subCategory);

    assertTrue(rootCategory.getSubCategories().contains(subCategory));
    assertEquals(rootCategory, subCategory.getParentCategory());
    assertEquals(1, rootCategory.getSubCategories().size());

    rootCategory.removeSubCategory(subCategory);

    assertFalse(rootCategory.getSubCategories().contains(subCategory));
    assertNull(subCategory.getParentCategory());
    assertEquals(0, rootCategory.getSubCategories().size());
  }

  @Test
  @DisplayName("Should add and remove products")
  void shouldAddAndRemoveProducts() {
    rootCategory.addProduct(product);

    assertTrue(rootCategory.getProducts().contains(product));
    assertEquals(rootCategory, product.getCategory());
    assertEquals(1, rootCategory.getProducts().size());

    rootCategory.removeProduct(product);

    assertFalse(rootCategory.getProducts().contains(product));
    assertNull(product.getCategory());
    assertEquals(0, rootCategory.getProducts().size());
  }

  @Test
  @DisplayName("Should identify root category correctly")
  void shouldIdentifyRootCategoryCorrectly() {
    assertTrue(rootCategory.isRootCategory());

    subCategory.setParentCategory(rootCategory);
    assertFalse(subCategory.isRootCategory());
  }

  @Test
  @DisplayName("Should check if category has subcategories")
  void shouldCheckIfCategoryHasSubcategories() {
    assertFalse(rootCategory.hasSubCategories());

    rootCategory.addSubCategory(subCategory);
    assertTrue(rootCategory.hasSubCategories());
  }

  @Test
  @DisplayName("Should check if category has products")
  void shouldCheckIfCategoryHasProducts() {
    assertFalse(rootCategory.hasProducts());

    rootCategory.addProduct(product);
    assertTrue(rootCategory.hasProducts());
  }

  @Test
  @DisplayName("Should calculate category level correctly")
  void shouldCalculateCategoryLevelCorrectly() {
    assertEquals(0, rootCategory.getLevel());

    rootCategory.addSubCategory(subCategory);
    assertEquals(1, subCategory.getLevel());

    subCategory.addSubCategory(subSubCategory);
    assertEquals(2, subSubCategory.getLevel());
  }

  @Test
  @DisplayName("Should generate full path correctly")
  void shouldGenerateFullPathCorrectly() {
    assertEquals("Games", rootCategory.getFullPath());

    rootCategory.addSubCategory(subCategory);
    assertEquals("Games > RPG", subCategory.getFullPath());

    subCategory.addSubCategory(subSubCategory);
    assertEquals("Games > RPG > MMORPG", subSubCategory.getFullPath());
  }

  @Test
  @DisplayName("Should get all descendants correctly")
  void shouldGetAllDescendantsCorrectly() {
    rootCategory.addSubCategory(subCategory);
    subCategory.addSubCategory(subSubCategory);

    Set<Category> descendants = rootCategory.getAllDescendants();

    assertEquals(2, descendants.size());
    assertTrue(descendants.contains(subCategory));
    assertTrue(descendants.contains(subSubCategory));
  }

  @Test
  @DisplayName("Should get root category correctly")
  void shouldGetRootCategoryCorrectly() {
    rootCategory.addSubCategory(subCategory);
    subCategory.addSubCategory(subSubCategory);

    assertEquals(rootCategory, rootCategory.getRootCategory());
    assertEquals(rootCategory, subCategory.getRootCategory());
    assertEquals(rootCategory, subSubCategory.getRootCategory());
  }

  @Test
  @DisplayName("Should check ancestor relationship correctly")
  void shouldCheckAncestorRelationshipCorrectly() {
    rootCategory.addSubCategory(subCategory);
    subCategory.addSubCategory(subSubCategory);

    assertTrue(rootCategory.isAncestorOf(subCategory));
    assertTrue(rootCategory.isAncestorOf(subSubCategory));
    assertTrue(subCategory.isAncestorOf(subSubCategory));

    assertFalse(subCategory.isAncestorOf(rootCategory));
    assertFalse(subSubCategory.isAncestorOf(subCategory));
    assertFalse(subSubCategory.isAncestorOf(rootCategory));
  }

  @Test
  @DisplayName("Should check descendant relationship correctly")
  void shouldCheckDescendantRelationshipCorrectly() {
    rootCategory.addSubCategory(subCategory);
    subCategory.addSubCategory(subSubCategory);

    assertTrue(subCategory.isDescendantOf(rootCategory));
    assertTrue(subSubCategory.isDescendantOf(rootCategory));
    assertTrue(subSubCategory.isDescendantOf(subCategory));

    assertFalse(rootCategory.isDescendantOf(subCategory));
    assertFalse(subCategory.isDescendantOf(subSubCategory));
    assertFalse(rootCategory.isDescendantOf(subSubCategory));
  }

  @Test
  @DisplayName("Should handle multiple subcategories")
  void shouldHandleMultipleSubcategories() {
    Category rpg = new Category("RPG");
    Category strategy = new Category("Strategy");
    Category action = new Category("Action");

    rootCategory.addSubCategory(rpg);
    rootCategory.addSubCategory(strategy);
    rootCategory.addSubCategory(action);

    assertEquals(3, rootCategory.getSubCategories().size());
    assertTrue(rootCategory.getSubCategories().contains(rpg));
    assertTrue(rootCategory.getSubCategories().contains(strategy));
    assertTrue(rootCategory.getSubCategories().contains(action));
  }

  @Test
  @DisplayName("Should implement equals correctly")
  void shouldImplementEqualsCorrectly() {
    Category category1 = new Category("Test");
    Category category2 = new Category("Test");
    Category category3 = new Category("Different");

    assertEquals(category1, category2);
    assertNotEquals(category1, category3);
    assertNotEquals(category1, null);
    assertNotEquals(category1, "not a category");
  }

  @Test
  @DisplayName("Should implement hashCode correctly")
  void shouldImplementHashCodeCorrectly() {
    Category category1 = new Category("Test");
    Category category2 = new Category("Test");

    assertEquals(category1.hashCode(), category2.hashCode());
  }

  @Test
  @DisplayName("Should implement toString correctly")
  void shouldImplementToStringCorrectly() {
    rootCategory.setName("Games");
    rootCategory.setDescription("Game Categories");

    String toString = rootCategory.toString();

    assertNotNull(toString);
    assertTrue(toString.contains("Category("));
    assertTrue(toString.contains("name=Games"));
    assertTrue(toString.contains("description=Game Categories"));
  }

  @Test
  @DisplayName("Should handle null values gracefully")
  void shouldHandleNullValuesGracefully() {
    Category category = new Category();

    assertNull(category.getName());
    assertNull(category.getDescription());
    assertNull(category.getParentCategory());

    // Should not throw exceptions
    assertFalse(category.hasSubCategories());
    assertFalse(category.hasProducts());
    assertTrue(category.isRootCategory());
    assertEquals(0, category.getLevel());
  }
}
