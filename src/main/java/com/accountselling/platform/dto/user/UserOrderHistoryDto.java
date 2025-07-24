package com.accountselling.platform.dto.user;

import com.accountselling.platform.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for user order history response.
 * Contains order information for user's order history display.
 * 
 * DTO สำหรับการแสดงประวัติคำสั่งซื้อของผู้ใช้
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderHistoryDto {

    private UUID orderId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Integer totalItems;
    private List<OrderItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private UUID productId;
        private String productName;
        private String categoryName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}