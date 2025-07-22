package com.accountselling.platform.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Product entity.
 * Tests product creation, stock management, and business logic.
 */
@DisplayName("Product Entity Tests")
class ProductTest {

    private Product product;
    private Category category;
    private Stock stock1;
    private Stock stock2;
    private Stock stock3;

    @BeforeEach
    void setUp() {
        category = new Category("Games");
        product = new Product();
        stock1 = new Stock();
        stock2 = new Stock();
        stock3 = new Stock();
        
        // Set up stock items with different states and unique credentials
        stock1.setCredentials("user1:pass1");
        stock1.setSold(false);
        stock1.setReservedUntil(null);
        
        stock2.setCredentials("user2:pass2");
        stock2.setSold(true);
        stock2.setSoldAt(LocalDateTime.now());
        
        stock3.setCredentials("user3:pass3");
        stock3.setSold(false);
        stock3.setReservedUntil(LocalDateTime.now().plusMinutes(30));
    }

    @Test
    @DisplayName("Should create product with default constructor")
    void shouldCreateProductWithDefaultConstructor() {
        Product newProduct = new Product();
        
        assertNotNull(newProduct);
        assertNull(newProduct.getName());
        assertNull(newProduct.getDescription());
        assertNull(newProduct.getPrice());
        assertTrue(newProduct.getActive());
        assertEquals(0, newProduct.getSortOrder());
        assertEquals(5, newProduct.getLowStockThreshold());
        assertNotNull(newProduct.getStockItems());
        assertTrue(newProduct.getStockItems().isEmpty());
    }

    @Test
    @DisplayName("Should create product with name and price constructor")
    void shouldCreateProductWithNameAndPriceConstructor() {
        BigDecimal price = new BigDecimal("99.99");
        Product newProduct = new Product("Test Product", price);
        
        assertEquals("Test Product", newProduct.getName());
        assertEquals(price, newProduct.getPrice());
        assertTrue(newProduct.getActive());
    }

    @Test
    @DisplayName("Should create product with name, price, and category constructor")
    void shouldCreateProductWithNamePriceAndCategoryConstructor() {
        BigDecimal price = new BigDecimal("149.99");
        Product newProduct = new Product("Game Account", price, category);
        
        assertEquals("Game Account", newProduct.getName());
        assertEquals(price, newProduct.getPrice());
        assertEquals(category, newProduct.getCategory());
    }

    @Test
    @DisplayName("Should create product with all main fields constructor")
    void shouldCreateProductWithAllMainFieldsConstructor() {
        BigDecimal price = new BigDecimal("199.99");
        Product newProduct = new Product("Premium Account", "High level account", price, category);
        
        assertEquals("Premium Account", newProduct.getName());
        assertEquals("High level account", newProduct.getDescription());
        assertEquals(price, newProduct.getPrice());
        assertEquals(category, newProduct.getCategory());
    }

    @Test
    @DisplayName("Should set and get product properties")
    void shouldSetAndGetProductProperties() {
        UUID id = UUID.randomUUID();
        BigDecimal price = new BigDecimal("299.99");
        
        product.setId(id);
        product.setName("Epic Game Account");
        product.setDescription("Legendary account with rare items");
        product.setPrice(price);
        product.setImageUrl("https://example.com/image.jpg");
        product.setServer("Asia");
        product.setActive(false);
        product.setSortOrder(10);
        product.setLowStockThreshold(3);
        product.setCategory(category);

        assertEquals(id, product.getId());
        assertEquals("Epic Game Account", product.getName());
        assertEquals("Legendary account with rare items", product.getDescription());
        assertEquals(price, product.getPrice());
        assertEquals("https://example.com/image.jpg", product.getImageUrl());
        assertEquals("Asia", product.getServer());
        assertFalse(product.getActive());
        assertEquals(10, product.getSortOrder());
        assertEquals(3, product.getLowStockThreshold());
        assertEquals(category, product.getCategory());
    }

