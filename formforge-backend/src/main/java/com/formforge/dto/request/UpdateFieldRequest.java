package com.formforge.dto.request;

import com.formforge.entity.enums.FieldType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFieldRequest {

    private FieldType fieldType;

    @Size(min = 1, max = 255, message = "Label must be 1-255 characters")
    private String label;

    @Size(max = 255, message = "Placeholder must be at most 255 characters")
    private String placeholder;

    @Size(max = 1000, message = "Help text must be at most 1000 characters")
    private String helpText;

    private Boolean isRequired;

    private String validationRules;

    private String fieldConfig;

    @Size(max = 500, message = "Default value must be at most 500 characters")
    private String defaultValue;
}
