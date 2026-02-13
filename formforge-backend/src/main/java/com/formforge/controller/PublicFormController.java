package com.formforge.controller;

import com.formforge.dto.request.SubmitResponseRequest;
import com.formforge.dto.response.PublicFormResponse;
import com.formforge.dto.response.SubmissionSuccessResponse;
import com.formforge.service.ResponseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/forms")
@RequiredArgsConstructor
public class PublicFormController {

    private final ResponseService responseService;

    @GetMapping("/{slug}")
    public ResponseEntity<PublicFormResponse> getPublicForm(@PathVariable String slug) {
        PublicFormResponse response = responseService.getPublicForm(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/submit")
    public ResponseEntity<SubmissionSuccessResponse> submitResponse(
            @PathVariable String slug,
            @Valid @RequestBody SubmitResponseRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        SubmissionSuccessResponse response = responseService.submitResponse(slug, request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
