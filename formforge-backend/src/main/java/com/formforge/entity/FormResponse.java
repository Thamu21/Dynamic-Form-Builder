package com.formforge.entity;

import com.formforge.entity.enums.ResponseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_responses", indexes = {
        @Index(name = "idx_responses_form_date", columnList = "form_id, submitted_at"),
        @Index(name = "idx_responses_submitted", columnList = "submitted_at"),
        @Index(name = "idx_responses_form_status", columnList = "form_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    /**
     * Nullable for anonymous submissions.
     * Populated when authenticated user submits.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondent_id")
    private User respondent;

    @Column(name = "submission_ip", length = 45)
    private String submissionIp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ResponseStatus status = ResponseStatus.COMPLETED;

    /**
     * Snapshot of form version at submission time.
     * Preserves which version of the form was submitted.
     */
    @Column(name = "form_version", nullable = false)
    private Integer formVersion;

    /**
     * HYBRID STORAGE: Full JSON payload for fast reads/exports.
     * 
     * READ PERFORMANCE: O(1) fetch for entire submission without JOINs.
     * DEBUGGING: JSON shows exactly what was submitted.
     * ANALYTICS: Aggregate functions for dashboards.
     * EXPORT: CSV/Excel export reads JSON directly (10-100x faster).
     * 
     * Structure: {"fieldKey1": "value1", "fieldKey2": "value2", ...}
     */
    @Column(name = "response_json", columnDefinition = "JSON", nullable = false)
    private String responseJson;

    /**
     * SCHEMA SNAPSHOTTING: Captures form schema at submission time.
     * 
     * CRITICAL FOR PRODUCTION:
     * - Historical accuracy: Shows fields as they existed at submit time
     * - Data integrity: Field deletion doesn't orphan response data
     * - Audit trail: Required for compliance (GDPR, HIPAA)
     * - Analytics: Compare responses across form versions
     * 
     * Structure: [{"fieldKey": "...", "label": "...", "type": "...", ...}, ...]
     */
    @Column(name = "form_schema_snapshot", columnDefinition = "JSON", nullable = false)
    private String formSchemaSnapshot;

    @Column(name = "submitted_at")
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<FieldValue> fieldValues = new ArrayList<>();

    // Helper methods
    public void addFieldValue(FieldValue fieldValue) {
        fieldValues.add(fieldValue);
        fieldValue.setResponse(this);
    }
}
