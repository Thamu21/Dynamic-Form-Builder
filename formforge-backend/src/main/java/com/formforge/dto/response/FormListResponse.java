package com.formforge.dto.response;

import com.formforge.entity.Form;
import com.formforge.entity.enums.FormStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormListResponse {
    private Long id;
    private String title;
    private String description;
    private String slug;
    private FormStatus status;
    private Integer version;
    private Integer fieldCount;
    private Long responseCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FormListResponse from(Form form, Long responseCount) {
        return FormListResponse.builder()
                .id(form.getId())
                .title(form.getTitle())
                .description(form.getDescription())
                .slug(form.getSlug())
                .status(form.getStatus())
                .version(form.getVersion())
                .fieldCount(form.getFields() != null
                        ? (int) form.getFields().stream().filter(f -> !f.getIsDeleted()).count()
                        : 0)
                .responseCount(responseCount)
                .publishedAt(form.getPublishedAt())
                .createdAt(form.getCreatedAt())
                .updatedAt(form.getUpdatedAt())
                .build();
    }
}
