package com.accountselling.platform.dto.statistics;

import java.math.BigDecimal;

/**
 * Payment statistics data class containing comprehensive payment metrics including counts by
 * status, revenue information, and success rates.
 */
public record PaymentStatistics(
    long total,
    long pending,
    long processing,
    long completed,
    long failed,
    long cancelled,
    long refunded,
    BigDecimal totalRevenue,
    BigDecimal totalRefunded,
    BigDecimal averagePaymentAmount,
    double successRate) {}
