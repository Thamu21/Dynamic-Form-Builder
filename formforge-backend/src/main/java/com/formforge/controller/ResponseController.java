package com.formforge.controller;

import com.formforge.dto.response.PagedResponse;
import com.formforge.dto.response.SubmissionListResponse;
import com.formforge.security.SecurityUser;
import com.formforge.service.ResponseManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forms/{formId}/responses")
@RequiredArgsConstructor
public class ResponseController {

    private final ResponseManagementService responseManagementService;

    @GetMapping
    public ResponseEntity<PagedResponse<SubmissionListResponse>> getResponses(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long formId,
            @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PagedResponse<SubmissionListResponse> response = responseManagementService.getResponses(formId, user.getId(),
                pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{responseId}")
    public ResponseEntity<SubmissionListResponse> getResponse(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long formId,
            @PathVariable Long responseId) {

        SubmissionListResponse response = responseManagementService.getResponse(formId, responseId, user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{responseId}")
    public ResponseEntity<Void> deleteResponse(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long formId,
            @PathVariable Long responseId) {

        responseManagementService.deleteResponse(formId, responseId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportToCsv(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long formId) {

        String csv = responseManagementService.exportToCsv(formId, user.getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"responses.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
