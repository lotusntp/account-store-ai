package com.accountselling.platform.dto.statistics;

import java.math.BigDecimal;

/**
 * Payment method statistics data class containing metrics aggregated by payment method including
 * counts, revenue, and success rates.
 */
public record PaymentMethodStatistics(
    String method, long totalCount, BigDecimal revenue, double successRate) {}
