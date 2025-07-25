package com.accountselling.platform.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for CategoryRepository. Tests repository methods for category management functionality
 * including hierarchical operations and search capabilities.
 */
@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private CategoryRepository categoryRepository;

  private Category rootCategory;
  private Category subCategory1;
  private Category subCategory2;
  private Category inactiveCategory;

  @BeforeEach
  void setUp() {
    // Create test categories with hierarchy
    rootCategory = new Category("Games", "Gaming accounts and items");
    rootCategory.setSortOrder(1);
    rootCategory.setActive(true);

    subCategory1 = new Category("MMORPG", "Massively Multiplayer Online Role-Playing Games");
    subCategory1.setSortOrder(1);
    subCategory1.setActive(true);
    subCategory1.setParentCategory(rootCategory);

    subCategory2 = new Category("FPS", "First Person Shooter games");
    subCategory2.setSortOrder(2);
    subCategory2.setActive(true);
    subCategory2.setParentCategory(rootCategory);

    inactiveCategory = new Category("Inactive", "Inactive category");
    inactiveCategory.setActive(false);
  }

  @Test
  void findByName_WhenCategoryExists_ShouldReturnCategory() {
    // Given
    entityManager.persistAndFlush(rootCategory);

    // When
    Optional<Category> result = categoryRepository.findByName("Games");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Games");
    assertThat(result.get().getDescription()).isEqualTo("Gaming accounts and items");
  }

  @Test
  void findByName_WhenCategoryDoesNotExist_ShouldReturnEmpty() {
    // When
    Optional<Category> result = categoryRepository.findByName("NonExistent");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void existsByName_WhenCategoryExists_ShouldReturnTrue() {
    // Given
    entityManager.persistAndFlush(rootCategory);

    // When
    boolean exists = categoryRepository.existsByName("Games");

    // Then
    assertThat(exists).isTrue();
  }

  @Test
  void existsByName_WhenCategoryDoesNotExist_ShouldReturnFalse() {
    // When
    boolean exists = categoryRepository.existsByName("NonExistent");

    // Then
    assertThat(exists).isFalse();
  }

  @Test
  void findByActive_ShouldReturnCategoriesWithSpecifiedStatus() {
    // Given
    entityManager.persistAndFlush(rootCategory);
    entityManager.persistAndFlush(inactiveCategory);

    // When
    List<Category> activeCategories = categoryRepository.findByActive(true);
    List<Category> inactiveCategories = categoryRepository.findByActive(false);

    // Then
    assertThat(activeCategories).hasSize(1);
    assertThat(activeCategories.get(0).getName()).isEqualTo("Games");

    assertThat(inactiveCategories).hasSize(1);
    assertThat(inactiveCategories.get(0).getName()).isEqualTo("Inactive");
  }

  @Test
  void findActiveOrderedBySort_ShouldReturnActiveCategoriesOrderedBySortAndName() {
    // Given
    Category category1 = new Category("A Category");
    category1.setSortOrder(2);
    category1.setActive(true);

    Category category2 = new Category("B Category");
    category2.setSortOrder(1);
    category2.setActive(true);

    Category category3 = new Category("C Category");
    category3.setSortOrder(1);
    category3.setActive(true);

    entityManager.persistAndFlush(category1);
    entityManager.persistAndFlush(category2);
    entityManager.persistAndFlush(category3);
    entityManager.persistAndFlush(inactiveCategory);

    // When
    List<Category> result = categoryRepository.findActiveOrderedBySort();

    // Then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getName()).isEqualTo("B Category"); // sortOrder 1, name B
    assertThat(result.get(1).getName()).isEqualTo("C Category"); // sortOrder 1, name C
    assertThat(result.get(2).getName()).isEqualTo("A Category"); // sortOrder 2, name A
  }

  @Test
  void findRootCategories_ShouldReturnCategoriesWithoutParent() {
    // Given
    entityManager.persistAndFlush(rootCategory);
    entityManager.persistAndFlush(subCategory1);
    entityManager.persistAndFlush(subCategory2);

    Category anotherRoot = new Category("Another Root");
    anotherRoot.setSortOrder(0);
    entityManager.persistAndFlush(anotherRoot);

    // When
    List<Category> rootCategories = categoryRepository.findRootCategories();

    // Then
    assertThat(rootCategories).hasSize(2);
    assertThat(rootCategories.get(0).getName()).isEqualTo("Another Root"); // sortOrder 0
    assertThat(rootCategories.get(1).getName()).isEqualTo("Games"); // sortOrder 1
    assertThat(rootCategories).allMatch(category -> category.getParentCategory() == null);
  }

  @Test
  void findActiveRootCategories_ShouldReturnActiveRootCategories() {
    // Given
    entityManager.persistAndFlush(rootCategory);
    entityManager.persistAndFlush(inactiveCategory);

    Category activeRoot = new Category("Active Root");
    activeRoot.setActive(true);
    activeRoot.setSortOrder(0);
    entityManager.persistAndFlush(activeRoot);

    // When
    List<Category> activeRootCategories = categoryRepository.findActiveRootCategories();

    // Then
    assertThat(activeRootCategories).hasSize(2);
    assertThat(activeRootCategories.get(0).getName()).isEqualTo("Active Root");
    assertThat(activeRootCategories.get(1).getName()).isEqualTo("Games");
    assertThat(activeRootCategories).allMatch(Category::getActive);
    assertThat(activeRootCategories).allMatch(category -> category.getParentCategory() == null);
  }

  @Test
  void findByParentCategoryId_ShouldReturnSubcategoriesOrderedBySortAndName() {
    // Given
    entityManager.persistAndFlush(rootCategory);
    entityManager.persistAndFlush(subCategory1);
    entityManager.persistAndFlush(subCategory2);

    // When
    List<Category> subcategories = categoryRepository.findByParentCategoryId(rootCategory.getId());

    // Then
    assertThat(subcategories).hasSize(2);
    assertThat(subcategories.get(0).getName()).isEqualTo("MMORPG"); // sortOrder 1
    assertThat(subcategories.get(1).getName()).isEqualTo("FPS"); // sortOrder 2
    assertThat(subcategories)
        .allMatch(category -> category.getParentCategory().getId().equals(rootCategory.getId()));
  }

  @Test
  void findActiveByParentCategoryId_ShouldReturnActiveSubcategories() {
    // Given
    entityManager.persistAndFlush(rootCategory);
    entityManager.persistAndFlush(subCategory1);

    Category inactiveSubCategory = new Category("Inactive Sub");
    inactiveSubCategory.setParentCategory(rootCategory);
    inactiveSubCategory.setActive(false);
    entityManager.persistAndFlush(inactiveSubCategory);

    // When
    List<Category> activeSubcategories =
        categoryRepository.findActiveByParentCategoryId(rootCategory.getId());

    // Then
    assertThat(activeSubcategories).hasSize(1);
    assertThat(activeSubcategories.get(0).getName()).isEqualTo("MMORPG");
    assertThat(activeSubcategories).allMatch(Category::getActive);
  }

  @Test
  void findByNameContainingIgnoreCase_ShouldReturnMatchingCategories() {
    // Given
    Category gameCategory = new Category("Game Accounts");
    Category socialCategory = new Category("Social Media");
    Category streamingCategory = new Category("Streaming Services");

    entityManager.persistAndFlush(gameCategory);
    entityManager.persistAndFlush(socialCategory);
    entityManager.persistAndFlush(streamingCategory);

    // When
    List<Category> results = categoryRepository.findByNameContainingIgnoreCase("game");

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getName()).isEqualTo("Game Accounts");
  }

  @Test
  void findByNameContainingIgnoreCaseAndActive_ShouldReturnMatchingActiveCategories() {
    // Given
    Category activeGame = new Category("Game Accounts");
    activeGame.setActive(true);

    Category inactiveGame = new Category("Game Items");
    inactiveGame.setActive(false);

    entityManager.persistAndFlush(activeGame);
    entityManager.persistAndFlush(inactiveGame);

    // When
    List<Category> results =
        categoryRepository.findByNameContainingIgnoreCaseAndActive("game", true);

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getName()).isEqualTo("Game Accounts");
    assertThat(results).allMatch(Category::getActive);
  }

  @Test
  void findCategoriesWithProducts_ShouldReturnCategoriesHavingProducts() {
    // Given
    entityManager.persistAndFlush(rootCategory);
    entityManager.persistAndFlush(subCategory1);

    Product product = new Product("Test Product", new BigDecimal("10.00"), rootCategory);
    entityManager.persistAndFlush(product);

    // When
    List<Category> categoriesWithProducts = categoryRepository.findCategoriesWithProducts();

    // Then
    assertThat(categoriesWithProducts).hasSize(1);
    assertThat(categoriesWithProducts.get(0).getName()).isEqualTo("Games");
  }

  @Test
  void findCategoriesWithoutProducts_ShouldReturnCategoriesWithoutProducts() {
    // Given
    entityManager.persistAndFlush(rootCategory);
    entityManager.persistAndFlush(subCategory1);

    Product product = new Product("Test Product", new BigDecimal("10.00"), rootCategory);
    entityManager.persistAndFlush(product);

    // When
    List<Category> categoriesWithoutProducts = categoryRepository.findCategoriesWithoutProducts();

    // Then
    assertThat(categoriesWithoutProducts).hasSize(1);
    assertThat(categoriesWithoutProducts.get(0).getName()).isEqualTo("MMORPG");
  }

  @Test
  void countProductsByCategoryId_ShouldReturnCorrectCount() {
    // Given
    entityManager.persistAndFlush(rootCategory);

    Product product1 = new Product("Product 1", new BigDecimal("10.00"), rootCategory);
    Product product2 = new Product("Product 2", new BigDecimal("20.00"), rootCategory);
    entityManager.persistAndFlush(product1);
    entityManager.persistAndFlush(product2);

    // When
    long count = categoryRepository.countProductsByCategoryId(rootCategory.getId());

    // Then
    assertThat(count).isEqualTo(2);
  }

  @Test
  void countActiveProductsByCategoryId_ShouldReturnCorrectCount() {
    // Given
    entityManager.persistAndFlush(rootCategory);

    Product activeProduct = new Product("Active Product", new BigDecimal("10.00"), rootCategory);
    activeProduct.setActive(true);

    Product inactiveProduct =
        new Product("Inactive Product", new BigDecimal("20.00"), rootCategory);
    inactiveProduct.setActive(false);

    entityManager.persistAndFlush(activeProduct);
    entityManager.persistAndFlush(inactiveProduct);

    // When
    long count = categoryRepository.countActiveProductsByCategoryId(rootCategory.getId());

    // Then
    assertThat(count).isEqualTo(1);
  }

  @Test
  void findAllByOrderByNameAsc_ShouldReturnCategoriesOrderedByName() {
    // Given
    Category zCategory = new Category("Z Category");
    Category aCategory = new Category("A Category");
    Category mCategory = new Category("M Category");

    entityManager.persistAndFlush(zCategory);
    entityManager.persistAndFlush(aCategory);
    entityManager.persistAndFlush(mCategory);

    // When
    List<Category> orderedCategories = categoryRepository.findAllByOrderByNameAsc();

    // Then
    assertThat(orderedCategories).hasSize(3);
    assertThat(orderedCategories)
        .extracting(Category::getName)
        .containsExactly("A Category", "M Category", "Z Category");
  }

  @Test
  void findAllByOrderBySortOrderAscNameAsc_ShouldReturnCategoriesOrderedBySortThenName() {
    // Given
    Category category1 = new Category("B Category");
    category1.setSortOrder(2);

    Category category2 = new Category("A Category");
    category2.setSortOrder(1);

    Category category3 = new Category("C Category");
    category3.setSortOrder(1);

    entityManager.persistAndFlush(category1);
    entityManager.persistAndFlush(category2);
    entityManager.persistAndFlush(category3);

    // When
    List<Category> orderedCategories = categoryRepository.findAllByOrderBySortOrderAscNameAsc();

    // Then
    assertThat(orderedCategories).hasSize(3);
    assertThat(orderedCategories.get(0).getName()).isEqualTo("A Category"); // sortOrder 1, name A
    assertThat(orderedCategories.get(1).getName()).isEqualTo("C Category"); // sortOrder 1, name C
    assertThat(orderedCategories.get(2).getName()).isEqualTo("B Category"); // sortOrder 2, name B
  }

  @Test
  void save_ShouldPersistCategoryWithHierarchy() {
    // Given
    rootCategory.addSubCategory(subCategory1);
    rootCategory.addSubCategory(subCategory2);

    // When
    Category savedCategory = entityManager.persistAndFlush(rootCategory);

    // Then
    assertThat(savedCategory.getId()).isNotNull();
    assertThat(savedCategory.getName()).isEqualTo("Games");
    assertThat(savedCategory.getSubCategories()).hasSize(2);
    assertThat(savedCategory.getSubCategories())
        .extracting(Category::getName)
        .containsExactlyInAnyOrder("MMORPG", "FPS");
  }

  @Test
  void delete_ShouldRemoveCategoryAndUpdateHierarchy() {
    // Given
    Category savedRoot = entityManager.persistAndFlush(rootCategory);
    Category savedSub = entityManager.persistAndFlush(subCategory1);

    // When
    categoryRepository.delete(savedSub);
    entityManager.flush();

    // Then
    assertThat(categoryRepository.findById(savedSub.getId())).isEmpty();
    assertThat(categoryRepository.findById(savedRoot.getId())).isPresent();
  }

  @Test
  void findDirectSubcategories_ShouldReturnDirectSubcategories() {
    // Given - Create a deeper hierarchy
    entityManager.persistAndFlush(rootCategory);
    entityManager.persistAndFlush(subCategory1);
    entityManager.persistAndFlush(subCategory2);

    Category subSubCategory = new Category("WoW", "World of Warcraft");
    subSubCategory.setParentCategory(subCategory1);
    subSubCategory.setSortOrder(1);
    entityManager.persistAndFlush(subSubCategory);

    // When
    List<Category> directSubs = categoryRepository.findDirectSubcategories(rootCategory.getId());

    // Then
    assertThat(directSubs).hasSize(2);
    assertThat(directSubs)
        .extracting(Category::getName)
        .containsExactly("MMORPG", "FPS"); // Ordered by sortOrder, name
  }

  @Test
  void findByParentId_ShouldReturnCategoriesByParent() {
    // Given - Create multi-level hierarchy
    entityManager.persistAndFlush(rootCategory);
    entityManager.persistAndFlush(subCategory1);
    entityManager.persistAndFlush(subCategory2);

    Category subSubCategory = new Category("WoW", "World of Warcraft");
    subSubCategory.setParentCategory(subCategory1);
    subSubCategory.setSortOrder(1);
    entityManager.persistAndFlush(subSubCategory);

    // When
    List<Category> rootCategories = categoryRepository.findByParentId(null);
    List<Category> level1Categories = categoryRepository.findByParentId(rootCategory.getId());
    List<Category> level2Categories = categoryRepository.findByParentId(subCategory1.getId());

    // Then
    assertThat(rootCategories).hasSize(1);
    assertThat(rootCategories.get(0).getName()).isEqualTo("Games");

    assertThat(level1Categories).hasSize(2);
    assertThat(level1Categories).extracting(Category::getName).containsExactly("MMORPG", "FPS");

    assertThat(level2Categories).hasSize(1);
    assertThat(level2Categories.get(0).getName()).isEqualTo("WoW");
  }
}
