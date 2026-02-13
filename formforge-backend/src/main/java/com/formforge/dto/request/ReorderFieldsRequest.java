package com.formforge.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderFieldsRequest {

    @NotNull(message = "Field order is required")
    private List<FieldOrderItem> fieldOrder;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldOrderItem {
        @NotNull(message = "Field ID is required")
        private Long fieldId;

        @NotNull(message = "Display order is required")
        private Integer displayOrder;
    }
}
