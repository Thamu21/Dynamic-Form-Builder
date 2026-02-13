package com.formforge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formforge.dto.request.SubmitResponseRequest;
import com.formforge.dto.response.PublicFormResponse;
import com.formforge.dto.response.SubmissionSuccessResponse;
import com.formforge.entity.FieldValue;
import com.formforge.entity.Form;
import com.formforge.entity.FormField;
import com.formforge.entity.FormResponse;
import com.formforge.entity.enums.FieldType;
import com.formforge.entity.enums.FormStatus;
import com.formforge.entity.enums.ResponseStatus;
import com.formforge.exception.ResourceNotFoundException;
import com.formforge.exception.ValidationException;
import com.formforge.repository.FormRepository;
import com.formforge.repository.FormResponseRepository;
import com.formforge.util.TypedValueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseService {

    private final FormRepository formRepository;
    private final FormResponseRepository responseRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get form for public rendering.
     */
    @Transactional(readOnly = true)
    public PublicFormResponse getPublicForm(String slug) {
        Form form = formRepository.findBySlugAndStatusAndIsDeletedFalse(slug, FormStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Form", "slug", slug));

        return PublicFormResponse.from(form);
    }

    /**
     * Submit response to a form.
     * 
     * TRANSACTIONAL INTEGRITY:
     * - Both response_json AND field_values are written in same transaction
     * - If any insert fails, entire submission is rolled back
     * - Spring @Transactional ensures atomicity
     */
    @Transactional
    public SubmissionSuccessResponse submitResponse(String slug, SubmitResponseRequest request, String clientIp) {
        // 1. Get and validate form
        Form form = formRepository.findBySlugAndStatusAndIsDeletedFalse(slug, FormStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Form", "slug", slug));

        // 2. Bot protection checks
        validateBotProtection(request);

        // 3. Server-side validation
        List<FormField> activeFields = form.getFields().stream()
                .filter(f -> !f.getIsDeleted())
                .collect(Collectors.toList());

        validateSubmission(activeFields, request.getValues());

        // 4. Create form schema snapshot
        String schemaSnapshot = createSchemaSnapshot(activeFields);

        // 5. Create response JSON for hybrid storage
        String responseJson = createResponseJson(request.getValues());

        // 6. Create form response entity
        FormResponse response = FormResponse.builder()
                .form(form)
                .submissionIp(clientIp)
                .status(ResponseStatus.COMPLETED)
                .formVersion(form.getVersion())
                .responseJson(responseJson)
                .formSchemaSnapshot(schemaSnapshot)
                .build();

        // 7. Create typed field values for EAV storage
        Map<String, FormField> fieldMap = activeFields.stream()
                .collect(Collectors.toMap(FormField::getFieldKey, f -> f));

        for (Map.Entry<String, String> entry : request.getValues().entrySet()) {
            FormField field = fieldMap.get(entry.getKey());
            if (field != null) {
                FieldValue fieldValue = createTypedFieldValue(field, entry.getValue());
                response.addFieldValue(fieldValue);
            }
        }

        // 8. Save (both response and field_values in same transaction)
        response = responseRepository.save(response);
        log.info("Response submitted: {} for form {}", response.getId(), slug);

        return SubmissionSuccessResponse.success(response.getId());
    }

    /**
     * BOT PROTECTION STRATEGIES:
     * 1. Honeypot field check (hidden field that should be empty)
     * 2. Timing check (too fast = bot)
     */
    private void validateBotProtection(SubmitResponseRequest request) {
        // Honeypot check
        if (request.getHoneypot() != null && !request.getHoneypot().isEmpty()) {
            log.warn("Bot detected: honeypot field populated");
            throw new ValidationException("Invalid submission");
        }

        // Timing check (minimum 2 seconds between load and submit)
        if (request.getLoadTimestamp() != null) {
            long elapsed = System.currentTimeMillis() - request.getLoadTimestamp();
            if (elapsed < 2000) {
                log.warn("Bot detected: submission too fast ({}ms)", elapsed);
                throw new ValidationException("Invalid submission");
            }
        }
    }

    /**
     * SERVER-SIDE VALIDATION:
     * - Never trust frontend validation
     * - Check required fields
     * - Validate field types (TODO: pattern validation)
     */
    private void validateSubmission(List<FormField> fields, Map<String, String> values) {
        Map<String, String> errors = new HashMap<>();

        for (FormField field : fields) {
            String value = values.get(field.getFieldKey());

            // Required check
            if (field.getIsRequired() && (value == null || value.trim().isEmpty())) {
                errors.put(field.getFieldKey(), field.getLabel() + " is required");
            }

            // Type-specific validation
            if (value != null && !value.isEmpty()) {
                switch (field.getFieldType()) {
                    case EMAIL:
                        if (!value.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                            errors.put(field.getFieldKey(), "Invalid email format");
                        }
                        break;
                    case NUMBER:
                        try {
                            Double.parseDouble(value);
                        } catch (NumberFormatException e) {
                            errors.put(field.getFieldKey(), "Must be a valid number");
                        }
                        break;
                    default:
                        // Additional validation based on validationRules JSON can be added here
                        break;
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }
    }

    /**
     * SCHEMA SNAPSHOTTING:
     * Captures form schema at submission time for historical accuracy.
     * 
     * Critical for:
     * - Historical accuracy (fields as they existed at submit)
     * - Data integrity (field deletion doesn't orphan data)
     * - Audit trail (compliance requirements)
     * - Analytics (compare across form versions)
     */
    private String createSchemaSnapshot(List<FormField> fields) {
        try {
            List<Map<String, Object>> snapshot = fields.stream()
                    .map(f -> {
                        Map<String, Object> fieldSnapshot = new HashMap<>();
                        fieldSnapshot.put("fieldKey", f.getFieldKey());
                        fieldSnapshot.put("fieldType", f.getFieldType().name());
                        fieldSnapshot.put("label", f.getLabel());
                        fieldSnapshot.put("isRequired", f.getIsRequired());
                        fieldSnapshot.put("validationRules", f.getValidationRules());
                        fieldSnapshot.put("fieldConfig", f.getFieldConfig());
                        return fieldSnapshot;
                    })
                    .collect(Collectors.toList());

            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Failed to create schema snapshot", e);
            return "[]";
        }
    }

    /**
     * HYBRID STORAGE: Create JSON representation for fast reads.
     */
    private String createResponseJson(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            log.error("Failed to create response JSON", e);
            return "{}";
        }
    }

    /**
     * TYPED VALUE MAPPING:
     * Maps string values to appropriate typed columns.
     */
    private FieldValue createTypedFieldValue(FormField field, String value) {
        FieldValue.FieldValueBuilder builder = FieldValue.builder()
                .field(field);

        TypedValueMapper.TypedValue typed = TypedValueMapper.mapValue(field.getFieldType(), value);

        if (typed != null) {
            if (typed.hasText()) {
                builder.valueText(typed.text());
            } else if (typed.hasNumber()) {
                builder.valueNumber(typed.number());
            } else if (typed.hasDate()) {
                builder.valueDate(typed.date());
            } else if (typed.hasBoolean()) {
                builder.valueBoolean(typed.bool());
            }
        }

        return builder.build();
    }
}
