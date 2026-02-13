package com.formforge.dto.response;

import com.formforge.entity.Form;
import com.formforge.entity.enums.FormStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String slug;
    private FormStatus status;
    private Integer version;
    private String settings;
    private List<FieldResponse> fields;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FormDetailResponse from(Form form) {
        return FormDetailResponse.builder()
                .id(form.getId())
                .title(form.getTitle())
                .description(form.getDescription())
                .slug(form.getSlug())
                .status(form.getStatus())
                .version(form.getVersion())
                .settings(form.getSettings())
                .fields(form.getFields() != null ? form.getFields().stream()
                        .filter(f -> !f.getIsDeleted())
                        .map(FieldResponse::from)
                        .collect(Collectors.toList()) : List.of())
                .publishedAt(form.getPublishedAt())
                .createdAt(form.getCreatedAt())
                .updatedAt(form.getUpdatedAt())
                .build();
    }
}
