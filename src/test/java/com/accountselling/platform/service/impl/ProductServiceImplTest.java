package com.accountselling.platform.service.impl;

import com.accountselling.platform.exception.ResourceAlreadyExistsException;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.repository.CategoryRepository;
import com.accountselling.platform.repository.ProductRepository;
import com.accountselling.platform.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductServiceImpl.
 * Tests product management functionality including search, filtering, stock operations, and CRUD.
 * 
 * ProductServiceImpl testing
 * Tests product management functions including search, filtering, stock management, and data operations
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category testCategory;
    private Product testProduct1;
    private Product testProduct2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testCategory = new Category("Gaming");
        testCategory.setId(UUID.randomUUID());
        testCategory.setActive(true);

        testProduct1 = new Product("World of Warcraft Account", new BigDecimal("1500.00"), testCategory);
        testProduct1.setId(UUID.randomUUID());
        testProduct1.setDescription("Level 80 Warrior with epic gear");
        testProduct1.setServer("Stormrage");
        testProduct1.setActive(true);
        testProduct1.setSortOrder(0);

        testProduct2 = new Product("Final Fantasy XIV Account", new BigDecimal("2000.00"), testCategory);
        testProduct2.setId(UUID.randomUUID());
        testProduct2.setDescription("Level 90 White Mage with raid experience");
        testProduct2.setServer("Gilgamesh");
        testProduct2.setActive(true);
        testProduct2.setSortOrder(1);

        pageable = PageRequest.of(0, 20, Sort.by("sortOrder").ascending());
    }

    // ========== Read Operations Tests ==========

    @Test
    void findById_WithExistingProduct_ShouldReturnProduct() {
        UUID productId = testProduct1.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));

        Optional<Product> result = productService.findById(productId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testProduct1);
        verify(productRepository).findById(productId);
    }

    @Test
    void findById_WithNonExistentProduct_ShouldReturnEmpty() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        Optional<Product> result = productService.findById(productId);

        assertThat(result).isEmpty();
        verify(productRepository).findById(productId);
    }

    @Test
    void findByName_WithExistingProduct_ShouldReturnProduct() {
        String productName = "World of Warcraft Account";
        when(productRepository.findByName(productName)).thenReturn(Optional.of(testProduct1));

        Optional<Product> result = productService.findByName(productName);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(productName);
        verify(productRepository).findByName(productName);
    }

    @Test
    void findAllProducts_ShouldReturnPaginatedProducts() {
        List<Product> products = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products, pageable, 2);
        when(productRepository.findAll(pageable)).thenReturn(productPage);

        Page<Product> result = productService.findAllProducts(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactlyElementsOf(products);
        verify(productRepository).findAll(pageable);
    }

    @Test
    void findActiveProducts_ShouldReturnOnlyActiveProducts() {
        List<Product> activeProducts = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(activeProducts, pageable, 2);
        when(productRepository.findByActive(true, pageable)).thenReturn(productPage);

        Page<Product> result = productService.findActiveProducts(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(Product::getActive);
        verify(productRepository).findByActive(true, pageable);
    }

    @Test
    void findProductsByCategory_WithoutSubcategories_ShouldReturnCategoryProducts() {
        UUID categoryId = testCategory.getId();
        List<Product> categoryProducts = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(categoryProducts, pageable, 2);
        when(productRepository.findActiveByCategoryId(categoryId, pageable)).thenReturn(productPage);

        Page<Product> result = productService.findProductsByCategory(categoryId, false, true, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(p -> p.getCategory().getId().equals(categoryId));
        verify(productRepository).findActiveByCategoryId(categoryId, pageable);
    }

    @Test
    void searchProductsByName_WithValidName_ShouldReturnMatchingProducts() {
        String searchName = "World";
        List<Product> matchingProducts = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(matchingProducts, pageable, 1);
        when(productRepository.findByNameContainingIgnoreCaseAndActive(searchName, true, pageable))
            .thenReturn(productPage);

        Page<Product> result = productService.searchProductsByName(searchName, true, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).containsIgnoringCase(searchName);
        verify(productRepository).findByNameContainingIgnoreCaseAndActive(searchName, true, pageable);
    }

    @Test
    void searchProductsByName_WithEmptyName_ShouldReturnActiveProducts() {
        List<Product> activeProducts = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(activeProducts, pageable, 2);
        when(productRepository.findByActive(true, pageable)).thenReturn(productPage);

        Page<Product> result = productService.searchProductsByName("", true, pageable);

        assertThat(result.getContent()).hasSize(2);
        verify(productRepository).findByActive(true, pageable);
    }

    @Test
    void findProductsByServer_WithValidServer_ShouldReturnServerProducts() {
        String server = "Stormrage";
        List<Product> serverProducts = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(serverProducts, pageable, 1);
        when(productRepository.findByServerAndActive(server, true, pageable)).thenReturn(productPage);

        Page<Product> result = productService.findProductsByServer(server, true, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getServer()).isEqualTo(server);
        verify(productRepository).findByServerAndActive(server, true, pageable);
    }

    @Test
    void findProductsByPriceRange_WithValidRange_ShouldReturnProductsInRange() {
        BigDecimal minPrice = new BigDecimal("1000.00");
        BigDecimal maxPrice = new BigDecimal("1800.00");
        List<Product> rangeProducts = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(rangeProducts, pageable, 1);
        when(productRepository.findActiveByPriceBetween(minPrice, maxPrice, pageable)).thenReturn(productPage);

        Page<Product> result = productService.findProductsByPriceRange(minPrice, maxPrice, true, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPrice()).isBetween(minPrice, maxPrice);
        verify(productRepository).findActiveByPriceBetween(minPrice, maxPrice, pageable);
    }

    @Test
    void findProductsByPriceRange_WithNullPrices_ShouldReturnActiveProducts() {
        List<Product> activeProducts = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(activeProducts, pageable, 2);
        when(productRepository.findByActive(true, pageable)).thenReturn(productPage);

        Page<Product> result = productService.findProductsByPriceRange(null, null, true, pageable);

        assertThat(result.getContent()).hasSize(2);
        verify(productRepository).findByActive(true, pageable);
    }

    @Test
    void searchProducts_WithCriteria_ShouldReturnMatchingProducts() {
        ProductService.ProductSearchCriteria criteria = new ProductService.ProductSearchCriteria();
        criteria.setName("World");
        criteria.setCategoryId(testCategory.getId());
        criteria.setServer("Stormrage");
        criteria.setActiveOnly(true);

        List<Product> matchingProducts = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(matchingProducts, pageable, 1);
        when(productRepository.searchProducts(
            criteria.getName(),
            criteria.getCategoryId(),
            criteria.getServer(),
            criteria.getMinPrice(),
            criteria.getMaxPrice(),
            criteria.getActiveOnly(),
            pageable
        )).thenReturn(productPage);

        Page<Product> result = productService.searchProducts(criteria, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).searchProducts(
            criteria.getName(),
            criteria.getCategoryId(),
            criteria.getServer(),
            criteria.getMinPrice(),
            criteria.getMaxPrice(),
            criteria.getActiveOnly(),
            pageable
        );
    }

    @Test
    void getAvailableServers_ShouldReturnDistinctServers() {
        List<String> servers = Arrays.asList("Stormrage", "Gilgamesh", "Tichondrius");
        when(productRepository.findDistinctServers()).thenReturn(servers);

        List<String> result = productService.getAvailableServers();

        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(servers);
        verify(productRepository).findDistinctServers();
    }

    @Test
    void existsByName_WithExistingName_ShouldReturnTrue() {
        String productName = "World of Warcraft Account";
        when(productRepository.existsByName(productName)).thenReturn(true);

        boolean result = productService.existsByName(productName);

        assertThat(result).isTrue();
        verify(productRepository).existsByName(productName);
    }

    // ========== Stock & Inventory Tests ==========

    @Test
    void getProductStockInfo_WithValidProduct_ShouldReturnStockInfo() {
        UUID productId = testProduct1.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));

        ProductService.ProductStockInfo result = productService.getProductStockInfo(productId);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(productId);
        verify(productRepository).findById(productId);
    }

    @Test
    void getProductStockInfo_WithNonExistentProduct_ShouldThrowException() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductStockInfo(productId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with ID: " + productId);
    }

    @Test
    void findProductsWithLowStock_ShouldReturnLowStockProducts() {
        List<Product> lowStockProducts = Arrays.asList(testProduct1);
        when(productRepository.findProductsWithLowStock()).thenReturn(lowStockProducts);

        List<Product> result = productService.findProductsWithLowStock();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testProduct1);
        verify(productRepository).findProductsWithLowStock();
    }

    @Test
    void findOutOfStockProducts_ShouldReturnOutOfStockProducts() {
        List<Product> outOfStockProducts = Arrays.asList(testProduct2);
        when(productRepository.findOutOfStockProducts()).thenReturn(outOfStockProducts);

        List<Product> result = productService.findOutOfStockProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testProduct2);
        verify(productRepository).findOutOfStockProducts();
    }

    // ========== Write Operations Tests ==========

    @Test
    void createProduct_WithValidData_ShouldCreateProduct() {
        String name = "New Game Account";
        String description = "High level character";
        BigDecimal price = new BigDecimal("1200.00");
        UUID categoryId = testCategory.getId();
        String server = "Stormrage";
        String imageUrl = "http://example.com/image.jpg";

        when(productRepository.existsByName(name)).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(UUID.randomUUID());
            return product;
        });

        Product result = productService.createProduct(name, description, price, categoryId, server, imageUrl);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getPrice()).isEqualTo(price);
        assertThat(result.getCategory()).isEqualTo(testCategory);
        assertThat(result.getServer()).isEqualTo(server);
        assertThat(result.getImageUrl()).isEqualTo(imageUrl);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_WithExistingName_ShouldThrowException() {
        String name = "Existing Product";
        when(productRepository.existsByName(name)).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(
            name, "description", new BigDecimal("100.00"), testCategory.getId(), "server", null))
            .isInstanceOf(ResourceAlreadyExistsException.class)
            .hasMessageContaining("Product name already exists: " + name);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_WithNonExistentCategory_ShouldThrowException() {
        String name = "New Product";
        UUID categoryId = UUID.randomUUID();
        
        when(productRepository.existsByName(name)).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(
            name, "description", new BigDecimal("100.00"), categoryId, "server", null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Category not found with ID: " + categoryId);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_WithValidData_ShouldUpdateProduct() {
        UUID productId = testProduct1.getId();
        String newName = "Updated Product Name";
        String newDescription = "Updated description";
        BigDecimal newPrice = new BigDecimal("1800.00");

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));
        when(productRepository.existsByName(newName)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct1);

        Product result = productService.updateProduct(productId, newName, newDescription, newPrice, null, null, null);

        assertThat(result).isNotNull();
        verify(productRepository).save(testProduct1);
        assertThat(testProduct1.getName()).isEqualTo(newName);
        assertThat(testProduct1.getDescription()).isEqualTo(newDescription);
        assertThat(testProduct1.getPrice()).isEqualTo(newPrice);
    }

    @Test
    void updateProduct_WithNonExistentProduct_ShouldThrowException() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(
            productId, "New Name", "description", new BigDecimal("100.00"), null, null, null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with ID: " + productId);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void moveProductToCategory_WithValidCategory_ShouldMoveProduct() {
        UUID productId = testProduct1.getId();
        Category newCategory = new Category("New Category");
        newCategory.setId(UUID.randomUUID());

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));
        when(categoryRepository.findById(newCategory.getId())).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct1);

        Product result = productService.moveProductToCategory(productId, newCategory.getId());

        assertThat(result).isNotNull();
        assertThat(result.getCategory()).isEqualTo(newCategory);
        verify(productRepository).save(testProduct1);
    }

    @Test
    void setProductActive_WithValidProduct_ShouldUpdateActiveStatus() {
        UUID productId = testProduct1.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct1);

        Product result = productService.setProductActive(productId, false);

        assertThat(result).isNotNull();
        assertThat(testProduct1.getActive()).isFalse();
        verify(productRepository).save(testProduct1);
    }

    @Test
    void deleteProduct_WithDeletableProduct_ShouldDeleteProduct() {
        UUID productId = testProduct1.getId();
        testProduct1.getStockItems().clear();
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));

        productService.deleteProduct(productId);

        verify(productRepository).delete(testProduct1);
    }

    @Test
    void deleteProduct_WithStockItems_ShouldThrowException() {
        UUID productId = testProduct1.getId();
        
        // Create a mock stock item and add it to the product
        Stock mockStock = new Stock();
        testProduct1.getStockItems().add(mockStock);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));

        assertThatThrownBy(() -> productService.deleteProduct(productId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot delete product that has stock items or is referenced in orders");

        verify(productRepository, never()).delete(any(Product.class));
    }

    // ========== Validation & Utility Tests ==========

    @Test
    void canDeleteProduct_WithEmptyProduct_ShouldReturnTrue() {
        UUID productId = testProduct1.getId();
        testProduct1.getStockItems().clear();
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));

        boolean result = productService.canDeleteProduct(productId);

        assertThat(result).isTrue();
    }

    @Test
    void canDeleteProduct_WithStockItems_ShouldReturnFalse() {
        UUID productId = testProduct1.getId();
        
        // Create a mock stock item and add it to the product
        Stock mockStock = new Stock();
        testProduct1.getStockItems().add(mockStock);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));

        boolean result = productService.canDeleteProduct(productId);

        assertThat(result).isFalse();
    }

    @Test
    void reorderProducts_WithValidProducts_ShouldUpdateSortOrder() {
        UUID categoryId = testCategory.getId();
        List<UUID> productIds = Arrays.asList(testProduct2.getId(), testProduct1.getId());
        
        testProduct1.setCategory(testCategory);
        testProduct2.setCategory(testCategory);
        
        when(productRepository.findById(testProduct2.getId())).thenReturn(Optional.of(testProduct2));
        when(productRepository.findById(testProduct1.getId())).thenReturn(Optional.of(testProduct1));
        when(productRepository.saveAll(anyList())).thenReturn(Arrays.asList(testProduct2, testProduct1));

        productService.reorderProducts(categoryId, productIds);

        assertThat(testProduct2.getSortOrder()).isEqualTo(0);
        assertThat(testProduct1.getSortOrder()).isEqualTo(1);
        verify(productRepository).saveAll(anyList());
    }

    @Test
    void bulkUpdateProductsActive_WithValidProducts_ShouldUpdateActiveStatus() {
        List<UUID> productIds = Arrays.asList(testProduct1.getId(), testProduct2.getId());
        List<Product> products = Arrays.asList(testProduct1, testProduct2);
        
        when(productRepository.findAllById(productIds)).thenReturn(products);
        when(productRepository.saveAll(products)).thenReturn(products);

        int result = productService.bulkUpdateProductsActive(productIds, false);

        assertThat(result).isEqualTo(2);
        assertThat(testProduct1.getActive()).isFalse();
        assertThat(testProduct2.getActive()).isFalse();
        verify(productRepository).saveAll(products);
    }

    // ========== ProductSearchCriteria Tests ==========

    @Test
    void productSearchCriteria_DefaultValues_ShouldBeCorrect() {
        ProductService.ProductSearchCriteria criteria = new ProductService.ProductSearchCriteria();

        assertThat(criteria.isIncludeSubcategories()).isFalse();
        assertThat(criteria.getActiveOnly()).isTrue();
        assertThat(criteria.getName()).isNull();
        assertThat(criteria.getCategoryId()).isNull();
        assertThat(criteria.getServer()).isNull();
        assertThat(criteria.getMinPrice()).isNull();
        assertThat(criteria.getMaxPrice()).isNull();
        assertThat(criteria.getInStock()).isNull();
    }

    @Test
    void productSearchCriteria_WithName_ShouldSetName() {
        String searchName = "World of Warcraft";
        ProductService.ProductSearchCriteria criteria = new ProductService.ProductSearchCriteria(searchName);

        assertThat(criteria.getName()).isEqualTo(searchName);
    }

    // ========== ProductStockInfo Tests ==========

    @Test
    void productStockInfo_WithValidData_ShouldCreateCorrectly() {
        UUID productId = UUID.randomUUID();
        long totalStock = 10;
        long availableStock = 7;
        long soldStock = 2;
        long reservedStock = 1;
        boolean inStock = true;
        boolean lowStock = false;

        ProductService.ProductStockInfo stockInfo = new ProductService.ProductStockInfo(
            productId, totalStock, availableStock, soldStock, reservedStock, inStock, lowStock);

        assertThat(stockInfo.getProductId()).isEqualTo(productId);
        assertThat(stockInfo.getTotalStock()).isEqualTo(totalStock);
        assertThat(stockInfo.getAvailableStock()).isEqualTo(availableStock);
        assertThat(stockInfo.getSoldStock()).isEqualTo(soldStock);
        assertThat(stockInfo.getReservedStock()).isEqualTo(reservedStock);
        assertThat(stockInfo.isInStock()).isEqualTo(inStock);
        assertThat(stockInfo.isLowStock()).isEqualTo(lowStock);
    }
}