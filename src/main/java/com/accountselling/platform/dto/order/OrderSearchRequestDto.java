package com.accountselling.platform.dto.order;

import com.accountselling.platform.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for order search requests.
 * Contains criteria for searching and filtering orders.
 */
public record OrderSearchRequestDto(
    
    String orderNumber,
    String username,
    OrderStatus status,
    LocalDateTime startDate,
    LocalDateTime endDate,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    String sortBy,
    String sortDirection
    
) {
    
    /**
     * Check if any search criteria is provided.
     * 
     * @return true if at least one search criterion is specified
     */
    public boolean hasSearchCriteria() {
        return orderNumber != null || username != null || status != null ||
               startDate != null || endDate != null || minAmount != null || maxAmount != null;
    }
    
    /**
     * Check if date range is specified.
     * 
     * @return true if both start and end dates are provided
     */
    public boolean hasDateRange() {
        return startDate != null && endDate != null;
    }
    
    /**
     * Check if amount range is specified.
     * 
     * @return true if min or max amount is provided
     */
    public boolean hasAmountRange() {
        return minAmount != null || maxAmount != null;
    }
    
    /**
     * Get effective sort direction.
     * 
     * @return "ASC" or "DESC", defaults to "DESC" if not specified
     */
    public String getEffectiveSortDirection() {
        return sortDirection != null && sortDirection.equalsIgnoreCase("ASC") ? "ASC" : "DESC";
    }
    
    /**
     * Get effective sort field.
     * 
     * @return sort field name, defaults to "createdAt" if not specified
     */
    public String getEffectiveSortBy() {
        return sortBy != null && !sortBy.trim().isEmpty() ? sortBy : "createdAt";
    }
    
    /**
     * Validate date range.
     * 
     * @return true if date range is valid (start <= end)
     */
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true; // No range specified is valid
        }
        return !startDate.isAfter(endDate);
    }
    
    /**
     * Validate amount range.
     * 
     * @return true if amount range is valid (min <= max)
     */
    public boolean isAmountRangeValid() {
        if (minAmount == null || maxAmount == null) {
            return true; // No range specified is valid
        }
        return minAmount.compareTo(maxAmount) <= 0;
    }
}