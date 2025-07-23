package com.accountselling.platform.service;

import com.accountselling.platform.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for Product management operations.
 * Provides business logic for product search, filtering, inventory management, and CRUD operations.
 * 
 * Interface for product management
 * Supports search, filtering, stock management, and product data operations
 */
public interface ProductService {

    // ========== Read Operations ==========

    /**
     * Find product by ID.
     * 
     * @param id the product ID
     * @return Optional containing the product if found
     */
    Optional<Product> findById(UUID id);

    /**
     * Find product by name.
     * 
     * @param name the product name
     * @return Optional containing the product if found
     */
    Optional<Product> findByName(String name);

    /**
     * Find all products with pagination.
     * 
     * @param pageable pagination parameters
     * @return page of products
     */
    Page<Product> findAllProducts(Pageable pageable);

    /**
     * Find all active products with pagination.
     * 
     * @param pageable pagination parameters
     * @return page of active products
     */
    Page<Product> findActiveProducts(Pageable pageable);

    /**
     * Find products by category with pagination.
     * 
     * @param categoryId the category ID
     * @param includeSubcategories whether to include products from subcategories
     * @param activeOnly whether to include only active products
     * @param pageable pagination parameters
     * @return page of products in the specified category
     */
    Page<Product> findProductsByCategory(UUID categoryId, boolean includeSubcategories, boolean activeOnly, Pageable pageable);

    /**
     * Search products by name with pagination.
     * Supports Thai language search
     * 
     * @param name the name pattern to search for
     * @param activeOnly whether to search only active products
     * @param pageable pagination parameters
     * @return page of products matching the name pattern
     */
    Page<Product> searchProductsByName(String name, boolean activeOnly, Pageable pageable);

    /**
     * Find products by server with pagination.
     * 
     * @param server the server name
     * @param activeOnly whether to include only active products
     * @param pageable pagination parameters
     * @return page of products for the specified server
     */
    Page<Product> findProductsByServer(String server, boolean activeOnly, Pageable pageable);

