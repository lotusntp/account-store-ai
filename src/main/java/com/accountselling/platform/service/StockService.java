package com.accountselling.platform.service;

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
 * อินเทอร์เฟซสำหรับการจัดการสต็อกสินค้า
 * รองรับการตรวจสอบความพร้อม การจองสินค้า การติดตามการขาย และการแจ้งเตือน
 */
public interface StockService {

    // ==================== BASIC STOCK OPERATIONS ====================

    /**
     * สร้างรายการสต็อกใหม่สำหรับสินค้า
     * Create new stock item for a product
     * 
     * @param productId ID ของสินค้า
     * @param credentials ข้อมูลบัญชีที่เข้ารหัสแล้ว
     * @param additionalInfo ข้อมูลเพิ่มเติม (ถ้ามี)
     * @return Stock รายการสต็อกที่สร้างขึ้น
     * @throws ResourceNotFoundException หากไม่พบสินค้า
     * @throws ResourceAlreadyExistsException หากมีข้อมูลบัญชีซ้ำ
     */
    Stock createStock(UUID productId, String credentials, String additionalInfo);

    /**
     * สร้างรายการสต็อกหลายรายการพร้อมกัน
     * Create multiple stock items for a product in bulk
     * 
     * @param productId ID ของสินค้า
     * @param credentialsList รายการข้อมูลบัญชีที่เข้ารหัสแล้ว
     * @return List<Stock> รายการสต็อกที่สร้างขึ้น
     * @throws ResourceNotFoundException หากไม่พบสินค้า
     */
    List<Stock> createBulkStock(UUID productId, List<String> credentialsList);

    /**
     * ดึงข้อมูลสต็อกทั้งหมดของสินค้า
     * Get all stock items for a product
     * 
     * @param productId ID ของสินค้า
     * @param pageable การแบ่งหน้าข้อมูล
     * @return Page<Stock> รายการสต็อกแบบแบ่งหน้า
     */
    Page<Stock> getStockByProduct(UUID productId, Pageable pageable);

    /**
     * ดึงข้อมูลสต็อกโดย ID
     * Get stock item by ID
     * 
     * @param stockId ID ของสต็อก
     * @return Optional<Stock> รายการสต็อก หรือ empty หากไม่พบ
     */
    Optional<Stock> getStockById(UUID stockId);

    /**
     * อัปเดตข้อมูลเพิ่มเติมของสต็อก
     * Update additional information of stock item
     * 
     * @param stockId ID ของสต็อก
     * @param additionalInfo ข้อมูลเพิ่มเติมใหม่
     * @return Stock รายการสต็อกที่อัปเดตแล้ว
     * @throws ResourceNotFoundException หากไม่พบสต็อก
     */
    Stock updateStockAdditionalInfo(UUID stockId, String additionalInfo);

    /**
     * ลบรายการสต็อก (เฉพาะที่ยังไม่ได้ขาย)
     * Delete stock item (only if not sold)
     * 
     * @param stockId ID ของสต็อก
     * @throws ResourceNotFoundException หากไม่พบสต็อก
     * @throws StockException หากสต็อกถูกขายแล้วหรือถูกจองอยู่
     */
    void deleteStock(UUID stockId);

    // ==================== STOCK AVAILABILITY OPERATIONS ====================

    /**
     * ตรวจสอบสต็อกที่มีจำหน่ายของสินค้า
     * Get available stock count for a product
     * 
     * @param productId ID ของสินค้า
     * @return long จำนวนสต็อกที่มีจำหน่าย
     */
    long getAvailableStockCount(UUID productId);

    /**
     * ตรวจสอบสต็อกทั้งหมดของสินค้า
     * Get total stock count for a product
     * 
     * @param productId ID ของสินค้า
     * @return long จำนวนสต็อกทั้งหมด
     */
    long getTotalStockCount(UUID productId);

    /**
     * ดึงรายการสต็อกที่มีจำหน่ายของสินค้า
     * Get available stock items for a product
     * 
     * @param productId ID ของสินค้า
     * @return List<Stock> รายการสต็อกที่มีจำหน่าย
     */
    List<Stock> getAvailableStock(UUID productId);

    /**
     * ดึงสต็อกรายการแรกที่มีจำหน่ายของสินค้า
     * Get first available stock item for a product (FIFO)
     * 
     * @param productId ID ของสินค้า
     * @return Optional<Stock> สต็อกรายการแรกที่มีจำหน่าย หรือ empty หากไม่มี
     */
    Optional<Stock> getFirstAvailableStock(UUID productId);

