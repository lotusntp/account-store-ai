package com.accountselling.platform.repository;

import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProductRepository.
 * Tests repository methods for product management functionality including
 * search, filtering, and inventory-related operations.
 */
@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Category gameCategory;
    private Category socialCategory;
    private Product activeProduct;
    private Product inactiveProduct;
    private Product expensiveProduct;

    @BeforeEach
    void setUp() {
        // Create test categories
        gameCategory = new Category("Games", "Gaming accounts");
        gameCategory.setActive(true);
        
        socialCategory = new Category("Social", "Social media accounts");
        socialCategory.setActive(true);

        // Create test products
        activeProduct = new Product("WoW Account", new BigDecimal("50.00"), gameCategory);
        activeProduct.setDescription("World of Warcraft account");
        activeProduct.setServer("Stormrage");
        activeProduct.setActive(true);
        activeProduct.setSortOrder(1);
        activeProduct.setLowStockThreshold(3);

        inactiveProduct = new Product("Inactive Game", new BigDecimal("30.00"), gameCategory);
        inactiveProduct.setDescription("Inactive game account");
        inactiveProduct.setServer("TestServer");
        inactiveProduct.setActive(false);
        inactiveProduct.setSortOrder(2);

        expensiveProduct = new Product("Premium Account", new BigDecimal("200.00"), socialCategory);
        expensiveProduct.setDescription("Premium social media account");
        expensiveProduct.setActive(true);
        expensiveProduct.setSortOrder(1);
    }

    @Test
    void findByName_WhenProductExists_ShouldReturnProduct() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);

        // When
        Optional<Product> result = productRepository.findByName("WoW Account");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("WoW Account");
        assertThat(result.get().getPrice()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void findByName_WhenProductDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Product> result = productRepository.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void existsByName_WhenProductExists_ShouldReturnTrue() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);

        // When
        boolean exists = productRepository.existsByName("WoW Account");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByName_WhenProductDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = productRepository.existsByName("NonExistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findByActive_ShouldReturnProductsWithSpecifiedStatus() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(inactiveProduct);

        // When
        List<Product> activeProducts = productRepository.findByActive(true);
        List<Product> inactiveProducts = productRepository.findByActive(false);

        // Then
        assertThat(activeProducts).hasSize(1);
        assertThat(activeProducts.get(0).getName()).isEqualTo("WoW Account");

        assertThat(inactiveProducts).hasSize(1);
        assertThat(inactiveProducts.get(0).getName()).isEqualTo("Inactive Game");
    }

    @Test
    void findByActive_WithPagination_ShouldReturnPagedResults() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(socialCategory);
        entityManager.persistAndFlush(expensiveProduct);

        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Product> result = productRepository.findByActive(true, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findByCategoryId_ShouldReturnProductsInCategory() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(socialCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(inactiveProduct);
        entityManager.persistAndFlush(expensiveProduct);

        // When
        List<Product> gameProducts = productRepository.findByCategoryId(gameCategory.getId());

        // Then
        assertThat(gameProducts).hasSize(2);
        assertThat(gameProducts).extracting(Product::getName)
                .containsExactlyInAnyOrder("WoW Account", "Inactive Game");
    }

    @Test
    void findActiveByCategoryId_WithPagination_ShouldReturnActiveProductsInCategory() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(inactiveProduct);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.findActiveByCategoryId(gameCategory.getId(), pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("WoW Account");
        assertThat(result.getContent()).allMatch(Product::getActive);
    }

    @Test
    void findByCategoryIdIn_ShouldReturnProductsInMultipleCategories() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(socialCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(expensiveProduct);

        List<UUID> categoryIds = List.of(gameCategory.getId(), socialCategory.getId());

        // When
        List<Product> products = productRepository.findByCategoryIdIn(categoryIds);

        // Then
        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName)
                .containsExactlyInAnyOrder("WoW Account", "Premium Account");
    }

    @Test
    void findActiveByCategoryIdIn_WithPagination_ShouldReturnActiveProductsInMultipleCategories() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(socialCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(inactiveProduct);
        entityManager.persistAndFlush(expensiveProduct);

        List<UUID> categoryIds = List.of(gameCategory.getId(), socialCategory.getId());
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.findActiveByCategoryIdIn(categoryIds, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("WoW Account", "Premium Account");
        assertThat(result.getContent()).allMatch(Product::getActive);
    }

    @Test
    void findByNameContainingIgnoreCase_ShouldReturnMatchingProducts() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(socialCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(expensiveProduct);

        // When
        List<Product> results = productRepository.findByNameContainingIgnoreCase("account");

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Product::getName)
                .containsExactlyInAnyOrder("WoW Account", "Premium Account");
    }

    @Test
    void findByNameContainingIgnoreCaseAndActive_WithPagination_ShouldReturnMatchingActiveProducts() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(inactiveProduct);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.findByNameContainingIgnoreCaseAndActive("game", true, pageable);

        // Then
        assertThat(result.getContent()).hasSize(0); // "WoW Account" doesn't contain "game"
        
        // Test with "wow"
        Page<Product> wowResult = productRepository.findByNameContainingIgnoreCaseAndActive("wow", true, pageable);
        assertThat(wowResult.getContent()).hasSize(1);
        assertThat(wowResult.getContent().get(0).getName()).isEqualTo("WoW Account");
    }

    @Test
    void findByServer_ShouldReturnProductsForSpecificServer() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(inactiveProduct);

        // When
        List<Product> stormrageProducts = productRepository.findByServer("Stormrage");
        List<Product> testServerProducts = productRepository.findByServer("TestServer");

        // Then
        assertThat(stormrageProducts).hasSize(1);
        assertThat(stormrageProducts.get(0).getName()).isEqualTo("WoW Account");

        assertThat(testServerProducts).hasSize(1);
        assertThat(testServerProducts.get(0).getName()).isEqualTo("Inactive Game");
    }

    @Test
    void findByServerAndActive_WithPagination_ShouldReturnActiveProductsForServer() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(inactiveProduct);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.findByServerAndActive("Stormrage", true, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("WoW Account");
        assertThat(result.getContent()).allMatch(Product::getActive);
    }

    @Test
    void findByPriceBetween_ShouldReturnProductsInPriceRange() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(socialCategory);
        entityManager.persistAndFlush(activeProduct); // 50.00
        entityManager.persistAndFlush(inactiveProduct); // 30.00
        entityManager.persistAndFlush(expensiveProduct); // 200.00

        // When
        List<Product> midRangeProducts = productRepository.findByPriceBetween(
                new BigDecimal("25.00"), new BigDecimal("100.00"));

        // Then
        assertThat(midRangeProducts).hasSize(2);
        assertThat(midRangeProducts).extracting(Product::getName)
                .containsExactlyInAnyOrder("Inactive Game", "WoW Account");
    }

    @Test
    void findActiveByPriceBetween_WithPagination_ShouldReturnActiveProductsInPriceRange() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(socialCategory);
        entityManager.persistAndFlush(activeProduct); // 50.00, active
        entityManager.persistAndFlush(inactiveProduct); // 30.00, inactive
        entityManager.persistAndFlush(expensiveProduct); // 200.00, active

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.findActiveByPriceBetween(
                new BigDecimal("25.00"), new BigDecimal("100.00"), pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("WoW Account");
        assertThat(result.getContent()).allMatch(Product::getActive);
    }

    @Test
    void searchProducts_WithMultipleCriteria_ShouldReturnMatchingProducts() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(socialCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(expensiveProduct);

        Pageable pageable = PageRequest.of(0, 10);

        // When - Search for active products in game category with price <= 100
        Page<Product> result = productRepository.searchProducts(
                null, // namePattern
                gameCategory.getId(), // categoryId
                null, // server
                null, // minPrice
                new BigDecimal("100.00"), // maxPrice
                true, // active
                pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("WoW Account");
    }

    @Test
    void searchProducts_WithNamePattern_ShouldReturnMatchingProducts() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(socialCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(expensiveProduct);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.searchProducts(
                "wow", // namePattern
                null, // categoryId
                null, // server
                null, // minPrice
                null, // maxPrice
                true, // active
                pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("WoW Account");
    }

    @Test
    void findProductsWithLowStock_ShouldReturnProductsBelowThreshold() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct); // threshold = 3

        // Add 2 available stock items (below threshold)
        Stock stock1 = new Stock();
        stock1.setProduct(activeProduct);
        stock1.setCredentials("encrypted_credentials_1");
        stock1.setSold(false);
        
        Stock stock2 = new Stock();
        stock2.setProduct(activeProduct);
        stock2.setCredentials("encrypted_credentials_2");
        stock2.setSold(false);

        entityManager.persistAndFlush(stock1);
        entityManager.persistAndFlush(stock2);

        // When
        List<Product> lowStockProducts = productRepository.findProductsWithLowStock();

        // Then
        assertThat(lowStockProducts).hasSize(1);
        assertThat(lowStockProducts.get(0).getName()).isEqualTo("WoW Account");
    }

    @Test
    void findOutOfStockProducts_ShouldReturnProductsWithNoAvailableStock() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);

        // Add only sold stock items
        Stock soldStock = new Stock();
        soldStock.setProduct(activeProduct);
        soldStock.setCredentials("encrypted_credentials");
        soldStock.setSold(true);
        entityManager.persistAndFlush(soldStock);

        // When
        List<Product> outOfStockProducts = productRepository.findOutOfStockProducts();

        // Then
        assertThat(outOfStockProducts).hasSize(1);
        assertThat(outOfStockProducts.get(0).getName()).isEqualTo("WoW Account");
    }

    @Test
    void findProductsWithAvailableStock_ShouldReturnProductsWithAvailableStock() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);

        // Add available stock
        Stock availableStock = new Stock();
        availableStock.setProduct(activeProduct);
        availableStock.setCredentials("encrypted_credentials");
        availableStock.setSold(false);
        entityManager.persistAndFlush(availableStock);

        // When
        List<Product> productsWithStock = productRepository.findProductsWithAvailableStock();

        // Then
        assertThat(productsWithStock).hasSize(1);
        assertThat(productsWithStock.get(0).getName()).isEqualTo("WoW Account");
    }

    @Test
    void findProductsWithAvailableStockByCategoryId_WithPagination_ShouldReturnProductsWithStockInCategory() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);

        // Add available stock
        Stock availableStock = new Stock();
        availableStock.setProduct(activeProduct);
        availableStock.setCredentials("encrypted_credentials");
        availableStock.setSold(false);
        entityManager.persistAndFlush(availableStock);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.findProductsWithAvailableStockByCategoryId(
                gameCategory.getId(), pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("WoW Account");
    }

    @Test
    void findDistinctServers_ShouldReturnUniqueServerNames() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct); // Stormrage
        entityManager.persistAndFlush(inactiveProduct); // TestServer

        Product anotherStormrageProduct = new Product("Another WoW", new BigDecimal("60.00"), gameCategory);
        anotherStormrageProduct.setServer("Stormrage");
        anotherStormrageProduct.setActive(true);
        entityManager.persistAndFlush(anotherStormrageProduct);

        // When
        List<String> servers = productRepository.findDistinctServers();

        // Then
        assertThat(servers).hasSize(1); // Only active products, so only "Stormrage"
        assertThat(servers).containsExactly("Stormrage");
    }

    @Test
    void findAllByOrderByNameAsc_ShouldReturnProductsOrderedByName() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(socialCategory);
        
        Product zProduct = new Product("Z Product", new BigDecimal("10.00"), gameCategory);
        Product aProduct = new Product("A Product", new BigDecimal("20.00"), gameCategory);
        Product mProduct = new Product("M Product", new BigDecimal("30.00"), socialCategory);
        
        entityManager.persistAndFlush(zProduct);
        entityManager.persistAndFlush(aProduct);
        entityManager.persistAndFlush(mProduct);

        // When
        List<Product> orderedProducts = productRepository.findAllByOrderByNameAsc();

        // Then
        assertThat(orderedProducts).hasSize(3);
        assertThat(orderedProducts).extracting(Product::getName)
                .containsExactly("A Product", "M Product", "Z Product");
    }

    @Test
    void findAllByOrderBySortOrderAscNameAsc_ShouldReturnProductsOrderedBySortThenName() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        
        Product product1 = new Product("B Product", new BigDecimal("10.00"), gameCategory);
        product1.setSortOrder(2);
        
        Product product2 = new Product("A Product", new BigDecimal("20.00"), gameCategory);
        product2.setSortOrder(1);
        
        Product product3 = new Product("C Product", new BigDecimal("30.00"), gameCategory);
        product3.setSortOrder(1);
        
        entityManager.persistAndFlush(product1);
        entityManager.persistAndFlush(product2);
        entityManager.persistAndFlush(product3);

        // When
        List<Product> orderedProducts = productRepository.findAllByOrderBySortOrderAscNameAsc();

        // Then
        assertThat(orderedProducts).hasSize(3);
        assertThat(orderedProducts.get(0).getName()).isEqualTo("A Product"); // sortOrder 1, name A
        assertThat(orderedProducts.get(1).getName()).isEqualTo("C Product"); // sortOrder 1, name C
        assertThat(orderedProducts.get(2).getName()).isEqualTo("B Product"); // sortOrder 2, name B
    }

    @Test
    void countByCategoryId_ShouldReturnCorrectCount() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(socialCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(inactiveProduct);
        entityManager.persistAndFlush(expensiveProduct);

        // When
        long gameCount = productRepository.countByCategoryId(gameCategory.getId());
        long socialCount = productRepository.countByCategoryId(socialCategory.getId());

        // Then
        assertThat(gameCount).isEqualTo(2);
        assertThat(socialCount).isEqualTo(1);
    }

    @Test
    void countByCategoryIdAndActive_ShouldReturnCorrectCount() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(inactiveProduct);

        // When
        long activeCount = productRepository.countByCategoryIdAndActive(gameCategory.getId(), true);
        long inactiveCount = productRepository.countByCategoryIdAndActive(gameCategory.getId(), false);

        // Then
        assertThat(activeCount).isEqualTo(1);
        assertThat(inactiveCount).isEqualTo(1);
    }

    @Test
    void save_ShouldPersistProductWithCategory() {
        // Given
        entityManager.persistAndFlush(gameCategory);

        // When
        Product savedProduct = entityManager.persistAndFlush(activeProduct);

        // Then
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("WoW Account");
        assertThat(savedProduct.getCategory().getName()).isEqualTo("Games");
    }

    @Test
    void delete_ShouldRemoveProductButKeepCategory() {
        // Given
        entityManager.persistAndFlush(gameCategory);
        Product savedProduct = entityManager.persistAndFlush(activeProduct);
        UUID categoryId = gameCategory.getId();

        // When
        productRepository.delete(savedProduct);
        entityManager.flush();

        // Then
        assertThat(productRepository.findById(savedProduct.getId())).isEmpty();
        assertThat(entityManager.find(Category.class, categoryId)).isNotNull();
    }
}