package com.formforge.dto.request;

import com.formforge.entity.enums.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFieldRequest {

    @NotBlank(message = "Field key is required")
    @Size(min = 1, max = 100, message = "Field key must be 1-100 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "Field key must start with letter and contain only alphanumeric and underscore")
    private String fieldKey;

    @NotNull(message = "Field type is required")
    private FieldType fieldType;

    @NotBlank(message = "Label is required")
    @Size(min = 1, max = 255, message = "Label must be 1-255 characters")
    private String label;

    @Size(max = 255, message = "Placeholder must be at most 255 characters")
    private String placeholder;

    @Size(max = 1000, message = "Help text must be at most 1000 characters")
    private String helpText;

    @Builder.Default
    private Boolean isRequired = false;

    /**
     * Validation rules as JSON.
     * Example: {"minLength": 3, "maxLength": 100, "pattern": "^[a-zA-Z]+$"}
     */
    private String validationRules;

    /**
     * Field-specific config as JSON.
     * Example: {"options": [{"value": "1", "label": "Option 1"}]}
     */
    private String fieldConfig;

    @Size(max = 500, message = "Default value must be at most 500 characters")
    private String defaultValue;
}
