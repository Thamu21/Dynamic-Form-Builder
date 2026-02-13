package com.formforge.entity;

import com.formforge.entity.enums.FieldType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_fields", indexes = {
        @Index(name = "idx_fields_form_order", columnList = "form_id, display_order"),
        @Index(name = "idx_fields_form_active", columnList = "form_id, is_deleted")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_form_field_key", columnNames = { "form_id", "field_key" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    /**
     * Unique identifier within the form, used for submission keys.
     * Example: "email", "firstName", "rating"
     */
    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 50)
    private FieldType fieldType;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(length = 255)
    private String placeholder;

    @Column(name = "help_text", columnDefinition = "TEXT")
    private String helpText;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * Validation rules stored as JSON.
     * Examples:
     * - TEXT: {"minLength": 3, "maxLength": 100, "pattern": "^[a-zA-Z]+$"}
     * - NUMBER: {"min": 0, "max": 100}
     * - EMAIL: {"pattern": "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$"}
     */
    @Column(name = "validation_rules", columnDefinition = "JSON")
    private String validationRules;

    /**
     * Field-specific configuration stored as JSON.
     * Examples:
     * - DROPDOWN: {"options": [{"value": "1", "label": "Option 1"}, ...]}
     * - CHECKBOX: {"checkedValue": "yes", "uncheckedValue": "no"}
     * - RADIO: {"options": [{"value": "a", "label": "A"}, ...]}
     */
    @Column(name = "field_config", columnDefinition = "JSON")
    private String fieldConfig;

    /**
     * Conditional display logic stored as JSON.
     * Future feature: show/hide field based on other field values.
     * Example: {"dependsOn": "country", "condition": "equals", "value": "USA"}
     */
    @Column(name = "display_conditions", columnDefinition = "JSON")
    private String displayConditions;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FieldValue> values = new ArrayList<>();
}