    /**
     * Find products within price range with pagination.
     * 
     * @param minPrice minimum price (inclusive)
     * @param maxPrice maximum price (inclusive)
     * @param activeOnly whether to include only active products
     * @param pageable pagination parameters
     * @return page of products within the price range
     */
    Page<Product> findProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, boolean activeOnly, Pageable pageable);

    /**
     * Advanced product search with multiple filters.
     * 
     * @param searchCriteria the search criteria object
     * @param pageable pagination parameters
     * @return page of products matching the search criteria
     */
    Page<Product> searchProducts(ProductSearchCriteria searchCriteria, Pageable pageable);

    /**
     * Get distinct servers from all products.
     * 
     * @return list of distinct server names
     */
    List<String> getAvailableServers();

    /**
     * Check if product exists by name.
     * 
     * @param name the product name to check
     * @return true if product exists, false otherwise
     */
    boolean existsByName(String name);

    // ========== Stock & Inventory Operations ==========

    /**
     * Find products with available stock.
     * 
     * @param pageable pagination parameters
     * @return page of products with available stock
     */
    Page<Product> findProductsWithAvailableStock(Pageable pageable);

    /**
     * Find products with available stock by category.
     * 
     * @param categoryId the category ID
     * @param includeSubcategories whether to include subcategories
     * @param pageable pagination parameters
     * @return page of products with available stock in the category
     */
    Page<Product> findProductsWithAvailableStockByCategory(UUID categoryId, boolean includeSubcategories, Pageable pageable);

    /**
     * Find products with low stock.
     * 
     * @return list of products with low stock
     */
    List<Product> findProductsWithLowStock();

    /**
     * Find products that are out of stock.
     * 
     * @return list of out of stock products
     */
    List<Product> findOutOfStockProducts();

    /**
     * Get stock information for a product.
     * 
     * @param productId the product ID
     * @return stock information object
     */
    ProductStockInfo getProductStockInfo(UUID productId);

    // ========== Write Operations ==========

    /**
     * Create a new product.
     * 
     * @param name the product name
     * @param description the product description
     * @param price the product price
     * @param categoryId the category ID
     * @param server the server name
     * @param imageUrl the image URL
     * @return the created product
     * @throws com.accountselling.platform.exception.ResourceAlreadyExistsException if product name already exists
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if category not found
     */
    Product createProduct(String name, String description, BigDecimal price, UUID categoryId, String server, String imageUrl);

    /**
     * Create a new product with all optional fields.
     * 
     * @param name the product name
     * @param description the product description
     * @param price the product price
     * @param categoryId the category ID
     * @param server the server name
     * @param imageUrl the image URL
     * @param sortOrder the sort order
     * @param lowStockThreshold the low stock threshold
     * @return the created product
     */
    Product createProduct(String name, String description, BigDecimal price, UUID categoryId, 
                         String server, String imageUrl, Integer sortOrder, Integer lowStockThreshold);

    /**
     * Update product information.
     * 
     * @param productId the product ID to update
     * @param name the new product name
     * @param description the new product description
     * @param price the new product price
     * @param categoryId the new category ID
     * @param server the new server name
     * @param imageUrl the new image URL
     * @return the updated product
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if product or category not found
     * @throws com.accountselling.platform.exception.ResourceAlreadyExistsException if new name already exists
     */
    Product updateProduct(UUID productId, String name, String description, BigDecimal price, 
                         UUID categoryId, String server, String imageUrl);

    /**
     * Update product settings.
     * 
     * @param productId the product ID
     * @param sortOrder the new sort order
     * @param lowStockThreshold the new low stock threshold
     * @return the updated product
     */
    Product updateProductSettings(UUID productId, Integer sortOrder, Integer lowStockThreshold);

    /**
     * Move product to a different category.
     * 
     * @param productId the product ID
     * @param newCategoryId the new category ID
     * @return the updated product
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if product or category not found
     */
    Product moveProductToCategory(UUID productId, UUID newCategoryId);

    /**
     * Set product active status.
     * 
     * @param productId the product ID
     * @param active the new active status
     * @return the updated product
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if product not found
     */
    Product setProductActive(UUID productId, boolean active);

    /**
     * Delete product by ID.
     * 
     * @param productId the product ID to delete
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if product not found
     * @throws IllegalStateException if product has stock items or is referenced in orders
     */
    void deleteProduct(UUID productId);

    /**
     * Force delete product and all associated data.
     * 
     * @param productId the product ID to delete
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if product not found
     */
    void forceDeleteProduct(UUID productId);

    // ========== Validation & Utility Methods ==========

    /**
     * Check if product can be deleted safely.
     * 
     * @param productId the product ID to check
     * @return true if can be deleted safely, false otherwise
     */
    boolean canDeleteProduct(UUID productId);

    /**
     * Reorder products within a category.
     * 
     * @param categoryId the category ID
     * @param productIds list of product IDs in the desired order
     */
    void reorderProducts(UUID categoryId, List<UUID> productIds);

    /**
     * Bulk update products active status.
     * 
     * @param productIds list of product IDs
     * @param active the new active status
     * @return count of updated products
     */
    int bulkUpdateProductsActive(List<UUID> productIds, boolean active);

    /**
     * Search product criteria class for advanced search.
     */
    class ProductSearchCriteria {
        private String name; // product name to search
        private UUID categoryId; // category ID
        private boolean includeSubcategories = false; // include subcategories
        private String server; // server name
        private BigDecimal minPrice; // minimum price
        private BigDecimal maxPrice; // maximum price
        private Boolean inStock; // has stock or not
        private Boolean activeOnly = true; // only active products

        // Constructors
        public ProductSearchCriteria() {}

        public ProductSearchCriteria(String name) {
            this.name = name;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

        public boolean isIncludeSubcategories() { return includeSubcategories; }
        public void setIncludeSubcategories(boolean includeSubcategories) { this.includeSubcategories = includeSubcategories; }

        public String getServer() { return server; }
        public void setServer(String server) { this.server = server; }

        public BigDecimal getMinPrice() { return minPrice; }
        public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

        public BigDecimal getMaxPrice() { return maxPrice; }
        public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

        public Boolean getInStock() { return inStock; }
        public void setInStock(Boolean inStock) { this.inStock = inStock; }

        public Boolean getActiveOnly() { return activeOnly; }
        public void setActiveOnly(Boolean activeOnly) { this.activeOnly = activeOnly; }
    }

    /**
     * Product stock information class.
     */
    class ProductStockInfo {
        private final UUID productId;
        private final long totalStock;
        private final long availableStock;
        private final long soldStock;
        private final long reservedStock;
        private final boolean inStock;
        private final boolean lowStock;

        public ProductStockInfo(UUID productId, long totalStock, long availableStock, 
                               long soldStock, long reservedStock, boolean inStock, boolean lowStock) {
            this.productId = productId;
            this.totalStock = totalStock;
            this.availableStock = availableStock;
            this.soldStock = soldStock;
            this.reservedStock = reservedStock;
            this.inStock = inStock;
            this.lowStock = lowStock;
        }

        // Getters
        public UUID getProductId() { return productId; }
        public long getTotalStock() { return totalStock; }
        public long getAvailableStock() { return availableStock; }
        public long getSoldStock() { return soldStock; }
        public long getReservedStock() { return reservedStock; }
        public boolean isInStock() { return inStock; }
        public boolean isLowStock() { return lowStock; }
    }
}