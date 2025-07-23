package com.accountselling.platform.service;

import com.accountselling.platform.dto.stock.StockStatistics;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for stock inventory management operations.
 * Handles stock availability, reservations, sales tracking, and notifications.
 * 
 * Interface for stock inventory management
 * Supports availability checking, product reservation, sales tracking, and notifications
 */
public interface StockService {

    // ==================== BASIC STOCK OPERATIONS ====================

    /**
     * Create new stock item for a product
     * 
     * @param productId ID of the product
     * @param credentials encrypted account credentials
     * @param additionalInfo additional information (if any)
     * @return Stock the created stock item
     * @throws ResourceNotFoundException if product is not found
     * @throws ResourceAlreadyExistsException if credentials already exist
     */
    Stock createStock(UUID productId, String credentials, String additionalInfo);

    /**
     * Create multiple stock items for a product in bulk
     * 
     * @param productId ID of the product
     * @param credentialsList list of encrypted account credentials
     * @return List<Stock> list of created stock items
     * @throws ResourceNotFoundException if product is not found
     */
    List<Stock> createBulkStock(UUID productId, List<String> credentialsList);

    /**
     * Get all stock items for a product
     * 
     * @param productId ID of the product
     * @param pageable pagination parameters
     * @return Page<Stock> paginated list of stock items
     */
    Page<Stock> getStockByProduct(UUID productId, Pageable pageable);

    /**
     * Get stock item by ID
     * 
     * @param stockId ID of the stock
     * @return Optional<Stock> stock item or empty if not found
     */
    Optional<Stock> getStockById(UUID stockId);

    /**
     * Update additional information of stock item
     * 
     * @param stockId ID of the stock
     * @param additionalInfo new additional information
     * @return Stock updated stock item
     * @throws ResourceNotFoundException if stock is not found
     */
    Stock updateStockAdditionalInfo(UUID stockId, String additionalInfo);

    /**
     * Delete stock item (only if not sold)
     * 
     * @param stockId ID of the stock
     * @throws ResourceNotFoundException if stock is not found
     * @throws StockException if stock is already sold or reserved
     */
    void deleteStock(UUID stockId);

    // ==================== STOCK AVAILABILITY OPERATIONS ====================

    /**
     * Get available stock count for a product
     * 
     * @param productId ID of the product
     * @return long number of available stock items
     */
    long getAvailableStockCount(UUID productId);

    /**
     * Get total stock count for a product
     * 
     * @param productId ID of the product
     * @return long total number of stock items
     */
    long getTotalStockCount(UUID productId);

    /**
     * Get available stock items for a product
     * 
     * @param productId ID of the product
     * @return List<Stock> list of available stock items
     */
    List<Stock> getAvailableStock(UUID productId);

    /**
     * Get first available stock item for a product (FIFO)
     * 
     * @param productId ID of the product
     * @return Optional<Stock> first available stock item or empty if none available
     */
    Optional<Stock> getFirstAvailableStock(UUID productId);

    /**
     * Check if product has available stock
     * 
     * @param productId ID of the product
     * @return boolean true if stock is available, false if not
     */
    boolean isInStock(UUID productId);

    /**
     * Check if product is out of stock
     * 
     * @param productId ID of the product
     * @return boolean true if out of stock, false if stock is available
     */
    boolean isOutOfStock(UUID productId);

    // ==================== STOCK RESERVATION OPERATIONS ====================

    /**
     * Reserve stock for purchase (temporary reservation)
     * 
     * @param productId ID of the product
     * @param quantity number of items to reserve
     * @param reservationDurationMinutes reservation duration in minutes
     * @return List<Stock> list of reserved stock items
     * @throws OutOfStockException หากสต็อกไม่เพียงพอ
     * @throws StockReservationException หากจองไม่สำเร็จ
     */
    List<Stock> reserveStock(UUID productId, int quantity, int reservationDurationMinutes);

    /**
     * Reserve stock until specific time
     * 
     * @param productId ID of the product
     * @param quantity number of items to reserve
     * @param reservedUntil reservation end time
     * @return List<Stock> list of reserved stock items
     * @throws OutOfStockException หากสต็อกไม่เพียงพอ
     * @throws StockReservationException หากจองไม่สำเร็จ
     */
    List<Stock> reserveStockUntil(UUID productId, int quantity, LocalDateTime reservedUntil);

