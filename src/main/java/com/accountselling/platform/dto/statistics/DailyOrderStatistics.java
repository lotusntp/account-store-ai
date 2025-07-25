package com.accountselling.platform.dto.statistics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Daily order statistics data class containing order metrics aggregated by date including order
 * counts and revenue.
 */
public record DailyOrderStatistics(LocalDateTime date, long orderCount, BigDecimal revenue) {}
