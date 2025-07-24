package com.accountselling.platform.service.impl;

import com.accountselling.platform.dto.statistics.DailyOrderStatistics;
import com.accountselling.platform.dto.statistics.OrderStatistics;
import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.exception.*;
import com.accountselling.platform.model.*;
import com.accountselling.platform.repository.*;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of OrderService for order management operations.
 * Provides comprehensive order processing functionality including creation,
 * status management, payment integration, and business logic operations
 * with proper transaction management and concurrent access protection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockService stockService;

    // ==================== ORDER CREATION ====================

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order createOrder(User user, Map<UUID, Integer> productQuantities) {
        log.info("Creating order for user: {} with {} products", 
                user.getUsername(), productQuantities.size());
        
        // Validate input parameters
        validateOrderCreationInput(user, productQuantities);
        
        // Validate order business rules
        validateOrder(user, productQuantities);
        
        // Calculate total amount
        BigDecimal totalAmount = calculateOrderTotal(productQuantities);
        
        // Create order entity
        Order order = new Order(user, totalAmount);
        Order savedOrder = orderRepository.save(order);
        
        try {
            // Reserve stock and create order items
            reserveStockForOrder(savedOrder, productQuantities);
            
            log.info("Successfully created order: {} for user: {} with total: {}", 
                    savedOrder.getOrderNumber(), user.getUsername(), totalAmount);
            
            return savedOrder;
            
        } catch (Exception e) {
            log.error("Failed to reserve stock for order creation. Rolling back order: {}", 
                     savedOrder.getOrderNumber(), e);
            throw e; // Transaction will be rolled back automatically
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order createOrder(User user, UUID productId, int quantity) {
        log.info("Creating single product order for user: {} with product: {} quantity: {}", 
                user.getUsername(), productId, quantity);
        
        Map<UUID, Integer> productQuantities = Map.of(productId, quantity);
        return createOrder(user, productQuantities);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order createOrderByUsername(String username, Map<UUID, Integer> productQuantities) {
        log.info("Creating order for username: {} with {} products", 
                username, productQuantities.size());
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.error("User not found with username: {}", username);
                return new ResourceNotFoundException("User not found with username: " + username);
            });
        
        return createOrder(user, productQuantities);
    }

    // ==================== ORDER RETRIEVAL ====================

    @Override
    public Order findById(UUID orderId) {
        log.debug("Finding order by ID: {}", orderId);
        
        return orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.error("Order not found with ID: {}", orderId);
                return new ResourceNotFoundException("Order not found with ID: " + orderId);
            });
    }

    @Override
    public Order findByOrderNumber(String orderNumber) {
        log.debug("Finding order by number: {}", orderNumber);
        
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> {
                log.error("Order not found with number: {}", orderNumber);
                return new ResourceNotFoundException("Order not found with number: " + orderNumber);
            });
    }

    @Override
    public Page<Order> getOrdersByUser(User user, Pageable pageable) {
        log.debug("Getting orders for user: {} with pagination", user.getUsername());
        
        return orderRepository.findByUser(user, pageable);
    }

    @Override
    public Page<Order> getOrdersByUsername(String username, Pageable pageable) {
        log.debug("Getting orders for username: {} with pagination", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.error("User not found with username: {}", username);
                return new ResourceNotFoundException("User not found with username: " + username);
            });
        
        return getOrdersByUser(user, pageable);
    }

    @Override
    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.debug("Getting orders by status: {} with pagination", status);
        
        return orderRepository.findByStatus(status, pageable);
    }

    @Override
    public List<Order> getOrdersByUserAndStatus(User user, OrderStatus status) {
        log.debug("Getting orders for user: {} with status: {}", user.getUsername(), status);
        
        return orderRepository.findByUserAndStatus(user, status);
    }

    // ==================== ORDER STATUS MANAGEMENT ====================

    @Override
    @Transactional
    public Order markOrderAsProcessing(UUID orderId) {
        log.info("Marking order as processing: {}", orderId);
        
        Order order = findById(orderId);
        
        try {
            order.markAsProcessing();
            Order updatedOrder = orderRepository.save(order);
            
            log.info("Successfully marked order as processing: {}", order.getOrderNumber());
            return updatedOrder;
            
        } catch (IllegalStateException e) {
            log.error("Cannot transition order {} to processing status: {}", 
                     order.getOrderNumber(), e.getMessage());
            throw new InvalidOrderStatusException("Cannot mark order as processing: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Order markOrderAsCompleted(UUID orderId) {
        log.info("Marking order as completed: {}", orderId);
        
        Order order = findById(orderId);
        
        try {
            order.markAsCompleted();
            Order updatedOrder = orderRepository.save(order);
            
            // Mark all stock items as sold
            markStockItemsAsSold(orderId);
            
            log.info("Successfully marked order as completed: {}", order.getOrderNumber());
            return updatedOrder;
            
        } catch (IllegalStateException e) {
            log.error("Cannot transition order {} to completed status: {}", 
                     order.getOrderNumber(), e.getMessage());
            throw new InvalidOrderStatusException("Cannot mark order as completed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Order markOrderAsFailed(UUID orderId, String reason) {
        log.info("Marking order as failed: {} with reason: {}", orderId, reason);
        
        Order order = findById(orderId);
        
        try {
            order.markAsFailed();
            if (reason != null && !reason.trim().isEmpty()) {
                order.setNotes(order.getNotes() != null ? 
                              order.getNotes() + "\nFailure reason: " + reason : 
                              "Failure reason: " + reason);
            }
            
            Order updatedOrder = orderRepository.save(order);
            
            // Release stock reservations
            releaseStockReservations(orderId);
            
            log.info("Successfully marked order as failed: {}", order.getOrderNumber());
            return updatedOrder;
            
        } catch (IllegalStateException e) {
            log.error("Cannot transition order {} to failed status: {}", 
                     order.getOrderNumber(), e.getMessage());
            throw new InvalidOrderStatusException("Cannot mark order as failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Order cancelOrder(UUID orderId, String reason) {
        log.info("Cancelling order: {} with reason: {}", orderId, reason);
        
        Order order = findById(orderId);
        
        if (!order.canBeCancelled()) {
            log.error("Order cannot be cancelled: {} with status: {}", 
                     order.getOrderNumber(), order.getStatus());
            throw new InvalidOrderStatusException("Order cannot be cancelled in current status: " + order.getStatus());
        }
        
        try {
            order.markAsCancelled();
            if (reason != null && !reason.trim().isEmpty()) {
                order.setNotes(order.getNotes() != null ? 
                              order.getNotes() + "\nCancellation reason: " + reason : 
                              "Cancellation reason: " + reason);
            }
            
            Order updatedOrder = orderRepository.save(order);
            
            // Release stock reservations
            releaseStockReservations(orderId);
            
            log.info("Successfully cancelled order: {}", order.getOrderNumber());
            return updatedOrder;
            
        } catch (IllegalStateException e) {
            log.error("Cannot transition order {} to cancelled status: {}", 
                     order.getOrderNumber(), e.getMessage());
            throw new InvalidOrderStatusException("Cannot cancel order: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Order cancelOrderByUser(UUID orderId, User user, String reason) {
        log.info("User {} cancelling order: {} with reason: {}", 
                user.getUsername(), orderId, reason);
        
        Order order = findById(orderId);
        
        if (!order.belongsToUser(user)) {
            log.error("User {} does not own order: {}", user.getUsername(), order.getOrderNumber());
            throw new UnauthorizedException("User does not have permission to cancel this order");
        }
        
        return cancelOrder(orderId, reason);
    }

    // ==================== ORDER VALIDATION ====================

    @Override
    public void validateOrder(User user, Map<UUID, Integer> productQuantities) {
        log.debug("Validating order for user: {} with {} products", 
                 user.getUsername(), productQuantities.size());
        
        // Check if user can create orders
        if (!canUserCreateOrders(user)) {
            log.error("User {} is not eligible to create orders", user.getUsername());
            throw new InvalidOrderException("User is not eligible to create orders");
        }
        
        // Validate each product and quantity
        for (Map.Entry<UUID, Integer> entry : productQuantities.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();
            
            // Validate quantity
            if (quantity == null || quantity <= 0) {
                log.error("Invalid quantity {} for product: {}", quantity, productId);
                throw new InvalidOrderException("Quantity must be positive for product: " + productId);
            }
            
            // Check product availability and stock
            int availableQuantity = getAvailableQuantityForProduct(productId);
            if (availableQuantity < quantity) {
                log.error("Insufficient stock for product: {}. Required: {}, Available: {}", 
                         productId, quantity, availableQuantity);
                throw new InsufficientStockException(
                    String.format("Insufficient stock for product. Required: %d, Available: %d", 
                                 quantity, availableQuantity));
            }
        }
        
        log.debug("Order validation passed for user: {}", user.getUsername());
    }

    @Override
    public boolean canUserCreateOrders(User user) {
        // Check if user exists and is active
        if (user == null) {
            return false;
        }
        
        // Add additional business rules here (e.g., user status, restrictions, etc.)
        // For now, assume all valid users can create orders
        return true;
    }

    @Override
    public boolean canOrderBeCancelled(UUID orderId) {
        try {
            Order order = findById(orderId);
            return order.canBeCancelled();
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean doesOrderBelongToUser(UUID orderId, User user) {
        try {
            Order order = findById(orderId);
            return order.belongsToUser(user);
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    // ==================== ORDER BUSINESS LOGIC ====================

    @Override
    public BigDecimal calculateOrderTotal(Map<UUID, Integer> productQuantities) {
        log.debug("Calculating order total for {} products", productQuantities.size());
        
        BigDecimal total = BigDecimal.ZERO;
        
        for (Map.Entry<UUID, Integer> entry : productQuantities.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID: " + productId);
                });
            
            BigDecimal productTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(productTotal);
        }
        
        log.debug("Calculated order total: {}", total);
        return total;
    }

    @Override
    public int getAvailableQuantityForProduct(UUID productId) {
        log.debug("Getting available quantity for product: {}", productId);
        
        // Verify product exists
        productRepository.findById(productId)
            .orElseThrow(() -> {
                log.error("Product not found with ID: {}", productId);
                return new ResourceNotFoundException("Product not found with ID: " + productId);
            });
        
        return (int) stockRepository.countAvailableByProductId(productId);
    }

    @Override
    @Transactional
    public void reserveStockForOrder(Order order, Map<UUID, Integer> productQuantities) {
        log.info("Reserving stock for order: {}", order.getOrderNumber());
        
        for (Map.Entry<UUID, Integer> entry : productQuantities.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with ID: " + productId);
                });
            
            // Reserve the required number of stock items (30 minutes default reservation)
            List<Stock> reservedStocks = stockService.reserveStock(productId, quantity, 30);
            
            // Create order items for each reserved stock
            for (Stock reservedStock : reservedStocks) {
                OrderItem orderItem = new OrderItem(order, product, reservedStock, product.getPrice());
                order.addOrderItem(orderItem);
            }
        }
        
        // Update order total amount
        order.updateTotalAmount();
        
        log.info("Successfully reserved {} stock items for order: {}", 
                order.getOrderItems().size(), order.getOrderNumber());
    }

    @Override
    @Transactional
    public void releaseStockReservations(UUID orderId) {
        log.info("Releasing stock reservations for order: {}", orderId);
        
        Order order = findById(orderId);
        
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getStockItem() != null && !orderItem.isStockItemSold()) {
                orderItem.releaseStockItemReservation();
                log.debug("Released reservation for stock item: {}", orderItem.getStockItem().getId());
            }
        }
        
        log.info("Successfully released stock reservations for order: {}", order.getOrderNumber());
    }

    @Override
    @Transactional
    public void markStockItemsAsSold(UUID orderId) {
        log.info("Marking stock items as sold for order: {}", orderId);
        
        Order order = findById(orderId);
        
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getStockItem() != null && !orderItem.isStockItemSold()) {
                orderItem.markStockItemAsSold();
                log.debug("Marked stock item as sold: {}", orderItem.getStockItem().getId());
            }
        }
        
        log.info("Successfully marked {} stock items as sold for order: {}", 
                order.getOrderItems().size(), order.getOrderNumber());
    }

    // ==================== ORDER SEARCH AND REPORTING ====================

    @Override
    public Page<Order> searchOrders(String orderNumber, String username, OrderStatus status,
                                  LocalDateTime startDate, LocalDateTime endDate,
                                  BigDecimal minAmount, BigDecimal maxAmount,
                                  Pageable pageable) {
        log.debug("Searching orders with criteria - orderNumber: {}, username: {}, status: {}", 
                 orderNumber, username, status);
        
        return orderRepository.searchOrders(orderNumber, username, status, 
                                          startDate, endDate, minAmount, maxAmount, pageable);
    }

    @Override
    public OrderStatistics getOrderStatistics() {
        log.debug("Getting order statistics");
        
        Object[] stats = orderRepository.getOrderStatistics();
        
        long total = ((Number) stats[0]).longValue();
        long pending = ((Number) stats[1]).longValue();
        long completed = ((Number) stats[2]).longValue();
        long failed = ((Number) stats[3]).longValue();
        
        // Calculate processing and cancelled from additional queries
        long processing = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        BigDecimal averageOrderValue = orderRepository.calculateAverageOrderValue();
        
        return new OrderStatistics(total, pending, processing, completed, failed, cancelled,
                                 totalRevenue, averageOrderValue);
    }

    @Override
    public OrderStatistics getOrderStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting order statistics for period: {} to {}", startDate, endDate);
        
        long total = orderRepository.countOrdersBetweenDates(startDate, endDate);
        long completed = orderRepository.countCompletedOrdersBetweenDates(startDate, endDate);
        
        // For status-specific counts within date range, we'll use the total counts minus completed
        // This is a simplified approach - in production you might want to add specific queries
        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        long processing = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long failed = orderRepository.countByStatus(OrderStatus.FAILED);
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        
        BigDecimal totalRevenue = orderRepository.calculateRevenueBetweenDates(startDate, endDate);
        BigDecimal averageOrderValue = total > 0 ? totalRevenue.divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        return new OrderStatistics(total, pending, processing, completed, failed, cancelled,
                                 totalRevenue, averageOrderValue);
    }

    @Override
    public List<DailyOrderStatistics> getDailyOrderStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting daily order statistics for period: {} to {}", startDate, endDate);
        
        List<Object[]> dailyStats = orderRepository.getDailyOrderStatistics(startDate, endDate);
        
        return dailyStats.stream()
            .map(stat -> new DailyOrderStatistics(
                (LocalDateTime) stat[0],
                ((Number) stat[1]).longValue(),
                (BigDecimal) stat[2]
            ))
            .toList();
    }

    @Override
    public List<User> getTopCustomersByOrderCount(int limit) {
        log.debug("Getting top {} customers by order count", limit);
        
        return orderRepository.findTopCustomersByOrderCount(limit);
    }

    @Override
    public List<Order> getRecentOrders(int limit) {
        log.debug("Getting {} recent orders", limit);
        
        return orderRepository.findRecentOrders(limit);
    }

    // ==================== ORDER INTEGRATION ====================

    @Override
    @Transactional
    public void processOrderCompletion(UUID orderId, String paymentTransactionId) {
        log.info("Processing order completion for order: {} with transaction: {}", 
                orderId, paymentTransactionId);
        
        Order completedOrder = markOrderAsCompleted(orderId);
        
        log.info("Successfully processed order completion: {} with transaction: {}", 
                completedOrder.getOrderNumber(), paymentTransactionId);
    }

    @Override
    @Transactional
    public void processOrderFailure(UUID orderId, String failureReason) {
        log.info("Processing order failure for order: {} with reason: {}", orderId, failureReason);
        
        Order failedOrder = markOrderAsFailed(orderId, failureReason);
        
        log.info("Successfully processed order failure: {} with reason: {}", 
                failedOrder.getOrderNumber(), failureReason);
    }

    @Override
    public Map<String, String> getOrderDownloadInfo(UUID orderId, User user) {
        log.info("Getting download info for order: {} by user: {}", orderId, user.getUsername());
        
        Order order = findById(orderId);
        
        // Verify order belongs to user
        if (!order.belongsToUser(user)) {
            log.error("User {} does not own order: {}", user.getUsername(), order.getOrderNumber());
            throw new UnauthorizedException("User does not have permission to access this order");
        }
        
        // Verify order is completed
        if (!order.isCompleted()) {
            log.error("Order {} is not completed, status: {}", order.getOrderNumber(), order.getStatus());
            throw new InvalidOrderStatusException("Order must be completed to download information");
        }
        
        Map<String, String> downloadInfo = new HashMap<>();
        
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getStockItem() != null) {
                String productName = orderItem.getProductName();
                String credentials = orderItem.getStockCredentials();
                
                if (credentials != null) {
                    downloadInfo.put(productName, credentials);
                    log.debug("Added download info for product: {}", productName);
                }
            }
        }
        
        log.info("Successfully retrieved download info for {} products in order: {}", 
                downloadInfo.size(), order.getOrderNumber());
        
        return downloadInfo;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void validateOrderCreationInput(User user, Map<UUID, Integer> productQuantities) {
        if (user == null) {
            throw new InvalidOrderException("User cannot be null");
        }
        
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new InvalidOrderException("Product quantities cannot be null or empty");
        }
        
        for (Map.Entry<UUID, Integer> entry : productQuantities.entrySet()) {
            if (entry.getKey() == null) {
                throw new InvalidOrderException("Product ID cannot be null");
            }
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new InvalidOrderException("Quantity must be positive for product: " + entry.getKey());
            }
        }
    }
}