    @Test
    @DisplayName("Should add and remove stock items")
    void shouldAddAndRemoveStockItems() {
        product.addStockItem(stock1);
        
        assertTrue(product.getStockItems().contains(stock1));
        assertEquals(product, stock1.getProduct());
        assertEquals(1, product.getStockItems().size());
        
        product.removeStockItem(stock1);
        
        assertFalse(product.getStockItems().contains(stock1));
        assertNull(stock1.getProduct());
        assertEquals(0, product.getStockItems().size());
    }

    @Test
    @DisplayName("Should calculate available stock count correctly")
    void shouldCalculateAvailableStockCountCorrectly() {
        // Set up stock items properly
        stock1.setProduct(product);
        stock2.setProduct(product);
        stock3.setProduct(product);
        
        product.addStockItem(stock1); // Available
        product.addStockItem(stock2); // Sold
        product.addStockItem(stock3); // Reserved
        
        assertEquals(1, product.getAvailableStockCount());
    }

    @Test
    @DisplayName("Should calculate total stock count correctly")
    void shouldCalculateTotalStockCountCorrectly() {
        assertEquals(0, product.getTotalStockCount()); // No stock items
        
        // Use addStockItem method which properly sets up bidirectional relationship
        product.addStockItem(stock1);
        product.addStockItem(stock2);
        product.addStockItem(stock3);
        
        assertEquals(3, product.getTotalStockCount());
    }

    @Test
    @DisplayName("Should calculate sold stock count correctly")
    void shouldCalculateSoldStockCountCorrectly() {
        assertEquals(0, product.getSoldStockCount()); // No stock items
        
        product.addStockItem(stock1); // Available
        product.addStockItem(stock2); // Sold
        product.addStockItem(stock3); // Reserved
        
        assertEquals(1, product.getSoldStockCount());
    }

    @Test
    @DisplayName("Should calculate reserved stock count correctly")
    void shouldCalculateReservedStockCountCorrectly() {
        assertEquals(0, product.getReservedStockCount()); // No stock items
        
        product.addStockItem(stock1); // Available
        product.addStockItem(stock2); // Sold
        product.addStockItem(stock3); // Reserved
        
        assertEquals(1, product.getReservedStockCount());
    }

    @Test
    @DisplayName("Should check if product is in stock")
    void shouldCheckIfProductIsInStock() {
        assertFalse(product.isInStock()); // No stock items
        
        product.addStockItem(stock2); // Only sold item
        assertFalse(product.isInStock());
        
        product.addStockItem(stock1); // Add available item
        assertTrue(product.isInStock());
    }

    @Test
    @DisplayName("Should check if product is out of stock")
    void shouldCheckIfProductIsOutOfStock() {
        assertTrue(product.isOutOfStock()); // No stock items
        
        product.addStockItem(stock1); // Add available item
        assertFalse(product.isOutOfStock());
    }

    @Test
    @DisplayName("Should check if product is low stock")
    void shouldCheckIfProductIsLowStock() {
        product.setLowStockThreshold(2);
        
        assertTrue(product.isLowStock()); // No stock
        
        product.addStockItem(stock1); // 1 available
        assertTrue(product.isLowStock());
        
        Stock stock4 = new Stock();
        stock4.setCredentials("user4:pass4");
        stock4.setSold(false);
        stock4.setReservedUntil(null);
        product.addStockItem(stock4); // 2 available
        
        assertTrue(product.isLowStock()); // Equal to threshold
        
        Stock stock5 = new Stock();
        stock5.setCredentials("user5:pass5");
        stock5.setSold(false);
        stock5.setReservedUntil(null);
        product.addStockItem(stock5); // 3 available
        
        assertFalse(product.isLowStock()); // Above threshold
    }

