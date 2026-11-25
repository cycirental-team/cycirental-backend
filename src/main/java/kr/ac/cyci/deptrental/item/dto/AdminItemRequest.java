package kr.ac.cyci.deptrental.item.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body used only by administrators when they manage inventory. */
public record AdminItemRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 500) String description,
        @Min(1) @Max(10000) int totalQuantity,
        @Min(0) @Max(10000) int availableQuantity) {
}
