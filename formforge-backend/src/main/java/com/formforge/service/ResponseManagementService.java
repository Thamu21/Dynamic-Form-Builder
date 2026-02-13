package com.formforge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formforge.dto.response.PagedResponse;
import com.formforge.dto.response.SubmissionListResponse;
import com.formforge.entity.Form;
import com.formforge.entity.FormResponse;
import com.formforge.exception.ResourceNotFoundException;
import com.formforge.exception.UnauthorizedException;
import com.formforge.repository.FormRepository;
import com.formforge.repository.FormResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseManagementService {

    private final FormRepository formRepository;
    private final FormResponseRepository responseRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PagedResponse<SubmissionListResponse> getResponses(Long formId, Long userId, Pageable pageable) {
        verifyFormOwnership(formId, userId);

        Page<FormResponse> responses = responseRepository.findByFormIdOrderBySubmittedAtDesc(formId, pageable);

        return PagedResponse.from(responses, SubmissionListResponse::from);
    }

    @Transactional(readOnly = true)
    public SubmissionListResponse getResponse(Long formId, Long responseId, Long userId) {
        verifyFormOwnership(formId, userId);

        FormResponse response = responseRepository.findByIdAndFormId(responseId, formId)
                .orElseThrow(() -> new ResourceNotFoundException("Response", responseId));

        return SubmissionListResponse.from(response);
    }

    @Transactional
    public void deleteResponse(Long formId, Long responseId, Long userId) {
        verifyFormOwnership(formId, userId);

        FormResponse response = responseRepository.findByIdAndFormId(responseId, formId)
                .orElseThrow(() -> new ResourceNotFoundException("Response", responseId));

        responseRepository.delete(response);
        log.info("Response deleted: {} from form {}", responseId, formId);
    }

    /**
     * EXPORT TO CSV:
     * Uses hybrid storage's response_json for fast export (no JOINs needed).
     * This is 10-100x faster than reconstructing from EAV table.
     */
    @Transactional(readOnly = true)
    public String exportToCsv(Long formId, Long userId) {
        Form form = verifyFormOwnership(formId, userId);

        List<FormResponse> responses = responseRepository.findAllByFormIdForExport(formId);

        if (responses.isEmpty()) {
            return "No responses";
        }

        try {
            // Get all unique field keys from first response's schema snapshot
            Set<String> allFieldKeys = new LinkedHashSet<>();
            allFieldKeys.add("submittedAt");
            allFieldKeys.add("submissionIp");

            // Parse schema from first response to get field order
            String schemaJson = responses.get(0).getFormSchemaSnapshot();
            List<Map<String, Object>> schema = objectMapper.readValue(
                    schemaJson, new TypeReference<List<Map<String, Object>>>() {
                    });
            for (Map<String, Object> field : schema) {
                allFieldKeys.add((String) field.get("fieldKey"));
            }

            StringWriter writer = new StringWriter();

            // Header row
            writer.write(String.join(",", allFieldKeys));
            writer.write("\n");

            // Data rows
            for (FormResponse response : responses) {
                Map<String, String> values = objectMapper.readValue(
                        response.getResponseJson(), new TypeReference<Map<String, String>>() {
                        });

                List<String> row = new ArrayList<>();
                for (String key : allFieldKeys) {
                    if ("submittedAt".equals(key)) {
                        row.add(escapeCsv(response.getSubmittedAt().toString()));
                    } else if ("submissionIp".equals(key)) {
                        row.add(escapeCsv(response.getSubmissionIp()));
                    } else {
                        row.add(escapeCsv(values.getOrDefault(key, "")));
                    }
                }
                writer.write(String.join(",", row));
                writer.write("\n");
            }

            return writer.toString();
        } catch (IOException e) {
            log.error("Failed to export CSV", e);
            throw new RuntimeException("Failed to export responses");
        }
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private Form verifyFormOwnership(Long formId, Long userId) {
        Form form = formRepository.findByIdAndCreatorIdAndIsDeletedFalse(formId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Form", formId));

        if (!form.getCreator().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this form");
        }

        return form;
    }
}
