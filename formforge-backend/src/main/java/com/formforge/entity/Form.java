package com.formforge.entity;

import com.formforge.entity.enums.FormStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forms", indexes = {
        @Index(name = "idx_forms_slug_status", columnList = "slug, status"),
        @Index(name = "idx_forms_creator_status", columnList = "creator_id, status, is_deleted"),
        @Index(name = "idx_forms_created", columnList = "created_at"),
        @Index(name = "idx_forms_group_version", columnList = "form_group_id, version")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_form_group_version", columnNames = { "form_group_id", "version" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Form {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID to link all versions of the same form together
    @Column(name = "form_group_id", nullable = false, length = 36)
    private String formGroupId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Slug is shared across versions (identifies the form publicly)
    // NOT unique at DB level anymore because multiple versions share it
    // Application logic ensures only ONE "PUBLISHED" form has this slug
    @Column(nullable = false, length = 100)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FormStatus status = FormStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * Form-level settings stored as JSON.
     * Examples: theme, showProgressBar, submitButtonText, notifications
     */
    @Column(columnDefinition = "JSON")
    private String settings;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<FormField> fields = new ArrayList<>();

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FormResponse> responses = new ArrayList<>();

    // Helper methods
    public void addField(FormField field) {
        fields.add(field);
        field.setForm(this);
    }

    public void removeField(FormField field) {
        fields.remove(field);
        field.setForm(null);
    }
}