    @Test
    @DisplayName("Should check if product is available for purchase")
    void shouldCheckIfProductIsAvailableForPurchase() {
        product.setActive(true);
        assertFalse(product.isAvailableForPurchase()); // No stock
        
        product.addStockItem(stock1); // Add available stock
        assertTrue(product.isAvailableForPurchase());
        
        product.setActive(false); // Deactivate product
        assertFalse(product.isAvailableForPurchase());
    }

    @Test
    @DisplayName("Should get next available stock")
    void shouldGetNextAvailableStock() {
        assertNull(product.getNextAvailableStock()); // No stock
        
        product.addStockItem(stock2); // Sold
        product.addStockItem(stock3); // Reserved
        assertNull(product.getNextAvailableStock()); // No available stock
        
        product.addStockItem(stock1); // Available
        assertEquals(stock1, product.getNextAvailableStock());
    }

    @Test
    @DisplayName("Should get category path")
    void shouldGetCategoryPath() {
        assertEquals("", product.getCategoryPath()); // No category
        
        Category parentCategory = new Category("Games");
        Category childCategory = new Category("RPG");
        parentCategory.addSubCategory(childCategory);
        
        product.setCategory(childCategory);
        assertEquals("Games > RPG", product.getCategoryPath());
    }

    @Test
    @DisplayName("Should format price correctly")
    void shouldFormatPriceCorrectly() {
        product.setPrice(new BigDecimal("99.99"));
        assertEquals("฿99.99", product.getFormattedPrice());
        
        product.setPrice(new BigDecimal("1500.00"));
        assertEquals("฿1500.00", product.getFormattedPrice());
    }

    @Test
    @DisplayName("Should check if product belongs to category")
    void shouldCheckIfProductBelongsToCategory() {
        Category rootCategory = new Category("Games");
        Category subCategory = new Category("RPG");
        Category subSubCategory = new Category("MMORPG");
        
        rootCategory.addSubCategory(subCategory);
        subCategory.addSubCategory(subSubCategory);
        
        product.setCategory(subSubCategory);
        
        assertTrue(product.belongsToCategory(subSubCategory));
        assertTrue(product.belongsToCategory(subCategory));
        assertTrue(product.belongsToCategory(rootCategory));
        
        Category otherCategory = new Category("Other");
        assertFalse(product.belongsToCategory(otherCategory));
        
        // Test null cases
        product.setCategory(null);
        assertFalse(product.belongsToCategory(rootCategory));
        
        product.setCategory(subSubCategory);
        assertFalse(product.belongsToCategory(null));
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        Product product1 = new Product("Test Product", new BigDecimal("99.99"));
        Product product2 = new Product("Test Product", new BigDecimal("149.99"));
        Product product3 = new Product("Different Product", new BigDecimal("99.99"));
        
        assertEquals(product1, product2); // Same name
        assertNotEquals(product1, product3); // Different name
        assertNotEquals(product1, null);
        assertNotEquals(product1, "not a product");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        Product product1 = new Product("Test Product", new BigDecimal("99.99"));
        Product product2 = new Product("Test Product", new BigDecimal("149.99"));
        
        assertEquals(product1.hashCode(), product2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));
        product.setActive(true);
        
        String toString = product.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("Product("));
        assertTrue(toString.contains("name=Test Product"));
        assertTrue(toString.contains("price=99.99"));
        assertTrue(toString.contains("active=true"));
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        Product newProduct = new Product();
        
        assertNull(newProduct.getName());
        assertNull(newProduct.getPrice());
        assertNull(newProduct.getCategory());
        
        // Should not throw exceptions
        assertEquals(0, newProduct.getAvailableStockCount());
        assertEquals(0, newProduct.getTotalStockCount());
        assertFalse(newProduct.isInStock());
        assertTrue(newProduct.isOutOfStock());
        assertNull(newProduct.getNextAvailableStock());
        assertEquals("", newProduct.getCategoryPath());
        assertFalse(newProduct.belongsToCategory(category));
    }
}