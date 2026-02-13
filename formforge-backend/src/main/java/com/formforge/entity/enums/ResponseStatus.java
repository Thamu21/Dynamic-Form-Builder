package com.formforge.entity.enums;

/**
 * Response submission statuses.
 * PENDING: Partially submitted (for multi-page forms, future)
 * COMPLETED: Successfully submitted
 * INVALID: Failed validation (stored for debugging)
 */
public enum ResponseStatus {
    PENDING,
    COMPLETED,
    INVALID
}
