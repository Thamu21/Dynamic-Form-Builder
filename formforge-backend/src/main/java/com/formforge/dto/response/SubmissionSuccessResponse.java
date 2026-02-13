package com.formforge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionSuccessResponse {
    private String message;
    private Long responseId;

    public static SubmissionSuccessResponse success(Long responseId) {
        return SubmissionSuccessResponse.builder()
                .message("Response submitted successfully")
                .responseId(responseId)
                .build();
    }
}
