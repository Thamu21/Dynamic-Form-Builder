package com.formforge.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitResponseRequest {

    /**
     * Form field values as key-value pairs.
     * Key = fieldKey, Value = submitted value (as string)
     * 
     * Example:
     * {
     * "email": "user@example.com",
     * "age": "25",
     * "subscribe": "true",
     * "birthDate": "2000-01-15"
     * }
     */
    @NotNull(message = "Values are required")
    @Size(min = 1, message = "At least one field value is required")
    private Map<String, String> values;

    /**
     * Honeypot field for bot protection.
     * Should be empty for legitimate submissions.
     */
    private String honeypot;

    /**
     * Timestamp when form was loaded (for timing-based bot detection).
     * Submissions faster than 2 seconds are flagged.
     */
    private Long loadTimestamp;
}
