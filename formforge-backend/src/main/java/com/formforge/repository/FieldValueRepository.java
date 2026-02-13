package com.formforge.repository;

import com.formforge.entity.FieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldValueRepository extends JpaRepository<FieldValue, Long> {

    /**
     * Find all values for a response (for response detail view).
     * Uses idx_values_response index.
     */
    List<FieldValue> findByResponseId(Long responseId);

    /**
     * Find values for a specific field across responses (for filtering).
     * Uses composite indexes (field_id, value_*).
     */
    @Query("SELECT fv FROM FieldValue fv WHERE fv.field.id = :fieldId AND fv.valueText LIKE :value")
    List<FieldValue> findByFieldIdAndTextContaining(
            @Param("fieldId") Long fieldId,
            @Param("value") String value);

    /**
     * Find numeric values in range (for numeric filtering).
     * Uses idx_values_field_number index.
     */
    @Query("SELECT fv FROM FieldValue fv WHERE fv.field.id = :fieldId " +
            "AND fv.valueNumber BETWEEN :min AND :max")
    List<FieldValue> findByFieldIdAndNumberRange(
            @Param("fieldId") Long fieldId,
            @Param("min") Double min,
            @Param("max") Double max);
}