    /**
     * Release stock reservation
     * 
     * @param stockIds list of stock IDs to cancel reservation
     * @return int number of reservations released
     */
    int releaseReservation(List<UUID> stockIds);

    /**
     * Release single stock reservation
     * 
     * @param stockId ID of the stock to cancel reservation
     * @return boolean true if release successful, false if unsuccessful
     * @throws ResourceNotFoundException if stock is not found
     */
    boolean releaseReservation(UUID stockId);

    /**
     * Get currently reserved stock items
     * 
     * @param productId ID of the product (null for all products)
     * @return List<Stock> list of currently reserved stock items
     */
    List<Stock> getReservedStock(UUID productId);

    /**
     * Get reserved stock count for a product
     * 
     * @param productId ID of the product
     * @return long number of reserved stock items
     */
    long getReservedStockCount(UUID productId);

    // ==================== STOCK SALES OPERATIONS ====================

    /**
     * Mark stock as sold
     * 
     * @param stockId ID of the stock
     * @return Stock stock item marked as sold
     * @throws ResourceNotFoundException if stock is not found
     * @throws StockException if stock is already sold
     */
    Stock markAsSold(UUID stockId);

    /**
     * Mark multiple stock items as sold
     * 
     * @param stockIds list of stock IDs to mark as sold
     * @return List<Stock> list of stock items marked as sold
     */
    List<Stock> markAsSold(List<UUID> stockIds);

    /**
     * Get sold stock items
     * 
     * @param productId ID of the product (null for all products)
     * @param startDate start date (null for no limit)
     * @param endDate end date (null for no limit)
     * @return List<Stock> list of sold stock items
     */
    List<Stock> getSoldStock(UUID productId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get sold stock count for a product
     * 
     * @param productId ID of the product
     * @return long number of sold stock items
     */
    long getSoldStockCount(UUID productId);

    // ==================== INVENTORY MANAGEMENT OPERATIONS ====================

    /**
     * Get stock statistics for a product
     * 
     * @param productId ID of the product
     * @return StockStatistics stock statistics
     */
    StockStatistics getStockStatistics(UUID productId);

    /**
     * Get products with low stock
     * 
     * @param threshold low stock threshold (null to use product's value)
     * @return List<Product> list of products with low stock
     */
    List<Product> getProductsWithLowStock(Integer threshold);

    /**
     * Check if product has low stock
     * 
     * @param productId ID of the product
     * @return boolean true if stock is low, false if stock is normal
     */
    boolean isLowStock(UUID productId);

    /**
     * Clean up expired reservations
     * 
     * @return int number of reservations cleaned up
     */
    int cleanupExpiredReservations();

    /**
     * Get expired reservations
     * 
     * @return List<Stock> list of stock items with expired reservations
     */
    List<Stock> getExpiredReservations();

    /**
     * Get reservations expiring soon
     * 
     * @param withinMinutes time frame considered as "expiring soon" in minutes
     * @return List<Stock> list of stock items with reservations expiring soon
     */
    List<Stock> getReservationsExpiringSoon(int withinMinutes);

    // ==================== UTILITY OPERATIONS ====================

    /**
     * Check for duplicate credentials in product stock
     * 
     * @param productId ID of the product
     * @return List<String> list of duplicate credentials
     */
    List<String> findDuplicateCredentials(UUID productId);

    /**
     * Check if credentials already exist for a product
     * 
     * @param productId ID of the product
     * @param credentials credentials to check
     * @return boolean true if already exists, false if not
     */
    boolean credentialsExist(UUID productId, String credentials);

    /**
     * Update low stock threshold for a product
     * 
     * @param productId ID of the product
     * @param threshold new low stock threshold
     * @throws ResourceNotFoundException if product is not found
     */
    void updateLowStockThreshold(UUID productId, int threshold);

    // ==================== LOW STOCK NOTIFICATION OPERATIONS ====================

    /**
     * Check and notify administrators about low stock products
     * This method should be called periodically or after stock changes
     * 
     * @return List<Product> products that have low stock and need attention
     */
    List<Product> checkAndNotifyLowStock();

    /**
     * Check if a specific product needs low stock notification after stock change
     * This method is called after stock operations to immediately check if notification is needed
     * 
     * @param productId ID of the product to check
     * @return boolean true if notification was sent, false if stock is normal
     */
    boolean checkAndNotifyLowStockForProduct(UUID productId);
}