package com.accountselling.platform.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Product Catalog domain models. Tests the interaction between Category and
 * Product entities.
 */
@DisplayName("Product Catalog Integration Tests")
class ProductCatalogIntegrationTest {

  private Category rootCategory;
  private Category gameCategory;
  private Category rpgCategory;
  private Category mmorpgCategory;
  private Product product1;
  private Product product2;
  private Product product3;

  @BeforeEach
  void setUp() {
    // Create hierarchical category structure
    rootCategory = new Category("All Categories", "Root category for all products");
    gameCategory = new Category("Games", "Game-related products");
    rpgCategory = new Category("RPG", "Role Playing Games");
    mmorpgCategory = new Category("MMORPG", "Massively Multiplayer Online RPG");

    // Build hierarchy
    rootCategory.addSubCategory(gameCategory);
    gameCategory.addSubCategory(rpgCategory);
    rpgCategory.addSubCategory(mmorpgCategory);

    // Create products
    product1 =
        new Product(
            "World of Warcraft Account",
            "High level character with rare items",
            new BigDecimal("299.99"),
            mmorpgCategory);
    product2 =
        new Product(
            "Final Fantasy XIV Account",
            "Max level character with all expansions",
            new BigDecimal("199.99"),
            mmorpgCategory);
    product3 =
        new Product(
            "Elder Scrolls Online Account",
            "Champion level character",
            new BigDecimal("149.99"),
            mmorpgCategory);
  }

  @Test
  @DisplayName("Should create hierarchical category structure correctly")
  void shouldCreateHierarchicalCategoryStructureCorrectly() {
    // Verify hierarchy levels
    assertEquals(0, rootCategory.getLevel());
    assertEquals(1, gameCategory.getLevel());
    assertEquals(2, rpgCategory.getLevel());
    assertEquals(3, mmorpgCategory.getLevel());

    // Verify parent-child relationships
    assertTrue(rootCategory.isRootCategory());
    assertFalse(gameCategory.isRootCategory());
    assertEquals(rootCategory, gameCategory.getParentCategory());
    assertEquals(gameCategory, rpgCategory.getParentCategory());
    assertEquals(rpgCategory, mmorpgCategory.getParentCategory());

    // Verify full paths
    assertEquals("All Categories", rootCategory.getFullPath());
    assertEquals("All Categories > Games", gameCategory.getFullPath());
    assertEquals("All Categories > Games > RPG", rpgCategory.getFullPath());
    assertEquals("All Categories > Games > RPG > MMORPG", mmorpgCategory.getFullPath());
  }

  @Test
  @DisplayName("Should manage category-product relationships correctly")
  void shouldManageCategoryProductRelationshipsCorrectly() {
    // Add products to category
    mmorpgCategory.addProduct(product1);
    mmorpgCategory.addProduct(product2);
    mmorpgCategory.addProduct(product3);

    // Verify products are added to category
    assertEquals(3, mmorpgCategory.getProducts().size());
    assertTrue(mmorpgCategory.getProducts().contains(product1));
    assertTrue(mmorpgCategory.getProducts().contains(product2));
    assertTrue(mmorpgCategory.getProducts().contains(product3));

    // Verify products have correct category
    assertEquals(mmorpgCategory, product1.getCategory());
    assertEquals(mmorpgCategory, product2.getCategory());
    assertEquals(mmorpgCategory, product3.getCategory());

    // Verify category paths in products
    assertEquals("All Categories > Games > RPG > MMORPG", product1.getCategoryPath());
    assertEquals("All Categories > Games > RPG > MMORPG", product2.getCategoryPath());
    assertEquals("All Categories > Games > RPG > MMORPG", product3.getCategoryPath());
  }

  @Test
  @DisplayName("Should handle product category inheritance correctly")
  void shouldHandleProductCategoryInheritanceCorrectly() {
    mmorpgCategory.addProduct(product1);

    // Product should belong to its direct category and all ancestor categories
    assertTrue(product1.belongsToCategory(mmorpgCategory));
    assertTrue(product1.belongsToCategory(rpgCategory));
    assertTrue(product1.belongsToCategory(gameCategory));
    assertTrue(product1.belongsToCategory(rootCategory));

    // Product should not belong to unrelated categories
    Category strategyCategory = new Category("Strategy", "Strategy Games");
    gameCategory.addSubCategory(strategyCategory);
    assertFalse(product1.belongsToCategory(strategyCategory));
  }

