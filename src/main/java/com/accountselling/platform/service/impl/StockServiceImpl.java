package com.accountselling.platform.service.impl;

import com.accountselling.platform.exception.*;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.repository.ProductRepository;
import com.accountselling.platform.repository.StockRepository;
import com.accountselling.platform.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of StockService for inventory management operations.
 * Provides comprehensive stock management including reservations, sales tracking,
 * and low stock notifications with concurrent access protection.
 * 
 * Implementation of StockService for inventory management operations.
 * Supports comprehensive stock management including reservations, sales tracking,
 * and low stock notifications with concurrent access protection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    
    // Default value for product reservation duration (minutes)
    private static final int DEFAULT_RESERVATION_MINUTES = 15;
    
    // Default value for low stock threshold
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    // ==================== BASIC STOCK OPERATIONS ====================

    @Override
    @Transactional
    public Stock createStock(UUID productId, String credentials, String additionalInfo) {
        log.info("Creating new stock for product ID: {}", productId);
        
        // Check if product exists
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> {
                log.error("Product not found with ID: {}", productId);
                return new ResourceNotFoundException("Product not found with ID: " + productId);
            });

        // Check if credentials already exist
        if (stockRepository.existsByProductIdAndCredentials(productId, credentials)) {
            log.error("Duplicate credentials for product ID: {}", productId);
            throw new ResourceAlreadyExistsException("Credentials already exist for this product");
        }

        // Create new stock
        Stock stock = new Stock(product, credentials, additionalInfo);
        Stock savedStock = stockRepository.save(stock);
        
        log.info("Successfully created stock ID: {} for product: {}", 
                savedStock.getId(), product.getName());
        
        return savedStock;
    }

    @Override
    @Transactional
    public List<Stock> createBulkStock(UUID productId, List<String> credentialsList) {
        log.info("Creating {} stock items for product ID: {}", 
                credentialsList.size(), productId);
        
        // Check if product exists
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> {
                log.error("Product not found with ID: {}", productId);
                return new ResourceNotFoundException("Product not found with ID: " + productId);
            });

        List<Stock> stockItems = new ArrayList<>();
        
        for (String credentials : credentialsList) {
            // Skip duplicate credentials
            if (!stockRepository.existsByProductIdAndCredentials(productId, credentials)) {
                stockItems.add(new Stock(product, credentials));
            } else {
                log.warn("Skipping duplicate credentials for product ID: {}", productId);
            }
        }
        
        List<Stock> savedStocks = stockRepository.saveAll(stockItems);
        
        log.info("Successfully created {} stock items for product: {}", 
                savedStocks.size(), product.getName());
        
        return savedStocks;
    }

    @Override
    public Page<Stock> getStockByProduct(UUID productId, Pageable pageable) {
        log.debug("Getting stock for product ID: {}", productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        return stockRepository.findByProduct(product, pageable);
    }

    @Override
    public Optional<Stock> getStockById(UUID stockId) {
        log.debug("Getting stock by ID: {}", stockId);
        return stockRepository.findById(stockId);
    }

    @Override
    @Transactional
    public Stock updateStockAdditionalInfo(UUID stockId, String additionalInfo) {
        log.info("Updating additional info for stock ID: {}", stockId);
        
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> {
                log.error("Stock not found with ID: {}", stockId);
                return new ResourceNotFoundException("Stock not found with ID: " + stockId);
            });
        
        stock.setAdditionalInfo(additionalInfo);
        Stock savedStock = stockRepository.save(stock);
        
        log.info("Successfully updated additional info for stock ID: {}", stockId);
        
        return savedStock;
    }

    @Override
    @Transactional
    public void deleteStock(UUID stockId) {
        log.info("Deleting stock ID: {}", stockId);
        
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> {
                log.error("Stock not found with ID: {}", stockId);
                return new ResourceNotFoundException("Stock not found with ID: " + stockId);
            });
        
        // Check if stock is already sold or reserved
        if (stock.getSold()) {
            log.error("Cannot delete sold stock ID: {}", stockId);
            throw new StockException("Cannot delete sold stock item");
        }
        
        if (stock.isReserved()) {
            log.error("Cannot delete reserved stock ID: {}", stockId);
            throw new StockException("Cannot delete reserved stock item");
        }
        
        stockRepository.delete(stock);
        
        log.info("Successfully deleted stock ID: {}", stockId);
    }

    // ==================== STOCK AVAILABILITY OPERATIONS ====================

    @Override
    public long getAvailableStockCount(UUID productId) {
        log.debug("Getting available stock count for product ID: {}", productId);
        return stockRepository.countAvailableByProductId(productId);
    }

    @Override
    public long getTotalStockCount(UUID productId) {
        log.debug("Getting total stock count for product ID: {}", productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        return stockRepository.countByProduct(product);
    }

    @Override
    public List<Stock> getAvailableStock(UUID productId) {
        log.debug("Getting available stock list for product ID: {}", productId);
        return stockRepository.findAvailableStockByProductId(productId);
    }

    @Override
    public Optional<Stock> getFirstAvailableStock(UUID productId) {
        log.debug("Getting first available stock for product ID: {}", productId);
        return stockRepository.findFirstAvailableByProductId(productId);
    }

    @Override
    public boolean isInStock(UUID productId) {
        long availableCount = getAvailableStockCount(productId);
        log.debug("Stock check: product ID {} has {} items in stock", 
                productId, availableCount);
        return availableCount > 0;
    }

    @Override
    public boolean isOutOfStock(UUID productId) {
        return !isInStock(productId);
    }

    // ==================== STOCK RESERVATION OPERATIONS ====================

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<Stock> reserveStock(UUID productId, int quantity, int reservationDurationMinutes) {
        log.info("Reserving {} stock items for product ID: {} for {} minutes", 
                quantity, productId, reservationDurationMinutes);
        
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(reservationDurationMinutes);
        return reserveStockUntil(productId, quantity, reservedUntil);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<Stock> reserveStockUntil(UUID productId, int quantity, LocalDateTime reservedUntil) {
        log.info("Reserving {} stock items for product ID: {} until {}", 
                quantity, productId, reservedUntil);
        
        // Check if product exists
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> {
                log.error("Product not found with ID: {}", productId);
                return new ResourceNotFoundException("Product not found with ID: " + productId);
            });

        // Clean up expired reservations first
        cleanupExpiredReservations();
        
        // Get available stock
        List<Stock> availableStock = stockRepository.findAvailableStockByProductId(productId);
        
        if (availableStock.size() < quantity) {
            log.error("Insufficient stock: need {} have {} items", 
                    quantity, availableStock.size());
            throw new OutOfStockException("Insufficient stock available. Required: " + quantity + ", Available: " + availableStock.size());
        }
        
        // Reserve stock according to requested quantity
        List<Stock> reservedStock = new ArrayList<>();
        for (int i = 0; i < quantity && i < availableStock.size(); i++) {
            Stock stock = availableStock.get(i);
            try {
                stock.reserveUntil(reservedUntil);
                Stock savedStock = stockRepository.save(stock);
                reservedStock.add(savedStock);
                
                log.debug("Successfully reserved stock ID: {}", stock.getId());
            } catch (IllegalStateException e) {
                log.error("Cannot reserve stock ID: {}", stock.getId(), e);
                throw new StockReservationException("Failed to reserve stock item: " + stock.getId(), e);
            }
        }
        
        log.info("Successfully reserved {} stock items", reservedStock.size());
        
        return reservedStock;
    }

    @Override
    @Transactional
    public int releaseReservation(List<UUID> stockIds) {
        log.info("Releasing reservation for {} stock items", stockIds.size());
        
        int releasedCount = stockRepository.releaseReservations(stockIds);
        
        log.info("Successfully released {} reservations", releasedCount);
        
        return releasedCount;
    }

    @Override
    @Transactional
    public boolean releaseReservation(UUID stockId) {
        log.info("Releasing reservation for stock ID: {}", stockId);
        
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> {
                log.error("Stock not found with ID: {}", stockId);
                return new ResourceNotFoundException("Stock not found with ID: " + stockId);
            });
        
        if (!stock.isReserved()) {
            log.warn("Stock ID: {} is not reserved", stockId);
            return false;
        }
        
        stock.releaseReservation();
        stockRepository.save(stock);
        
        log.info("Successfully released reservation for stock ID: {}", stockId);
        
        return true;
    }

    @Override
    public List<Stock> getReservedStock(UUID productId) {
        log.debug("Getting reserved stock for product ID: {}", productId);
        
        if (productId != null) {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            return stockRepository.findReservedStockByProduct(product);
        }
        
        return stockRepository.findReservedStock();
    }

    @Override
    public long getReservedStockCount(UUID productId) {
        log.debug("Getting reserved stock count for product ID: {}", productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        return stockRepository.countReservedByProduct(product);
    }

    // ==================== STOCK SALES OPERATIONS ====================

    @Override
    @Transactional
    public Stock markAsSold(UUID stockId) {
        log.info("Marking stock as sold ID: {}", stockId);
        
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> {
                log.error("Stock not found with ID: {}", stockId);
                return new ResourceNotFoundException("Stock not found with ID: " + stockId);
            });
        
        try {
            stock.markAsSold();
            Stock savedStock = stockRepository.save(stock);
            
            log.info("Successfully marked stock as sold ID: {}", stockId);
            
            return savedStock;
        } catch (IllegalStateException e) {
            log.error("Cannot mark stock as sold ID: {}", stockId, e);
            throw new StockException("Cannot mark stock as sold: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public List<Stock> markAsSold(List<UUID> stockIds) {
        log.info("Marking {} stock items as sold", stockIds.size());
        
        List<Stock> soldStock = new ArrayList<>();
        
        for (UUID stockId : stockIds) {
            try {
                Stock stock = markAsSold(stockId);
                soldStock.add(stock);
            } catch (Exception e) {
                log.error("Cannot mark stock as sold ID: {}", stockId, e);
                // Skip failed items and continue
            }
        }
        
        log.info("Successfully marked {} stock items as sold", soldStock.size());
        
        return soldStock;
    }

    @Override
    public List<Stock> getSoldStock(UUID productId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting sold stock for product ID: {}", productId);
        
        if (productId != null && startDate != null && endDate != null) {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            return stockRepository.findSoldByProductBetweenDates(product, startDate, endDate);
        } else if (startDate != null && endDate != null) {
            return stockRepository.findSoldBetweenDates(startDate, endDate);
        } else if (productId != null) {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            return stockRepository.findByProductAndSoldTrue(product);
        }
        
        return stockRepository.findBySoldTrue();
    }

    @Override
    public long getSoldStockCount(UUID productId) {
        log.debug("Getting sold stock count for product ID: {}", productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        return stockRepository.countByProductAndSoldTrue(product);
    }

    // ==================== INVENTORY MANAGEMENT OPERATIONS ====================

    @Override
    public StockStatistics getStockStatistics(UUID productId) {
        log.debug("Getting stock statistics for product ID: {}", productId);
        
        Object[] stats = stockRepository.getStockStatisticsByProductId(productId);
        
        if (stats != null && stats.length == 4) {
            long total = ((Number) stats[0]).longValue();
            long available = ((Number) stats[1]).longValue();
            long sold = ((Number) stats[2]).longValue();
            long reserved = ((Number) stats[3]).longValue();
            
            return new StockStatistics(total, available, sold, reserved);
        }
        
        return new StockStatistics(0, 0, 0, 0);
    }

    @Override
    public List<Product> getProductsWithLowStock(Integer threshold) {
        log.debug("Getting products with low stock using threshold: {}", threshold);
        
        long searchThreshold = threshold != null ? threshold : DEFAULT_LOW_STOCK_THRESHOLD;
        
        List<Product> lowStockProducts = stockRepository.findProductsWithLowStock(searchThreshold);
        
        log.info("Found {} products with low stock", lowStockProducts.size());
        
        return lowStockProducts;
    }

    @Override
    public boolean isLowStock(UUID productId) {
        log.debug("Checking low stock for product ID: {}", productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        long availableCount = getAvailableStockCount(productId);
        int threshold = product.getLowStockThreshold() != null ? product.getLowStockThreshold() : DEFAULT_LOW_STOCK_THRESHOLD;
        
        boolean isLow = availableCount <= threshold;
        
        log.debug("Product ID: {} has {} stock items, threshold {}, low stock: {}", 
                productId, availableCount, threshold, isLow);
        
        return isLow;
    }

    @Override
    @Transactional
    public int cleanupExpiredReservations() {
        log.debug("Cleaning up expired reservations");
        
        int cleanedCount = stockRepository.clearExpiredReservations();
        
        if (cleanedCount > 0) {
            log.info("Successfully cleaned up {} expired reservations", cleanedCount);
        }
        
        return cleanedCount;
    }

    @Override
    public List<Stock> getExpiredReservations() {
        log.debug("Getting expired reservations");
        return stockRepository.findExpiredReservations();
    }

    @Override
    public List<Stock> getReservationsExpiringSoon(int withinMinutes) {
        log.debug("Getting reservations expiring within {} minutes", withinMinutes);
        
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(withinMinutes);
        List<Stock> expiringSoon = stockRepository.findReservationsExpiringSoon(threshold);
        
        log.debug("Found {} reservations expiring soon", expiringSoon.size());
        
        return expiringSoon;
    }

    // ==================== UTILITY OPERATIONS ====================

    @Override
    public List<String> findDuplicateCredentials(UUID productId) {
        log.debug("Finding duplicate credentials for product ID: {}", productId);
        
        List<String> duplicates = stockRepository.findDuplicateCredentialsByProductId(productId);
        
        if (!duplicates.isEmpty()) {
            log.warn("Found {} duplicate credentials for product ID: {}", 
                    duplicates.size(), productId);
        }
        
        return duplicates;
    }

    @Override
    public boolean credentialsExist(UUID productId, String credentials) {
        log.debug("Checking credentials existence for product ID: {}", productId);
        return stockRepository.existsByProductIdAndCredentials(productId, credentials);
    }

    @Override
    @Transactional
    public void updateLowStockThreshold(UUID productId, int threshold) {
        log.info("Updating low stock threshold to {} for product ID: {}", 
                threshold, productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> {
                log.error("Product not found with ID: {}", productId);
                return new ResourceNotFoundException("Product not found with ID: " + productId);
            });
        
        product.setLowStockThreshold(threshold);
        productRepository.save(product);
        
        log.info("Successfully updated low stock threshold for product: {}", 
                product.getName());
    }

    /**
     * Inner class to hold stock statistics data
     */
    public static class StockStatistics {
        private final long total;
        private final long available;
        private final long sold;
        private final long reserved;

        public StockStatistics(long total, long available, long sold, long reserved) {
            this.total = total;
            this.available = available;
            this.sold = sold;
            this.reserved = reserved;
        }

        public long getTotal() { return total; }
        public long getAvailable() { return available; }
        public long getSold() { return sold; }
        public long getReserved() { return reserved; }
    }
}