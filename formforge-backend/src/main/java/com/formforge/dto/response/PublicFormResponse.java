package com.formforge.dto.response;

import com.formforge.entity.Form;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Public form response for rendering on frontend.
 * Contains only what's needed to display the form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicFormResponse {
    private String slug;
    private String title;
    private String description;
    private String settings;
    private List<PublicFieldResponse> fields;

    /**
     * Timestamp for bot detection (compare with submit time).
     */
    private Long loadTimestamp;

    public static PublicFormResponse from(Form form) {
        return PublicFormResponse.builder()
                .slug(form.getSlug())
                .title(form.getTitle())
                .description(form.getDescription())
                .settings(form.getSettings())
                .fields(form.getFields().stream()
                        .filter(f -> !f.getIsDeleted())
                        .map(PublicFieldResponse::from)
                        .collect(Collectors.toList()))
                .loadTimestamp(System.currentTimeMillis())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicFieldResponse {
        private String fieldKey;
        private String fieldType;
        private String label;
        private String placeholder;
        private String helpText;
        private Boolean isRequired;
        private Integer displayOrder;
        private String validationRules;
        private String fieldConfig;
        private String defaultValue;

        public static PublicFieldResponse from(com.formforge.entity.FormField field) {
            return PublicFieldResponse.builder()
                    .fieldKey(field.getFieldKey())
                    .fieldType(field.getFieldType().name())
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
}
