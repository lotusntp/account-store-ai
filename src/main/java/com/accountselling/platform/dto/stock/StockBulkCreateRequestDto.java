package com.accountselling.platform.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for creating multiple stock items in bulk. DTO สำหรับการสร้างสต็อกหลายรายการพร้อมกัน */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating multiple stock items in bulk")
public class StockBulkCreateRequestDto {

  @NotNull(message = "Product ID is required")
  @Schema(
      description = "Product ID",
      example = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      required = true)
  private UUID productId;

  @NotEmpty(message = "Credentials list cannot be empty")
  @Size(max = 100, message = "Cannot create more than 100 stock items at once")
  @Schema(
      description = "List of account credentials (each will be encrypted)",
      example = "[\"username:player1\\npassword:pass1\", \"username:player2\\npassword:pass2\"]",
      required = true)
  private List<
          @NotNull
          @Size(max = 2000, message = "Each credential entry cannot exceed 2000 characters") String>
      credentialsList;
}
