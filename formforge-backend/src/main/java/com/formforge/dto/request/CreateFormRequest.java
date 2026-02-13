package com.formforge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFormRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be 1-255 characters")
    private String title;

    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    /**
     * Optional form settings as JSON string.
     * Example: {"theme": "light", "showProgressBar": true}
     */
    private String settings;
}