    /**
     * ตรวจสอบว่าสินค้ามีสต็อกหรือไม่
     * Check if product has available stock
     * 
     * @param productId ID ของสินค้า
     * @return boolean true หากมีสต็อก, false หากไม่มี
     */
    boolean isInStock(UUID productId);

    /**
     * ตรวจสอบว่าสินค้าหมดสต็อกหรือไม่
     * Check if product is out of stock
     * 
     * @param productId ID ของสินค้า
     * @return boolean true หากหมดสต็อก, false หากยังมีสต็อก
     */
    boolean isOutOfStock(UUID productId);

    // ==================== STOCK RESERVATION OPERATIONS ====================

    /**
     * จองสต็อกสำหรับการซื้อ (จองชั่วคราว)
     * Reserve stock for purchase (temporary reservation)
     * 
     * @param productId ID ของสินค้า
     * @param quantity จำนวนที่ต้องการจอง
     * @param reservationDurationMinutes ระยะเวลาการจองเป็นนาที
     * @return List<Stock> รายการสต็อกที่จองได้
     * @throws OutOfStockException หากสต็อกไม่เพียงพอ
     * @throws StockReservationException หากจองไม่สำเร็จ
     */
    List<Stock> reserveStock(UUID productId, int quantity, int reservationDurationMinutes);

    /**
     * จองสต็อกจนถึงเวลาที่กำหนด
     * Reserve stock until specific time
     * 
     * @param productId ID ของสินค้า
     * @param quantity จำนวนที่ต้องการจอง
     * @param reservedUntil เวลาสิ้นสุดการจอง
     * @return List<Stock> รายการสต็อกที่จองได้
     * @throws OutOfStockException หากสต็อกไม่เพียงพอ
     * @throws StockReservationException หากจองไม่สำเร็จ
     */
    List<Stock> reserveStockUntil(UUID productId, int quantity, LocalDateTime reservedUntil);

    /**
     * ยกเลิกการจองสต็อก
     * Release stock reservation
     * 
     * @param stockIds รายการ ID ของสต็อกที่ต้องการยกเลิกการจอง
     * @return int จำนวนสต็อกที่ยกเลิกการจองได้
     */
    int releaseReservation(List<UUID> stockIds);

    /**
     * ยกเลิกการจองสต็อกรายการเดียว
     * Release single stock reservation
     * 
     * @param stockId ID ของสต็อกที่ต้องการยกเลิกการจอง
     * @return boolean true หากยกเลิกสำเร็จ, false หากไม่สำเร็จ
     * @throws ResourceNotFoundException หากไม่พบสต็อก
     */
    boolean releaseReservation(UUID stockId);

    /**
     * ดึงรายการสต็อกที่ถูกจองอยู่
     * Get currently reserved stock items
     * 
     * @param productId ID ของสินค้า (null หากต้องการทุกสินค้า)
     * @return List<Stock> รายการสต็อกที่ถูกจองอยู่
     */
    List<Stock> getReservedStock(UUID productId);

    /**
     * ตรวจสอบจำนวนสต็อกที่ถูกจองอยู่
     * Get reserved stock count for a product
     * 
     * @param productId ID ของสินค้า
     * @return long จำนวนสต็อกที่ถูกจองอยู่
     */
    long getReservedStockCount(UUID productId);

    // ==================== STOCK SALES OPERATIONS ====================

    /**
     * ทำเครื่องหมายสต็อกว่าขายแล้ว
     * Mark stock as sold
     * 
     * @param stockId ID ของสต็อก
     * @return Stock รายการสต็อกที่ทำเครื่องหมายขายแล้ว
     * @throws ResourceNotFoundException หากไม่พบสต็อก
     * @throws StockException หากสต็อกถูกขายแล้ว
     */
    Stock markAsSold(UUID stockId);

    /**
     * ทำเครื่องหมายสต็อกหลายรายการว่าขายแล้ว
     * Mark multiple stock items as sold
     * 
     * @param stockIds รายการ ID ของสต็อกที่ต้องการทำเครื่องหมายขายแล้ว
     * @return List<Stock> รายการสต็อกที่ทำเครื่องหมายขายแล้ว
     */
    List<Stock> markAsSold(List<UUID> stockIds);

