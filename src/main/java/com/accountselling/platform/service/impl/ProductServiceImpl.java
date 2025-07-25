package com.accountselling.platform.service.impl;

import com.accountselling.platform.exception.ResourceAlreadyExistsException;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.repository.CategoryRepository;
import com.accountselling.platform.repository.ProductRepository;
import com.accountselling.platform.repository.StockRepository;
import com.accountselling.platform.service.ProductService;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Implementation of ProductService for product management operations. Handles product search,
 * filtering, inventory management, and CRUD operations.
 *
 * <p>การใช้งาน ProductService สำหรับการจัดการสินค้า รองรับการค้นหา การกรอง การจัดการสต็อก
 * และการจัดการข้อมูลสินค้า
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final StockRepository stockRepository;

  // ========== Read Operations ==========

  @Override
  @Transactional(readOnly = true)
  public Optional<Product> findById(UUID id) {
    log.debug("Finding product by ID: {}", id);
    return productRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Product> findByName(String name) {
    log.debug("Finding product by name: {}", name);
    return productRepository.findByName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Product> findAllProducts(Pageable pageable) {
    log.debug("Finding all products with pagination: {}", pageable);
    return productRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Product> findActiveProducts(Pageable pageable) {
    log.debug("Finding active products with pagination: {}", pageable);
    return productRepository.findByActive(true, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Product> findProductsByCategory(
      UUID categoryId, boolean includeSubcategories, boolean activeOnly, Pageable pageable) {
    log.debug(
        "Finding products by category ID: {}, includeSubcategories: {}, activeOnly: {}",
        categoryId,
        includeSubcategories,
        activeOnly);

    if (!includeSubcategories) {
      if (activeOnly) {
        return productRepository.findActiveByCategoryId(categoryId, pageable);
      } else {
        return productRepository.findAll(pageable); // Apply category filter manually
      }
    } else {
      // Get all descendant categories
      List<UUID> categoryIds = getAllCategoryIds(categoryId);

      if (activeOnly) {
        return productRepository.findActiveByCategoryIdIn(categoryIds, pageable);
      } else {
        // For non-active search with multiple categories, use manual pagination
        List<Product> allProducts = productRepository.findByCategoryIdIn(categoryIds);
        return convertListToPage(allProducts, pageable);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Product> searchProductsByName(String name, boolean activeOnly, Pageable pageable) {
    log.debug("Searching products by name: {}, activeOnly: {}", name, activeOnly);

    if (!StringUtils.hasText(name)) {
      return activeOnly ? findActiveProducts(pageable) : findAllProducts(pageable);
    }

    if (activeOnly) {
      return productRepository.findByNameContainingIgnoreCaseAndActive(name, true, pageable);
    } else {
      return productRepository.findAll(pageable); // Apply name filter manually
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Product> findProductsByServer(String server, boolean activeOnly, Pageable pageable) {
    log.debug("Finding products by server: {}, activeOnly: {}", server, activeOnly);

    if (!StringUtils.hasText(server)) {
      return activeOnly ? findActiveProducts(pageable) : findAllProducts(pageable);
    }

    if (activeOnly) {
      return productRepository.findByServerAndActive(server, true, pageable);
    } else {
      return productRepository.findAll(pageable); // Apply server filter manually
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Product> findProductsByPriceRange(
      BigDecimal minPrice, BigDecimal maxPrice, boolean activeOnly, Pageable pageable) {
    log.debug(
        "Finding products by price range: {} to {}, activeOnly: {}",
        minPrice,
        maxPrice,
        activeOnly);

    if (minPrice == null && maxPrice == null) {
      return activeOnly ? findActiveProducts(pageable) : findAllProducts(pageable);
    }

    BigDecimal min = minPrice != null ? minPrice : BigDecimal.ZERO;
    BigDecimal max = maxPrice != null ? maxPrice : new BigDecimal("999999999.99");

    if (activeOnly) {
      return productRepository.findActiveByPriceBetween(min, max, pageable);
    } else {
      // For non-active search, use manual pagination
      List<Product> allProducts = productRepository.findByPriceBetween(min, max);
      return convertListToPage(allProducts, pageable);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Product> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
    log.debug("Searching products with criteria: {}", criteria);

    List<UUID> categoryIds = null;
    if (criteria.getCategoryId() != null) {
      if (criteria.isIncludeSubcategories()) {
        categoryIds = getAllCategoryIds(criteria.getCategoryId());
      } else {
        categoryIds = Collections.singletonList(criteria.getCategoryId());
      }
    }

    UUID categoryId = categoryIds != null && !categoryIds.isEmpty() ? categoryIds.get(0) : null;

    return productRepository.searchProducts(
        criteria.getName(),
        categoryId,
        criteria.getServer(),
        criteria.getMinPrice(),
        criteria.getMaxPrice(),
        criteria.getActiveOnly() != null ? criteria.getActiveOnly() : true,
        pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getAvailableServers() {
    log.debug("Getting available servers");
    return productRepository.findDistinctServers();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByName(String name) {
    return productRepository.existsByName(name);
  }

  // ========== Stock & Inventory Operations ==========

  @Override
  @Transactional(readOnly = true)
  public Page<Product> findProductsWithAvailableStock(Pageable pageable) {
    log.debug("Finding products with available stock");
    List<Product> products = productRepository.findProductsWithAvailableStock();
    // Convert to Page manually for pagination support
    return convertListToPage(products, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Product> findProductsWithAvailableStockByCategory(
      UUID categoryId, boolean includeSubcategories, Pageable pageable) {
    log.debug(
        "Finding products with available stock by category ID: {}, includeSubcategories: {}",
        categoryId,
        includeSubcategories);

    if (!includeSubcategories) {
      return productRepository.findProductsWithAvailableStockByCategoryId(categoryId, pageable);
    } else {
      // For hierarchical search, we need to implement custom logic
      List<UUID> categoryIds = getAllCategoryIds(categoryId);
      List<Product> allProducts = productRepository.findProductsWithAvailableStock();

      List<Product> filteredProducts =
          allProducts.stream()
              .filter(product -> categoryIds.contains(product.getCategory().getId()))
              .collect(Collectors.toList());

      return convertListToPage(filteredProducts, pageable);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<Product> findProductsWithLowStock() {
    log.debug("Finding products with low stock");
    return productRepository.findProductsWithLowStock();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Product> findOutOfStockProducts() {
    log.debug("Finding out of stock products");
    return productRepository.findOutOfStockProducts();
  }

  @Override
  @Transactional(readOnly = true)
  public ProductStockInfo getProductStockInfo(UUID productId) {
    log.debug("Getting stock info for product ID: {}", productId);

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product not found with ID: " + productId));

    // Use direct counting queries to avoid casting issues
    long totalStock = stockRepository.countByProduct(product);
    long availableStock = stockRepository.countAvailableByProduct(product);
    long soldStock = stockRepository.countByProductAndSoldTrue(product);
    long reservedStock = stockRepository.countReservedByProduct(product);

    boolean inStock = availableStock > 0;
    boolean lowStock =
        product.getLowStockThreshold() != null && availableStock <= product.getLowStockThreshold();

    return new ProductStockInfo(
        productId, totalStock, availableStock, soldStock, reservedStock, inStock, lowStock);
  }

  // ========== Write Operations ==========

  @Override
  @Transactional
  public Product createProduct(
      String name,
      String description,
      BigDecimal price,
      UUID categoryId,
      String server,
      String imageUrl) {
    return createProduct(name, description, price, categoryId, server, imageUrl, 0, 5);
  }

  @Override
  @Transactional
  public Product createProduct(
      String name,
      String description,
      BigDecimal price,
      UUID categoryId,
      String server,
      String imageUrl,
      Integer sortOrder,
      Integer lowStockThreshold) {
    log.info("Creating product with name: {}, categoryId: {}", name, categoryId);

    validateProductData(name, price, categoryId);

    if (productRepository.existsByName(name)) {
      throw new ResourceAlreadyExistsException("Product name already exists: " + name);
    }

    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

    Product product = new Product(name, description, price, category);
    product.setServer(server);
    product.setImageUrl(imageUrl);
    product.setSortOrder(sortOrder != null ? sortOrder : 0);
    product.setLowStockThreshold(lowStockThreshold != null ? lowStockThreshold : 5);
    product.setActive(true);

    Product savedProduct = productRepository.save(product);
    log.info("Successfully created product with ID: {}", savedProduct.getId());

    return savedProduct;
  }

  @Override
  @Transactional
  public Product updateProduct(
      UUID productId,
      String name,
      String description,
      BigDecimal price,
      UUID categoryId,
      String server,
      String imageUrl) {
    log.info("Updating product ID: {}", productId);

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product not found with ID: " + productId));

    if (StringUtils.hasText(name) && !name.equals(product.getName())) {
      if (productRepository.existsByName(name)) {
        throw new ResourceAlreadyExistsException("Product name already exists: " + name);
      }
      product.setName(name);
    }

    if (description != null) {
      product.setDescription(description);
    }

    if (price != null) {
      validatePrice(price);
      product.setPrice(price);
    }

    if (categoryId != null && !categoryId.equals(product.getCategory().getId())) {
      Category category =
          categoryRepository
              .findById(categoryId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
      product.setCategory(category);
    }

    if (server != null) {
      product.setServer(server);
    }

    if (imageUrl != null) {
      product.setImageUrl(imageUrl);
    }

    Product updatedProduct = productRepository.save(product);
    log.info("Successfully updated product ID: {}", productId);

    return updatedProduct;
  }

  @Override
  @Transactional
  public Product updateProductSettings(
      UUID productId, Integer sortOrder, Integer lowStockThreshold) {
    log.info("Updating product settings for ID: {}", productId);

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product not found with ID: " + productId));

    if (sortOrder != null) {
      product.setSortOrder(sortOrder);
    }

    if (lowStockThreshold != null) {
      product.setLowStockThreshold(lowStockThreshold);
    }

    Product updatedProduct = productRepository.save(product);
    log.info("Successfully updated product settings for ID: {}", productId);

    return updatedProduct;
  }

  @Override
  @Transactional
  public Product moveProductToCategory(UUID productId, UUID newCategoryId) {
    log.info("Moving product ID: {} to category ID: {}", productId, newCategoryId);

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product not found with ID: " + productId));

    Category newCategory =
        categoryRepository
            .findById(newCategoryId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Category not found with ID: " + newCategoryId));

    product.setCategory(newCategory);
    Product movedProduct = productRepository.save(product);

    log.info("Successfully moved product ID: {} to category ID: {}", productId, newCategoryId);
    return movedProduct;
  }

  @Override
  @Transactional
  public Product setProductActive(UUID productId, boolean active) {
    log.info("Setting product ID: {} active status to: {}", productId, active);

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product not found with ID: " + productId));

    product.setActive(active);
    Product updatedProduct = productRepository.save(product);

    log.info("Successfully set product ID: {} active status to: {}", productId, active);
    return updatedProduct;
  }

  @Override
  @Transactional
  public void deleteProduct(UUID productId) {
    log.info("Deleting product ID: {}", productId);

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product not found with ID: " + productId));

    if (!canDeleteProduct(productId)) {
      throw new IllegalStateException(
          "Cannot delete product that has stock items or is referenced in orders");
    }

    productRepository.delete(product);
    log.info("Successfully deleted product ID: {}", productId);
  }

  @Override
  @Transactional
  public void forceDeleteProduct(UUID productId) {
    log.info("Force deleting product ID: {}", productId);

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product not found with ID: " + productId));

    // Force delete will cascade to stock items
    productRepository.delete(product);
    log.info("Successfully force deleted product ID: {}", productId);
  }

  // ========== Validation & Utility Methods ==========

  @Override
  @Transactional(readOnly = true)
  public boolean canDeleteProduct(UUID productId) {
    Product product = productRepository.findById(productId).orElse(null);
    if (product == null) {
      return false;
    }

    // Check if product has stock items
    return product.getStockItems().isEmpty();
  }

  @Override
  @Transactional
  public void reorderProducts(UUID categoryId, List<UUID> productIds) {
    log.info("Reordering {} products in category ID: {}", productIds.size(), categoryId);

    List<Product> products =
        productIds.stream()
            .map(
                id ->
                    productRepository
                        .findById(id)
                        .orElseThrow(
                            () ->
                                new ResourceNotFoundException("Product not found with ID: " + id)))
            .collect(Collectors.toList());

    // Validate all products belong to the same category
    for (Product product : products) {
      if (!product.getCategory().getId().equals(categoryId)) {
        throw new IllegalArgumentException("All products must belong to the specified category");
      }
    }

    // Update sort orders
    for (int i = 0; i < products.size(); i++) {
      products.get(i).setSortOrder(i);
    }

    productRepository.saveAll(products);
    log.info("Successfully reordered {} products", products.size());
  }

  @Override
  @Transactional
  public int bulkUpdateProductsActive(List<UUID> productIds, boolean active) {
    log.info("Bulk updating {} products active status to: {}", productIds.size(), active);

    List<Product> products = productRepository.findAllById(productIds);

    products.forEach(product -> product.setActive(active));
    productRepository.saveAll(products);

    log.info("Successfully bulk updated {} products active status", products.size());
    return products.size();
  }

  // ========== Private Helper Methods ==========

  private void validateProductData(String name, BigDecimal price, UUID categoryId) {
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("Product name cannot be blank");
    }
    if (name.length() > 200) {
      throw new IllegalArgumentException("Product name cannot exceed 200 characters");
    }

    validatePrice(price);

    if (categoryId == null) {
      throw new IllegalArgumentException("Category ID cannot be null");
    }
  }

  private void validatePrice(BigDecimal price) {
    if (price == null) {
      throw new IllegalArgumentException("Product price cannot be null");
    }
    if (price.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Product price must be greater than 0");
    }
  }

  private List<UUID> getAllCategoryIds(UUID categoryId) {
    List<UUID> categoryIds = new ArrayList<>();
    categoryIds.add(categoryId);

    // Get all descendant categories
    Category category = categoryRepository.findById(categoryId).orElse(null);
    if (category != null) {
      collectDescendantCategoryIds(category, categoryIds);
    }

    return categoryIds;
  }

  private void collectDescendantCategoryIds(Category category, List<UUID> categoryIds) {
    List<Category> subcategories = categoryRepository.findByParentCategoryId(category.getId());
    for (Category subcategory : subcategories) {
      categoryIds.add(subcategory.getId());
      collectDescendantCategoryIds(subcategory, categoryIds);
    }
  }

  private Page<Product> convertListToPage(List<Product> products, Pageable pageable) {
    // Simple implementation - in production, consider using PageImpl
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), products.size());

    List<Product> pageContent =
        start < products.size() ? products.subList(start, end) : Collections.emptyList();

    return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, products.size());
  }
}
