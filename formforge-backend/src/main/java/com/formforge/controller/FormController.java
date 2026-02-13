package com.formforge.controller;

import com.formforge.dto.request.CreateFormRequest;
import com.formforge.dto.request.UpdateFormRequest;
import com.formforge.dto.response.FormDetailResponse;
import com.formforge.dto.response.FormListResponse;
import com.formforge.dto.response.PagedResponse;
import com.formforge.entity.enums.FormStatus;
import com.formforge.security.SecurityUser;
import com.formforge.service.FormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

    @GetMapping
    public ResponseEntity<PagedResponse<FormListResponse>> getMyForms(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(required = false) FormStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PagedResponse<FormListResponse> response = formService.getMyForms(user.getId(), status, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<FormDetailResponse> createForm(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody CreateFormRequest request) {

        FormDetailResponse response = formService.createForm(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/draft")
    public ResponseEntity<FormDetailResponse> createDraft(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id) {

        FormDetailResponse response = formService.createDraft(id, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormDetailResponse> getForm(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id) {

        FormDetailResponse response = formService.getForm(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormDetailResponse> updateForm(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateFormRequest request) {

        FormDetailResponse response = formService.updateForm(id, user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForm(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id) {

        formService.deleteForm(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<FormDetailResponse> publishForm(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id) {

        FormDetailResponse response = formService.publishForm(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<FormDetailResponse> archiveForm(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id) {

        FormDetailResponse response = formService.archiveForm(id, user.getId());
        return ResponseEntity.ok(response);
    }
}
