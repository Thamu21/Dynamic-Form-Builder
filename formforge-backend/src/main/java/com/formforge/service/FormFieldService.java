package com.formforge.service;

import com.formforge.dto.request.CreateFieldRequest;
import com.formforge.dto.request.ReorderFieldsRequest;
import com.formforge.dto.request.UpdateFieldRequest;
import com.formforge.dto.response.FieldResponse;
import com.formforge.entity.Form;
import com.formforge.entity.FormField;
import com.formforge.entity.enums.FormStatus;
import com.formforge.exception.DuplicateResourceException;
import com.formforge.exception.ResourceNotFoundException;
import com.formforge.exception.UnauthorizedException;
import com.formforge.repository.FormFieldRepository;
import com.formforge.repository.FormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormFieldService {

    private final FormFieldRepository fieldRepository;
    private final FormRepository formRepository;

    @Transactional(readOnly = true)
    public List<FieldResponse> getFields(Long formId, Long userId) {
        verifyFormOwnership(formId, userId);

        return fieldRepository.findByFormIdAndIsDeletedFalseOrderByDisplayOrderAsc(formId)
                .stream()
                .map(FieldResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public FieldResponse createField(Long formId, Long userId, CreateFieldRequest request) {
        Form form = verifyFormAndEditable(formId, userId);

        // Check for duplicate field key
        if (fieldRepository.existsByFormIdAndFieldKeyAndIsDeletedFalse(formId, request.getFieldKey())) {
            throw new DuplicateResourceException("Field", "fieldKey", request.getFieldKey());
        }

        // Get next display order
        Integer maxOrder = fieldRepository.findMaxDisplayOrderByFormId(formId);
        int nextOrder = maxOrder + 1;

        FormField field = FormField.builder()
                .form(form)
                .fieldKey(request.getFieldKey())
                .fieldType(request.getFieldType())
                .label(request.getLabel())
                .placeholder(request.getPlaceholder())
                .helpText(request.getHelpText())
                .isRequired(request.getIsRequired())
                .displayOrder(nextOrder)
                .validationRules(request.getValidationRules())
                .fieldConfig(request.getFieldConfig())
                .defaultValue(request.getDefaultValue())
                .build();

        field = fieldRepository.save(field);
        log.info("Field created: {} for form {}", field.getFieldKey(), formId);

        return FieldResponse.from(field);
    }

    @Transactional
    public FieldResponse updateField(Long formId, Long fieldId, Long userId, UpdateFieldRequest request) {
        verifyFormAndEditable(formId, userId);

        FormField field = fieldRepository.findByIdAndFormIdAndIsDeletedFalse(fieldId, formId)
                .orElseThrow(() -> new ResourceNotFoundException("Field", fieldId));

        if (request.getFieldType() != null) {
            field.setFieldType(request.getFieldType());
        }
        if (request.getLabel() != null) {
            field.setLabel(request.getLabel());
        }
        if (request.getPlaceholder() != null) {
            field.setPlaceholder(request.getPlaceholder());
        }
        if (request.getHelpText() != null) {
            field.setHelpText(request.getHelpText());
        }
        if (request.getIsRequired() != null) {
            field.setIsRequired(request.getIsRequired());
        }
        if (request.getValidationRules() != null) {
            field.setValidationRules(request.getValidationRules());
        }
        if (request.getFieldConfig() != null) {
            field.setFieldConfig(request.getFieldConfig());
        }
        if (request.getDefaultValue() != null) {
            field.setDefaultValue(request.getDefaultValue());
        }

        field = fieldRepository.save(field);
        log.info("Field updated: {}", field.getFieldKey());

        return FieldResponse.from(field);
    }

    @Transactional
    public void deleteField(Long formId, Long fieldId, Long userId) {
        verifyFormAndEditable(formId, userId);

        FormField field = fieldRepository.findByIdAndFormIdAndIsDeletedFalse(fieldId, formId)
                .orElseThrow(() -> new ResourceNotFoundException("Field", fieldId));

        fieldRepository.softDeleteById(fieldId);
        log.info("Field deleted: {}", field.getFieldKey());
    }

    @Transactional
    public List<FieldResponse> reorderFields(Long formId, Long userId, ReorderFieldsRequest request) {
        verifyFormAndEditable(formId, userId);

        for (ReorderFieldsRequest.FieldOrderItem item : request.getFieldOrder()) {
            fieldRepository.updateDisplayOrder(item.getFieldId(), item.getDisplayOrder());
        }

        log.info("Fields reordered for form {}", formId);

        return fieldRepository.findByFormIdAndIsDeletedFalseOrderByDisplayOrderAsc(formId)
                .stream()
                .map(FieldResponse::from)
                .collect(Collectors.toList());
    }

    private Form verifyFormOwnership(Long formId, Long userId) {
        Form form = formRepository.findByIdAndCreatorIdAndIsDeletedFalse(formId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Form", formId));

        if (!form.getCreator().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this form");
        }

        return form;
    }

    private Form verifyFormAndEditable(Long formId, Long userId) {
        Form form = verifyFormOwnership(formId, userId);

        if (form.getStatus() == FormStatus.PUBLISHED || form.getStatus() == FormStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Cannot edit a " + form.getStatus() + " form. Please create a draft first.");
        }

        return form;
    }
}