    /**
     * ดึงรายการสต็อกที่ขายแล้ว
     * Get sold stock items
     * 
     * @param productId ID ของสินค้า (null หากต้องการทุกสินค้า)
     * @param startDate วันที่เริ่มต้น (null หากไม่จำกัด)
     * @param endDate วันที่สิ้นสุด (null หากไม่จำกัด)
     * @return List<Stock> รายการสต็อกที่ขายแล้ว
     */
    List<Stock> getSoldStock(UUID productId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * ตรวจสอบจำนวนสต็อกที่ขายแล้ว
     * Get sold stock count for a product
     * 
     * @param productId ID ของสินค้า
     * @return long จำนวนสต็อกที่ขายแล้ว
     */
    long getSoldStockCount(UUID productId);

    // ==================== INVENTORY MANAGEMENT OPERATIONS ====================

    /**
     * ดึงสถิติสต็อกของสินค้า
     * Get stock statistics for a product
     * 
     * @param productId ID ของสินค้า
     * @return StockStatistics สถิติของสต็อก
     */
    StockStatistics getStockStatistics(UUID productId);

    /**
     * ตรวจสอบสินค้าที่มีสต็อกต่ำ
     * Get products with low stock
     * 
     * @param threshold ค่าขีดจำกัดสต็อกต่ำ (null หากใช้ค่าของสินค้า)
     * @return List<Product> รายการสินค้าที่มีสต็อกต่ำ
     */
    List<Product> getProductsWithLowStock(Integer threshold);

    /**
     * ตรวจสอบว่าสินค้ามีสต็อกต่ำหรือไม่
     * Check if product has low stock
     * 
     * @param productId ID ของสินค้า
     * @return boolean true หากสต็อกต่ำ, false หากสต็อกปกติ
     */
    boolean isLowStock(UUID productId);

    /**
     * ล้างการจองที่หมดอายุ
     * Clean up expired reservations
     * 
     * @return int จำนวนการจองที่ล้างได้
     */
    int cleanupExpiredReservations();

    /**
     * ดึงรายการการจองที่หมดอายุ
     * Get expired reservations
     * 
     * @return List<Stock> รายการสต็อกที่การจองหมดอายุแล้ว
     */
    List<Stock> getExpiredReservations();

    /**
     * ดึงรายการการจองที่ใกล้หมดอายุ
     * Get reservations expiring soon
     * 
     * @param withinMinutes ระยะเวลาที่ถือว่า "ใกล้หมดอายุ" เป็นนาที
     * @return List<Stock> รายการสต็อกที่การจองใกล้หมดอายุ
     */
    List<Stock> getReservationsExpiringSoon(int withinMinutes);

    // ==================== UTILITY OPERATIONS ====================

    /**
     * ตรวจสอบการซ้ำของข้อมูลบัญชีในสินค้า
     * Check for duplicate credentials in product stock
     * 
     * @param productId ID ของสินค้า
     * @return List<String> รายการข้อมูลบัญชีที่ซ้ำ
     */
    List<String> findDuplicateCredentials(UUID productId);

    /**
     * ตรวจสอบว่าข้อมูลบัญชีมีอยู่แล้วหรือไม่
     * Check if credentials already exist for a product
     * 
     * @param productId ID ของสินค้า
     * @param credentials ข้อมูลบัญชีที่ต้องการตรวจสอบ
     * @return boolean true หากมีอยู่แล้ว, false หากไม่มี
     */
    boolean credentialsExist(UUID productId, String credentials);

    /**
     * อัปเดตเกณฑ์สต็อกต่ำของสินค้า
     * Update low stock threshold for a product
     * 
     * @param productId ID ของสินค้า
     * @param threshold ค่าเกณฑ์สต็อกต่ำใหม่
     * @throws ResourceNotFoundException หากไม่พบสินค้า
     */
    void updateLowStockThreshold(UUID productId, int threshold);

    /**
     * Class สำหรับเก็บสถิติของสต็อก
     * Class for storing stock statistics
     */
    record StockStatistics(
        long total,        // จำนวนสต็อกทั้งหมด
        long available,    // จำนวนสต็อกที่มีจำหน่าย
        long sold,         // จำนวนสต็อกที่ขายแล้ว
        long reserved      // จำนวนสต็อกที่ถูกจองอยู่
    ) {}
}