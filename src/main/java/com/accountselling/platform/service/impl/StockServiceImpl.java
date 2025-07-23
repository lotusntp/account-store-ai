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
 * การ implement StockService สำหรับการจัดการสต็อกสินค้า
 * รองรับการจัดการสต็อกครบครัน รวมถึงการจอง การติดตามการขาย 
 * และการแจ้งเตือนสต็อกต่ำ พร้อมป้องกัน concurrent access
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    
    // ค่าเริ่มต้นสำหรับระยะเวลาการจองสินค้า (นาที)
    private static final int DEFAULT_RESERVATION_MINUTES = 15;
    
    // ค่าเริ่นต้นสำหรับเกณฑ์สต็อกต่ำ
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    // ==================== BASIC STOCK OPERATIONS ====================

    @Override
    @Transactional
    public Stock createStock(UUID productId, String credentials, String additionalInfo) {
        log.info("สร้างสต็อกใหม่สำหรับสินค้า ID: {} - Creating new stock for product ID: {}", productId, productId);
        
        // ตรวจสอบว่าสินค้ามีอยู่หรือไม่
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> {
                log.error("ไม่พบสินค้า ID: {} - Product not found with ID: {}", productId, productId);
                return new ResourceNotFoundException("Product not found with ID: " + productId);
            });

        // ตรวจสอบว่าข้อมูลบัญชีซ้ำหรือไม่
        if (stockRepository.existsByProductIdAndCredentials(productId, credentials)) {
            log.error("ข้อมูลบัญชีซ้ำสำหรับสินค้า ID: {} - Duplicate credentials for product ID: {}", productId, productId);
            throw new ResourceAlreadyExistsException("Credentials already exist for this product");
        }

        // สร้างสต็อกใหม่
        Stock stock = new Stock(product, credentials, additionalInfo);
        Stock savedStock = stockRepository.save(stock);
        
        log.info("สร้างสต็อกสำเร็จ ID: {} สำหรับสินค้า: {} - Successfully created stock ID: {} for product: {}", 
                savedStock.getId(), product.getName(), savedStock.getId(), product.getName());
        
        return savedStock;
    }

    @Override
    @Transactional
    public List<Stock> createBulkStock(UUID productId, List<String> credentialsList) {
        log.info("สร้างสต็อกจำนวน {} รายการสำหรับสินค้า ID: {} - Creating {} stock items for product ID: {}", 
                credentialsList.size(), productId, credentialsList.size(), productId);
        
        // ตรวจสอบว่าสินค้ามีอยู่หรือไม่
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> {
                log.error("ไม่พบสินค้า ID: {} - Product not found with ID: {}", productId, productId);
                return new ResourceNotFoundException("Product not found with ID: " + productId);
            });

        List<Stock> stockItems = new ArrayList<>();
        
        for (String credentials : credentialsList) {
            // ข้ามข้อมูลบัญชีที่ซ้ำ
            if (!stockRepository.existsByProductIdAndCredentials(productId, credentials)) {
                stockItems.add(new Stock(product, credentials));
            } else {
                log.warn("ข้าม credentials ที่ซ้ำสำหรับสินค้า ID: {} - Skipping duplicate credentials for product ID: {}", productId, productId);
            }
        }
        
        List<Stock> savedStocks = stockRepository.saveAll(stockItems);
        
        log.info("สร้างสต็อกสำเร็จจำนวน {} รายการสำหรับสินค้า: {} - Successfully created {} stock items for product: {}", 
                savedStocks.size(), product.getName(), savedStocks.size(), product.getName());
        
        return savedStocks;
    }

    @Override
    public Page<Stock> getStockByProduct(UUID productId, Pageable pageable) {
        log.debug("ดึงข้อมูลสต็อกสำหรับสินค้า ID: {} - Getting stock for product ID: {}", productId, productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        return stockRepository.findByProduct(product, pageable);
    }

    @Override
    public Optional<Stock> getStockById(UUID stockId) {
        log.debug("ดึงข้อมูลสต็อก ID: {} - Getting stock by ID: {}", stockId, stockId);
        return stockRepository.findById(stockId);
    }

    @Override
    @Transactional
    public Stock updateStockAdditionalInfo(UUID stockId, String additionalInfo) {
        log.info("อัปเดตข้อมูลเพิ่มเติมของสต็อก ID: {} - Updating additional info for stock ID: {}", stockId, stockId);
        
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> {
                log.error("ไม่พบสต็อก ID: {} - Stock not found with ID: {}", stockId, stockId);
                return new ResourceNotFoundException("Stock not found with ID: " + stockId);
            });
        
        stock.setAdditionalInfo(additionalInfo);
        Stock savedStock = stockRepository.save(stock);
        
        log.info("อัปเดตข้อมูลเพิ่มเติมสำเร็จสำหรับสต็อก ID: {} - Successfully updated additional info for stock ID: {}", stockId, stockId);
        
        return savedStock;
    }

    @Override
    @Transactional
    public void deleteStock(UUID stockId) {
        log.info("ลบสต็อก ID: {} - Deleting stock ID: {}", stockId, stockId);
        
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> {
                log.error("ไม่พบสต็อก ID: {} - Stock not found with ID: {}", stockId, stockId);
                return new ResourceNotFoundException("Stock not found with ID: " + stockId);
            });
        
        // ตรวจสอบว่าสต็อกถูกขายแล้วหรือถูกจองอยู่
        if (stock.getSold()) {
            log.error("ไม่สามารถลบสต็อกที่ขายแล้ว ID: {} - Cannot delete sold stock ID: {}", stockId, stockId);
            throw new StockException("Cannot delete sold stock item");
        }
        
        if (stock.isReserved()) {
            log.error("ไม่สามารถลบสต็อกที่ถูกจองอยู่ ID: {} - Cannot delete reserved stock ID: {}", stockId, stockId);
            throw new StockException("Cannot delete reserved stock item");
        }
        
        stockRepository.delete(stock);
        
        log.info("ลบสต็อกสำเร็จ ID: {} - Successfully deleted stock ID: {}", stockId, stockId);
    }

    // ==================== STOCK AVAILABILITY OPERATIONS ====================

    @Override
    public long getAvailableStockCount(UUID productId) {
        log.debug("ตรวจสอบจำนวนสต็อกที่มีจำหน่ายสำหรับสินค้า ID: {} - Getting available stock count for product ID: {}", productId, productId);
        return stockRepository.countAvailableByProductId(productId);
    }

    @Override
    public long getTotalStockCount(UUID productId) {
        log.debug("ตรวจสอบจำนวนสต็อกทั้งหมดสำหรับสินค้า ID: {} - Getting total stock count for product ID: {}", productId, productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        return stockRepository.countByProduct(product);
    }

    @Override
    public List<Stock> getAvailableStock(UUID productId) {
        log.debug("ดึงรายการสต็อกที่มีจำหน่ายสำหรับสินค้า ID: {} - Getting available stock list for product ID: {}", productId, productId);
        return stockRepository.findAvailableStockByProductId(productId);
    }

    @Override
    public Optional<Stock> getFirstAvailableStock(UUID productId) {
        log.debug("ดึงสต็อกรายการแรกที่มีจำหน่ายสำหรับสินค้า ID: {} - Getting first available stock for product ID: {}", productId, productId);
        return stockRepository.findFirstAvailableByProductId(productId);
    }

    @Override
    public boolean isInStock(UUID productId) {
        long availableCount = getAvailableStockCount(productId);
        log.debug("ตรวจสอบสต็อก สินค้า ID: {} มีสต็อก {} รายการ - Stock check product ID: {} has {} items in stock", 
                productId, availableCount, productId, availableCount);
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
        log.info("จองสต็อก {} รายการสำหรับสินค้า ID: {} เป็นเวลา {} นาที - Reserving {} stock items for product ID: {} for {} minutes", 
                quantity, productId, reservationDurationMinutes, quantity, productId, reservationDurationMinutes);
        
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(reservationDurationMinutes);
        return reserveStockUntil(productId, quantity, reservedUntil);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<Stock> reserveStockUntil(UUID productId, int quantity, LocalDateTime reservedUntil) {
        log.info("จองสต็อก {} รายการสำหรับสินค้า ID: {} จนถึง {} - Reserving {} stock items for product ID: {} until {}", 
                quantity, productId, reservedUntil, quantity, productId, reservedUntil);
        
        // ตรวจสอบว่าสินค้ามีอยู่หรือไม่
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> {
                log.error("ไม่พบสินค้า ID: {} - Product not found with ID: {}", productId, productId);
                return new ResourceNotFoundException("Product not found with ID: " + productId);
            });

        // ล้างการจองที่หมดอายุก่อน
        cleanupExpiredReservations();
        
        // ดึงสต็อกที่มีจำหน่าย
        List<Stock> availableStock = stockRepository.findAvailableStockByProductId(productId);
        
        if (availableStock.size() < quantity) {
            log.error("สต็อกไม่เพียงพอ ต้องการ {} มี {} รายการ - Insufficient stock: need {} have {} items", 
                    quantity, availableStock.size(), quantity, availableStock.size());
            throw new OutOfStockException("Insufficient stock available. Required: " + quantity + ", Available: " + availableStock.size());
        }
        
        // จองสต็อกตามจำนวนที่ต้องการ
        List<Stock> reservedStock = new ArrayList<>();
        for (int i = 0; i < quantity && i < availableStock.size(); i++) {
            Stock stock = availableStock.get(i);
            try {
                stock.reserveUntil(reservedUntil);
                Stock savedStock = stockRepository.save(stock);
                reservedStock.add(savedStock);
                
                log.debug("จองสต็อกสำเร็จ ID: {} - Successfully reserved stock ID: {}", stock.getId(), stock.getId());
            } catch (IllegalStateException e) {
                log.error("ไม่สามารถจองสต็อก ID: {} - Cannot reserve stock ID: {}", stock.getId(), stock.getId(), e);
                throw new StockReservationException("Failed to reserve stock item: " + stock.getId(), e);
            }
        }
        
        log.info("จองสต็อกสำเร็จจำนวน {} รายการ - Successfully reserved {} stock items", reservedStock.size(), reservedStock.size());
        
        return reservedStock;
    }

    @Override
    @Transactional
    public int releaseReservation(List<UUID> stockIds) {
        log.info("ยกเลิกการจองสต็อกจำนวน {} รายการ - Releasing reservation for {} stock items", stockIds.size(), stockIds.size());
        
        int releasedCount = stockRepository.releaseReservations(stockIds);
        
        log.info("ยกเลิกการจองสำเร็จจำนวน {} รายการ - Successfully released {} reservations", releasedCount, releasedCount);
        
        return releasedCount;
    }

    @Override
    @Transactional
    public boolean releaseReservation(UUID stockId) {
        log.info("ยกเลิกการจองสต็อก ID: {} - Releasing reservation for stock ID: {}", stockId, stockId);
        
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> {
                log.error("ไม่พบสต็อก ID: {} - Stock not found with ID: {}", stockId, stockId);
                return new ResourceNotFoundException("Stock not found with ID: " + stockId);
            });
        
        if (!stock.isReserved()) {
            log.warn("สต็อก ID: {} ไม่ได้ถูกจองอยู่ - Stock ID: {} is not reserved", stockId, stockId);
            return false;
        }
        
        stock.releaseReservation();
        stockRepository.save(stock);
        
        log.info("ยกเลิกการจองสำเร็จสำหรับสต็อก ID: {} - Successfully released reservation for stock ID: {}", stockId, stockId);
        
        return true;
    }

    @Override
    public List<Stock> getReservedStock(UUID productId) {
        log.debug("ดึงรายการสต็อกที่ถูกจองสำหรับสินค้า ID: {} - Getting reserved stock for product ID: {}", productId, productId);
        
        if (productId != null) {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            return stockRepository.findReservedStockByProduct(product);
        }
        
        return stockRepository.findReservedStock();
    }

    @Override
    public long getReservedStockCount(UUID productId) {
        log.debug("ตรวจสอบจำนวนสต็อกที่ถูกจองสำหรับสินค้า ID: {} - Getting reserved stock count for product ID: {}", productId, productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        return stockRepository.countReservedByProduct(product);
    }

    // ==================== STOCK SALES OPERATIONS ====================

    @Override
    @Transactional
    public Stock markAsSold(UUID stockId) {
        log.info("ทำเครื่องหมายขายแล้วสำหรับสต็อก ID: {} - Marking stock as sold ID: {}", stockId, stockId);
        
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> {
                log.error("ไม่พบสต็อก ID: {} - Stock not found with ID: {}", stockId, stockId);
                return new ResourceNotFoundException("Stock not found with ID: " + stockId);
            });
        
        try {
            stock.markAsSold();
            Stock savedStock = stockRepository.save(stock);
            
            log.info("ทำเครื่องหมายขายแล้วสำเร็จสำหรับสต็อก ID: {} - Successfully marked stock as sold ID: {}", stockId, stockId);
            
            return savedStock;
        } catch (IllegalStateException e) {
            log.error("ไม่สามารถทำเครื่องหมายขายแล้วสำหรับสต็อก ID: {} - Cannot mark stock as sold ID: {}", stockId, stockId, e);
            throw new StockException("Cannot mark stock as sold: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public List<Stock> markAsSold(List<UUID> stockIds) {
        log.info("ทำเครื่องหมายขายแล้วสำหรับสต็อกจำนวน {} รายการ - Marking {} stock items as sold", stockIds.size(), stockIds.size());
        
        List<Stock> soldStock = new ArrayList<>();
        
        for (UUID stockId : stockIds) {
            try {
                Stock stock = markAsSold(stockId);
                soldStock.add(stock);
            } catch (Exception e) {
                log.error("ไม่สามารถทำเครื่องหมายขายแล้วสำหรับสต็อก ID: {} - Cannot mark stock as sold ID: {}", stockId, stockId, e);
                // ข้ามรายการที่ผิดพลาดและดำเนินการต่อ
            }
        }
        
        log.info("ทำเครื่องหมายขายแล้วสำเร็จจำนวน {} รายการ - Successfully marked {} stock items as sold", soldStock.size(), soldStock.size());
        
        return soldStock;
    }

    @Override
    public List<Stock> getSoldStock(UUID productId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("ดึงรายการสต็อกที่ขายแล้วสำหรับสินค้า ID: {} - Getting sold stock for product ID: {}", productId, productId);
        
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
        log.debug("ตรวจสอบจำนวนสต็อกที่ขายแล้วสำหรับสินค้า ID: {} - Getting sold stock count for product ID: {}", productId, productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        return stockRepository.countByProductAndSoldTrue(product);
    }

    // ==================== INVENTORY MANAGEMENT OPERATIONS ====================

    @Override
    public StockStatistics getStockStatistics(UUID productId) {
        log.debug("ดึงสถิติสต็อกสำหรับสินค้า ID: {} - Getting stock statistics for product ID: {}", productId, productId);
        
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
        log.debug("ตรวจสอบสินค้าที่มีสต็อกต่ำด้วยเกณฑ์: {} - Getting products with low stock using threshold: {}", threshold, threshold);
        
        long searchThreshold = threshold != null ? threshold : DEFAULT_LOW_STOCK_THRESHOLD;
        
        List<Product> lowStockProducts = stockRepository.findProductsWithLowStock(searchThreshold);
        
        log.info("พบสินค้าที่มีสต็อกต่ำจำนวน {} รายการ - Found {} products with low stock", lowStockProducts.size(), lowStockProducts.size());
        
        return lowStockProducts;
    }

    @Override
    public boolean isLowStock(UUID productId) {
        log.debug("ตรวจสอบสต็อกต่ำสำหรับสินค้า ID: {} - Checking low stock for product ID: {}", productId, productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        long availableCount = getAvailableStockCount(productId);
        int threshold = product.getLowStockThreshold() != null ? product.getLowStockThreshold() : DEFAULT_LOW_STOCK_THRESHOLD;
        
        boolean isLow = availableCount <= threshold;
        
        log.debug("สินค้า ID: {} มีสต็อก {} รายการ เกณฑ์ {} = สต็อกต่ำ: {} - Product ID: {} has {} stock items, threshold {}, low stock: {}", 
                productId, availableCount, threshold, isLow, productId, availableCount, threshold, isLow);
        
        return isLow;
    }

    @Override
    @Transactional
    public int cleanupExpiredReservations() {
        log.debug("ล้างการจองที่หมดอายุ - Cleaning up expired reservations");
        
        int cleanedCount = stockRepository.clearExpiredReservations();
        
        if (cleanedCount > 0) {
            log.info("ล้างการจองที่หมดอายุสำเร็จจำนวน {} รายการ - Successfully cleaned up {} expired reservations", cleanedCount, cleanedCount);
        }
        
        return cleanedCount;
    }

    @Override
    public List<Stock> getExpiredReservations() {
        log.debug("ดึงรายการการจองที่หมดอายุ - Getting expired reservations");
        return stockRepository.findExpiredReservations();
    }

    @Override
    public List<Stock> getReservationsExpiringSoon(int withinMinutes) {
        log.debug("ดึงรายการการจองที่ใกล้หมดอายุภายใน {} นาที - Getting reservations expiring within {} minutes", withinMinutes, withinMinutes);
        
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(withinMinutes);
        List<Stock> expiringSoon = stockRepository.findReservationsExpiringSoon(threshold);
        
        log.debug("พบการจองที่ใกล้หมดอายุจำนวน {} รายการ - Found {} reservations expiring soon", expiringSoon.size(), expiringSoon.size());
        
        return expiringSoon;
    }

    // ==================== UTILITY OPERATIONS ====================

    @Override
    public List<String> findDuplicateCredentials(UUID productId) {
        log.debug("ตรวจสอบข้อมูลบัญชีที่ซ้ำสำหรับสินค้า ID: {} - Finding duplicate credentials for product ID: {}", productId, productId);
        
        List<String> duplicates = stockRepository.findDuplicateCredentialsByProductId(productId);
        
        if (!duplicates.isEmpty()) {
            log.warn("พบข้อมูลบัญชีที่ซ้ำจำนวน {} รายการสำหรับสินค้า ID: {} - Found {} duplicate credentials for product ID: {}", 
                    duplicates.size(), productId, duplicates.size(), productId);
        }
        
        return duplicates;
    }

    @Override
    public boolean credentialsExist(UUID productId, String credentials) {
        log.debug("ตรวจสอบการมีอยู่ของข้อมูลบัญชีสำหรับสินค้า ID: {} - Checking credentials existence for product ID: {}", productId, productId);
        return stockRepository.existsByProductIdAndCredentials(productId, credentials);
    }

    @Override
    @Transactional
    public void updateLowStockThreshold(UUID productId, int threshold) {
        log.info("อัปเดตเกณฑ์สต็อกต่ำเป็น {} สำหรับสินค้า ID: {} - Updating low stock threshold to {} for product ID: {}", 
                threshold, productId, threshold, productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> {
                log.error("ไม่พบสินค้า ID: {} - Product not found with ID: {}", productId, productId);
                return new ResourceNotFoundException("Product not found with ID: " + productId);
            });
        
        product.setLowStockThreshold(threshold);
        productRepository.save(product);
        
        log.info("อัปเดตเกณฑ์สต็อกต่ำสำเร็จสำหรับสินค้า: {} - Successfully updated low stock threshold for product: {}", 
                product.getName(), product.getName());
    }
}