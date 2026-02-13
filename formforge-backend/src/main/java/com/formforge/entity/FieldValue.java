package com.formforge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "field_values", indexes = {
        @Index(name = "idx_values_response", columnList = "response_id"),
        /**
         * COMPOSITE INDEX ANALYSIS:
         * 
         * idx_values_field_text (field_id, value_text):
         * - Query: Filter responses where email field contains 'gmail'
         * - Composite enables: WHERE field_id = ? AND value_text LIKE ?
         * - Tradeoff: Higher write cost, faster filtered reads
         */
        @Index(name = "idx_values_field_text", columnList = "field_id, value_text"),
        /**
         * idx_values_field_number (field_id, value_number):
         * - Query: Filter responses where age > 25
         * - Range scans optimized for numeric comparisons
         * - Tradeoff: ~8 bytes per row, enables fast aggregations
         */
        @Index(name = "idx_values_field_number", columnList = "field_id, value_number"),
        /**
         * idx_values_field_date (field_id, value_date):
         * - Query: Filter responses where date is in 2026
         * - Essential for date-range reporting
         */
        @Index(name = "idx_values_field_date", columnList = "field_id, value_date"),
        @Index(name = "idx_values_field_bool", columnList = "field_id, value_boolean")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_response_field", columnNames = { "response_id", "field_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false)
    private FormResponse response;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private FormField field;

    /**
     * TYPED VALUE COLUMNS:
     * Only ONE column should be populated per row based on field type.
     * 
     * Why typed storage improves:
     * 
     * 1. QUERY PERFORMANCE
     * - Numeric comparisons: value_number > 100 (no CAST overhead)
     * - Date ranges: value_date BETWEEN ...
     * - Native operators are 2-10x faster than string parsing
     * 
     * 2. SORTING
     * - Numbers sort numerically: 2, 10, 100 (not "10", "100", "2")
     * - Dates sort chronologically
     * - No implicit conversion bugs
     * 
     * 3. INDEXING
     * - B-tree indexes work efficiently on native types
     * - Range scans on numbers/dates are optimized
     * - Composite indexes enable fast aggregations
     * 
     * 4. DATA INTEGRITY
     * - Database enforces type constraints
     * - No invalid data (e.g., "abc" in number field)
     */

    /**
     * For TEXT, EMAIL, TEXTAREA, DROPDOWN (stored value), RADIO
     */
    @Column(name = "value_text", columnDefinition = "TEXT")
    private String valueText;

    /**
     * For NUMBER field type
     */
    @Column(name = "value_number")
    private Double valueNumber;

    /**
     * For DATE field type
     */
    @Column(name = "value_date")
    private LocalDateTime valueDate;

    /**
     * For CHECKBOX field type
     */
    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Validates that exactly one value column is populated.
     * Called before persist to maintain data integrity.
     */
    @PrePersist
    @PreUpdate
    public void validateSingleValue() {
        int populatedCount = 0;
        if (valueText != null)
            populatedCount++;
        if (valueNumber != null)
            populatedCount++;
        if (valueDate != null)
            populatedCount++;
        if (valueBoolean != null)
            populatedCount++;

        if (populatedCount != 1) {
            throw new IllegalStateException(
                    "Exactly one value column must be populated. Found: " + populatedCount);
        }
    }
}
