package com.accountselling.platform.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for creating a new stock item.
 * DTO สำหรับการสร้างสต็อกรายการใหม่
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a new stock item")
public class StockCreateRequestDto {

    @NotNull(message = "Product ID is required")
    @Schema(description = "Product ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479", required = true)
    private UUID productId;

    @NotBlank(message = "Credentials are required")
    @Size(max = 2000, message = "Credentials cannot exceed 2000 characters")
    @Schema(description = "Account credentials (will be encrypted)", 
            example = "username:player123\\npassword:secret456\\nemail:player@example.com", 
            required = true)
    private String credentials;

    @Size(max = 1000, message = "Additional info cannot exceed 1000 characters")
    @Schema(description = "Additional information about the account", 
            example = "Level 80 character with rare items")
    private String additionalInfo;
}