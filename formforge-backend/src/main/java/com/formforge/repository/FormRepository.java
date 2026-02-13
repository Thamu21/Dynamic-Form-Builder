package com.formforge.repository;

import com.formforge.entity.Form;
import com.formforge.entity.enums.FormStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormRepository extends JpaRepository<Form, Long> {

    // Find published version by slug
    Optional<Form> findBySlugAndStatusAndIsDeletedFalse(String slug, FormStatus status);

    // Find latest draft for a group
    Optional<Form> findByFormGroupIdAndStatusAndIsDeletedFalse(String formGroupId, FormStatus status);

    // Find max version for a group
    @Query("SELECT MAX(f.version) FROM Form f WHERE f.formGroupId = :formGroupId")
    Integer findMaxVersionByGroupId(String formGroupId);

    // Standard lookups
    Page<Form> findByCreatorIdAndIsDeletedFalse(Long creatorId, Pageable pageable);

    Page<Form> findByCreatorIdAndStatusAndIsDeletedFalse(Long creatorId, FormStatus status, Pageable pageable);

    @Query("SELECT f FROM Form f LEFT JOIN FETCH f.fields WHERE f.id = :id")
    Optional<Form> findByIdWithFields(Long id);

    Optional<Form> findByIdAndCreatorIdAndIsDeletedFalse(Long id, Long creatorId);

    // Check if slug exists (for any version)
    boolean existsBySlug(String slug);
}
