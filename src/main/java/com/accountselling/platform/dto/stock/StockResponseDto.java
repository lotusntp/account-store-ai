package com.accountselling.platform.dto.stock;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for stock response information. DTO สำหรับข้อมูลตอบกลับของสต็อก */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO containing stock information")
public class StockResponseDto {

  @Schema(description = "Stock ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
  private UUID id;

  @Schema(description = "Product information")
  private ProductInfo product;

  @Schema(
      description = "Account credentials (masked for security)",
      example = "username:***\\npassword:***")
  private String maskedCredentials;

  @Schema(
      description = "Additional information about the account",
      example = "Level 80 character with rare items")
  private String additionalInfo;

  @Schema(description = "Whether this stock item is sold", example = "false")
  private Boolean sold;

  @Schema(description = "Sale date/time (if sold)")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime soldAt;

  @Schema(description = "Reservation expiry date/time (if reserved)")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime reservedUntil;

  @Schema(description = "Whether this stock item is currently reserved", example = "false")
  private Boolean reserved;

  @Schema(description = "Stock status", example = "AVAILABLE")
  private String status;

  @Schema(description = "Creation date/time")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime createdAt;

  @Schema(description = "Last update date/time")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime updatedAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Product information")
  public static class ProductInfo {
    @Schema(description = "Product ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID id;

    @Schema(description = "Product name", example = "Premium Gaming Account")
    private String name;

    @Schema(description = "Server name", example = "US-West")
    private String server;

    @Schema(description = "Category name", example = "Gaming Accounts")
    private String categoryName;

    @Schema(description = "Whether the product is active", example = "true")
    private Boolean active;
  }

  /** Get status based on stock state. รับสถานะตามสถานะของสต็อก */
  public String getStatus() {
    if (sold != null && sold) {
      return "SOLD";
    }
    if (reserved != null
        && reserved
        && reservedUntil != null
        && reservedUntil.isAfter(LocalDateTime.now())) {
      return "RESERVED";
    }
    return "AVAILABLE";
  }

  /** Check if stock is currently reserved. ตรวจสอบว่าสต็อกถูกจองอยู่หรือไม่ */
  public Boolean getReserved() {
    return reservedUntil != null
        && reservedUntil.isAfter(LocalDateTime.now())
        && (sold == null || !sold);
  }
}
