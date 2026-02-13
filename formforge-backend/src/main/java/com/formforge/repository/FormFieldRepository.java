package com.formforge.repository;

import com.formforge.entity.FormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormFieldRepository extends JpaRepository<FormField, Long> {

    /**
     * Find all active fields for a form, ordered by display order.
     * Uses idx_fields_form_order index.
     */
    List<FormField> findByFormIdAndIsDeletedFalseOrderByDisplayOrderAsc(Long formId);

    /**
     * Find field by ID and form ID for ownership validation.
     */
    Optional<FormField> findByIdAndFormIdAndIsDeletedFalse(Long id, Long formId);

    /**
     * Find max display order for new field insertion.
     */
    @Query("SELECT COALESCE(MAX(f.displayOrder), 0) FROM FormField f WHERE f.form.id = :formId AND f.isDeleted = false")
    Integer findMaxDisplayOrderByFormId(@Param("formId") Long formId);

    /**
     * Bulk update display orders for reordering.
     */
    @Modifying
    @Query("UPDATE FormField f SET f.displayOrder = :order WHERE f.id = :id")
    void updateDisplayOrder(@Param("id") Long id, @Param("order") Integer order);

    /**
     * Soft delete field.
     */
    @Modifying
    @Query("UPDATE FormField f SET f.isDeleted = true WHERE f.id = :id")
    void softDeleteById(@Param("id") Long id);

    /**
     * Check if field key exists in form (for uniqueness).
     */
    boolean existsByFormIdAndFieldKeyAndIsDeletedFalse(Long formId, String fieldKey);
}
