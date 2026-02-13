package com.formforge.controller;

import com.formforge.dto.request.CreateFieldRequest;
import com.formforge.dto.request.ReorderFieldsRequest;
import com.formforge.dto.request.UpdateFieldRequest;
import com.formforge.dto.response.FieldResponse;
import com.formforge.security.SecurityUser;
import com.formforge.service.FormFieldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forms/{formId}/fields")
@RequiredArgsConstructor
public class FormFieldController {

    private final FormFieldService fieldService;

    @GetMapping
    public ResponseEntity<List<FieldResponse>> getFields(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long formId) {

        List<FieldResponse> response = fieldService.getFields(formId, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<FieldResponse> createField(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long formId,
            @Valid @RequestBody CreateFieldRequest request) {

        FieldResponse response = fieldService.createField(formId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{fieldId}")
    public ResponseEntity<FieldResponse> updateField(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long formId,
            @PathVariable Long fieldId,
            @Valid @RequestBody UpdateFieldRequest request) {

        FieldResponse response = fieldService.updateField(formId, fieldId, user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{fieldId}")
    public ResponseEntity<Void> deleteField(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long formId,
            @PathVariable Long fieldId) {

        fieldService.deleteField(formId, fieldId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<FieldResponse>> reorderFields(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long formId,
            @Valid @RequestBody ReorderFieldsRequest request) {

        List<FieldResponse> response = fieldService.reorderFields(formId, user.getId(), request);
        return ResponseEntity.ok(response);
    }
}
