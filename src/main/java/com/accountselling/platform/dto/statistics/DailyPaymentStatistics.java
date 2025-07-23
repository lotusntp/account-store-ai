package com.accountselling.platform.dto.statistics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Daily payment statistics data class containing payment metrics
 * aggregated by date including payment counts, revenue, and refunds.
 */
public record DailyPaymentStatistics(
    LocalDateTime date,
    long paymentCount,
    BigDecimal revenue,
    BigDecimal refunds
) {}