  @Test
  @DisplayName("Should support category reorganization")
  void shouldSupportCategoryReorganization() {
    // Add products to category
    mmorpgCategory.addProduct(product1);
    mmorpgCategory.addProduct(product2);

    // Move MMORPG category to be under Games directly
    rpgCategory.removeSubCategory(mmorpgCategory);
    gameCategory.addSubCategory(mmorpgCategory);

    // Verify new hierarchy
    assertEquals(gameCategory, mmorpgCategory.getParentCategory());
    assertEquals(2, mmorpgCategory.getLevel());
    assertEquals("All Categories > Games > MMORPG", mmorpgCategory.getFullPath());

    // Verify products still belong to category and new ancestors
    assertTrue(product1.belongsToCategory(mmorpgCategory));
    assertTrue(product1.belongsToCategory(gameCategory));
    assertTrue(product1.belongsToCategory(rootCategory));
    assertFalse(product1.belongsToCategory(rpgCategory)); // No longer ancestor
  }

  @Test
  @DisplayName("Should handle category deactivation correctly")
  void shouldHandleCategoryDeactivationCorrectly() {
    mmorpgCategory.addProduct(product1);
    product1.setActive(true);

    // Deactivate category
    mmorpgCategory.setActive(false);

    // Category should be deactivated but products remain active
    assertFalse(mmorpgCategory.getActive());
    assertTrue(product1.getActive());

    // Product should still belong to the deactivated category
    assertEquals(mmorpgCategory, product1.getCategory());
    assertTrue(product1.belongsToCategory(mmorpgCategory));
  }

  @Test
  @DisplayName("Should support multiple products in same category")
  void shouldSupportMultipleProductsInSameCategory() {
    // Add all products to the same category
    mmorpgCategory.addProduct(product1);
    mmorpgCategory.addProduct(product2);
    mmorpgCategory.addProduct(product3);

    // Verify all products are in the category
    assertEquals(3, mmorpgCategory.getProducts().size());
    assertTrue(mmorpgCategory.hasProducts());

    // Remove one product
    mmorpgCategory.removeProduct(product2);
    assertEquals(2, mmorpgCategory.getProducts().size());
    assertFalse(mmorpgCategory.getProducts().contains(product2));
    assertNull(product2.getCategory());

    // Category should still have products
    assertTrue(mmorpgCategory.hasProducts());
  }

  @Test
  @DisplayName("Should handle empty categories correctly")
  void shouldHandleEmptyCategoriesCorrectly() {
    // New category should be empty
    assertFalse(mmorpgCategory.hasProducts());
    assertFalse(mmorpgCategory.hasSubCategories());
    assertEquals(0, mmorpgCategory.getProducts().size());
    assertEquals(0, mmorpgCategory.getSubCategories().size());

    // Add and remove product
    mmorpgCategory.addProduct(product1);
    assertTrue(mmorpgCategory.hasProducts());

    mmorpgCategory.removeProduct(product1);
    assertFalse(mmorpgCategory.hasProducts());
  }

  @Test
  @DisplayName("Should support category search and filtering scenarios")
  void shouldSupportCategorySearchAndFilteringScenarios() {
    // Set up products with different attributes
    product1.setServer("US-East");
    product1.setPrice(new BigDecimal("299.99"));

    product2.setServer("EU-West");
    product2.setPrice(new BigDecimal("199.99"));

    product3.setServer("US-East");
    product3.setPrice(new BigDecimal("149.99"));

    mmorpgCategory.addProduct(product1);
    mmorpgCategory.addProduct(product2);
    mmorpgCategory.addProduct(product3);

    // Verify products can be filtered by category
    assertEquals(3, mmorpgCategory.getProducts().size());

    // Verify products have different attributes for filtering
    long usEastProducts =
        mmorpgCategory.getProducts().stream().filter(p -> "US-East".equals(p.getServer())).count();
    assertEquals(2, usEastProducts);

    long expensiveProducts =
        mmorpgCategory.getProducts().stream()
            .filter(p -> p.getPrice().compareTo(new BigDecimal("200.00")) >= 0)
            .count();
    assertEquals(1, expensiveProducts); // Only product1 (299.99) is >= 200.00
  }

  @Test
  @DisplayName("Should maintain data consistency during concurrent operations")
  void shouldMaintainDataConsistencyDuringConcurrentOperations() {
    // This test simulates concurrent operations that might happen in real scenarios

    // Add products to category
    mmorpgCategory.addProduct(product1);
    mmorpgCategory.addProduct(product2);

    // Simulate moving product to different category
    Category newCategory = new Category("Action RPG", "Action Role Playing Games");
    rpgCategory.addSubCategory(newCategory);

    // Move product1 to new category
    mmorpgCategory.removeProduct(product1);
    newCategory.addProduct(product1);

    // Verify consistency
    assertEquals(1, mmorpgCategory.getProducts().size());
    assertEquals(1, newCategory.getProducts().size());
    assertEquals(newCategory, product1.getCategory());
    assertFalse(mmorpgCategory.getProducts().contains(product1));
    assertTrue(newCategory.getProducts().contains(product1));
  }
}
