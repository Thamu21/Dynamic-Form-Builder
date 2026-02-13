package com.formforge.repository;

import com.formforge.entity.FormResponse;
import com.formforge.entity.enums.ResponseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FormResponseRepository extends JpaRepository<FormResponse, Long> {

    /**
     * Find all responses for a form with pagination.
     * Uses idx_responses_form_date index.
     */
    Page<FormResponse> findByFormIdOrderBySubmittedAtDesc(Long formId, Pageable pageable);

    /**
     * Find responses filtered by status.
     * Uses idx_responses_form_status index.
     */
    Page<FormResponse> findByFormIdAndStatusOrderBySubmittedAtDesc(
            Long formId,
            ResponseStatus status,
            Pageable pageable);

    /**
     * Find responses in date range (for filtering).
     */
    @Query("SELECT r FROM FormResponse r WHERE r.form.id = :formId " +
            "AND r.submittedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY r.submittedAt DESC")
    Page<FormResponse> findByFormIdAndDateRange(
            @Param("formId") Long formId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find single response by ID and form ID (ownership check).
     */
    Optional<FormResponse> findByIdAndFormId(Long id, Long formId);

    /**
     * Get all responses for CSV export (no pagination).
     * Uses response_json for fast export.
     */
    @Query("SELECT r FROM FormResponse r WHERE r.form.id = :formId ORDER BY r.submittedAt DESC")
    List<FormResponse> findAllByFormIdForExport(@Param("formId") Long formId);

    /**
     * Count responses for dashboard stats.
     */
    long countByFormId(Long formId);

    /**
     * Count responses by IP for rate limiting (DB-backed alternative).
     */
    @Query("SELECT COUNT(r) FROM FormResponse r WHERE r.form.id = :formId " +
            "AND r.submissionIp = :ip AND r.submittedAt > :since")
    long countByFormIdAndIpSince(
            @Param("formId") Long formId,
            @Param("ip") String ip,
            @Param("since") LocalDateTime since);
}
