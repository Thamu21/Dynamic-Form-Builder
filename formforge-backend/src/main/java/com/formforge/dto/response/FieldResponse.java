package com.formforge.dto.response;

import com.formforge.entity.FormField;
import com.formforge.entity.enums.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldResponse {
    private Long id;
    private String fieldKey;
    private FieldType fieldType;
    private String label;
    private String placeholder;
    private String helpText;
    private Boolean isRequired;
    private Integer displayOrder;
    private String validationRules;
    private String fieldConfig;
    private String defaultValue;

    public static FieldResponse from(FormField field) {
        return FieldResponse.builder()
                .id(field.getId())
                .fieldKey(field.getFieldKey())
                .fieldType(field.getFieldType())
                .label(field.getLabel())
                .placeholder(field.getPlaceholder())
                .helpText(field.getHelpText())
                .isRequired(field.getIsRequired())
                .displayOrder(field.getDisplayOrder())
                .validationRules(field.getValidationRules())
                .fieldConfig(field.getFieldConfig())
                .defaultValue(field.getDefaultValue())
                .build();
    }
}
