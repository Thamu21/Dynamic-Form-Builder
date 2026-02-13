package com.formforge.dto.response;

import com.formforge.entity.FormResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionListResponse {
    private Long id;
    private String responseJson;
    private String submissionIp;
    private Integer formVersion;
    private LocalDateTime submittedAt;

    public static SubmissionListResponse from(FormResponse response) {
        return SubmissionListResponse.builder()
                .id(response.getId())
                .responseJson(response.getResponseJson())
                .submissionIp(response.getSubmissionIp())
                .formVersion(response.getFormVersion())
                .submittedAt(response.getSubmittedAt())
                .build();
    }
}
