package com.formforge.service;

import com.formforge.dto.request.CreateFormRequest;
import com.formforge.dto.request.UpdateFormRequest;
import com.formforge.dto.response.FormDetailResponse;
import com.formforge.dto.response.FormListResponse;
import com.formforge.dto.response.PagedResponse;
import com.formforge.entity.Form;
import com.formforge.entity.FormField;
import com.formforge.entity.enums.FormStatus;
import com.formforge.exception.ResourceNotFoundException;
import com.formforge.exception.UnauthorizedException;
import com.formforge.repository.FormRepository;
import com.formforge.repository.FormResponseRepository;
import com.formforge.repository.UserRepository;
import com.formforge.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormService {

    private final FormRepository formRepository;
    private final FormResponseRepository responseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<FormListResponse> getMyForms(Long userId, FormStatus status, Pageable pageable) {
        Page<Form> forms;
        if (status != null) {
            forms = formRepository.findByCreatorIdAndStatusAndIsDeletedFalse(userId, status, pageable);
        } else {
            // TODO: Ideally group by formGroupId and show latest, but for now show all
            // non-deleted
            forms = formRepository.findByCreatorIdAndIsDeletedFalse(userId, pageable);
        }

        return PagedResponse.from(forms, form -> {
            // Count responses for the *Group* or just this version?
            // User likely wants total responses for the form concept.
            // But responses are linked to specific versions.
            // Let's count for this version for now.
            long responseCount = responseRepository.countByFormId(form.getId());
            return FormListResponse.from(form, responseCount);
        });
    }

    @Transactional
    public FormDetailResponse createForm(Long userId, CreateFormRequest request) {
        Form form = Form.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .slug(SlugGenerator.generateSlug(request.getTitle()))
                .formGroupId(UUID.randomUUID().toString())
                .creator(userRepository.getReferenceById(userId))
                .status(FormStatus.DRAFT)
                .version(1)
                .settings(request.getSettings())
                .build();

        form = formRepository.save(form);
        log.info("Form created: {} (Group: {}) by user {}", form.getSlug(), form.getFormGroupId(), userId);

        return FormDetailResponse.from(form);
    }

    @Transactional
    public FormDetailResponse createDraft(Long formId, Long userId) {
        Form sourceForm = getFormAndVerifyOwnership(formId, userId);

        // Check if a draft already exists for this group logic?
        // If source is already draft, return it.
        if (sourceForm.getStatus() == FormStatus.DRAFT) {
            return FormDetailResponse.from(sourceForm);
        }

        // Check if a draft exists for this group (latest version)
        // If we have v1(Published) and v2(Draft) and user clicks edit on v1, we should
        // probably return v2?
        // Or create v3? No, usually v2.
        // Let's strict check:
        // We need to query if there is ANY draft with this groupId.
        /*
         * This logic is simplified. In a real app we'd find the latest draft.
         * But for now, if the user explicitly requests a draft from a published form,
         * we clone it.
         */

        // Deep clone logic
        Form newDraft = Form.builder()
                .title(sourceForm.getTitle())
                .description(sourceForm.getDescription())
                .slug(sourceForm.getSlug())
                .formGroupId(sourceForm.getFormGroupId())
                .creator(sourceForm.getCreator())
                .status(FormStatus.DRAFT)
                .version(sourceForm.getVersion() + 1)
                .settings(sourceForm.getSettings())
                .isDeleted(false)
                .build();

        final Form draftRef = newDraft;

        // Clone fields
        if (sourceForm.getFields() != null) {
            var newFields = sourceForm.getFields().stream()
                    .map(f -> FormField.builder()
                            .form(draftRef)
                            .fieldKey(f.getFieldKey())
                            .fieldType(f.getFieldType())
                            .label(f.getLabel())
                            .placeholder(f.getPlaceholder())
                            .helpText(f.getHelpText())
                            .isRequired(f.getIsRequired())
                            .displayOrder(f.getDisplayOrder())
                            .validationRules(f.getValidationRules())
                            .fieldConfig(f.getFieldConfig())
                            .defaultValue(f.getDefaultValue())
                            .isDeleted(false)
                            .build())
                    .collect(Collectors.toList());

            newDraft.setFields(newFields);
        }

        newDraft = formRepository.save(newDraft);
        log.info("Draft created: {} (v{}) from Form {}", newDraft.getSlug(), newDraft.getVersion(), formId);

        return FormDetailResponse.from(newDraft);
    }

    @Transactional(readOnly = true)
    public FormDetailResponse getForm(Long formId, Long userId) {
        Form form = getFormAndVerifyOwnership(formId, userId);
        return FormDetailResponse.from(form);
    }

    @Transactional
    public FormDetailResponse updateForm(Long formId, Long userId, UpdateFormRequest request) {
        Form form = getFormAndVerifyOwnership(formId, userId);

        if (form.getStatus() == FormStatus.PUBLISHED || form.getStatus() == FormStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Cannot edit a " + form.getStatus() + " form directly. Create a draft first.");
        }

        if (request.getTitle() != null) {
            form.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            form.setDescription(request.getDescription());
        }
        if (request.getSettings() != null) {
            form.setSettings(request.getSettings());
        }

        form = formRepository.save(form);
        log.info("Form updated: {}", form.getSlug());

        return FormDetailResponse.from(form);
    }

    @Transactional
    public void deleteForm(Long formId, Long userId) {
        Form form = getFormAndVerifyOwnership(formId, userId);
        form.setIsDeleted(true);
        formRepository.save(form);
        log.info("Form deleted: {}", form.getSlug());
    }

    @Transactional
    public FormDetailResponse publishForm(Long formId, Long userId) {
        Form draftForm = getFormAndVerifyOwnership(formId, userId);

        if (draftForm.getStatus() != FormStatus.DRAFT) {
            // idempotent if already published?
            if (draftForm.getStatus() == FormStatus.PUBLISHED)
                return FormDetailResponse.from(draftForm);
            throw new IllegalStateException("Only DRAFT forms can be published");
        }

        // Archive currently published version (if any)
        // Find by slug/groupId and status=PUBLISHED
        // Note: slug is shared, so finding by slug + status=PUBLISHED is safe
        Optional<Form> currentPublished = formRepository.findBySlugAndStatusAndIsDeletedFalse(
                draftForm.getSlug(), FormStatus.PUBLISHED);

        if (currentPublished.isPresent()) {
            Form published = currentPublished.get();
            // Ensure it's the same group (sanity check)
            if (!published.getFormGroupId().equals(draftForm.getFormGroupId())) {
                log.warn("Slug collision during publish? {} vs {}", published.getId(), draftForm.getId());
                // In real world, handle slug rename logic. For now, assume same group.
            }
            published.setStatus(FormStatus.ARCHIVED);
            formRepository.save(published);
        }

        draftForm.setStatus(FormStatus.PUBLISHED);
        draftForm.setPublishedAt(LocalDateTime.now());
        // Version is already set during creation

        draftForm = formRepository.save(draftForm);
        log.info("Form published: {} (v{})", draftForm.getSlug(), draftForm.getVersion());

        return FormDetailResponse.from(draftForm);
    }

    @Transactional
    public FormDetailResponse archiveForm(Long formId, Long userId) {
        Form form = getFormAndVerifyOwnership(formId, userId);
        form.setStatus(FormStatus.ARCHIVED);
        form = formRepository.save(form);
        log.info("Form archived: {}", form.getSlug());

        return FormDetailResponse.from(form);
    }

    private Form getFormAndVerifyOwnership(Long formId, Long userId) {
        Form form = formRepository.findByIdWithFields(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form", formId));

        if (!form.getCreator().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this form");
        }

        return form;
    }
}
