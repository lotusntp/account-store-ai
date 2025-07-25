package com.accountselling.platform.dto.statistics;

import java.math.BigDecimal;

/**
 * Order statistics data class containing comprehensive order metrics including counts by status,
 * revenue information, and average values.
 */
public record OrderStatistics(
    long total,
    long pending,
    long processing,
    long completed,
    long failed,
    long cancelled,
    BigDecimal totalRevenue,
    BigDecimal averageOrderValue) {}
