package com.formforge.entity.enums;

/**
 * Supported field types for dynamic form fields.
 * 
 * EXTENSIBILITY NOTES:
 * - New field types can be added without DB schema changes
 * - Each type maps to a specific value_* column in field_values:
 * TEXT, EMAIL, DROPDOWN -> value_text
 * NUMBER -> value_number
 * DATE -> value_date
 * CHECKBOX -> value_boolean
 * 
 * FUTURE TYPES (planned):
 * - FILE: For file upload support
 * - RATING: Star rating input
 * - SIGNATURE: Digital signature capture
 * - PHONE: Phone number with validation
 */
public enum FieldType {
    TEXT,
    NUMBER,
    DATE,
    DROPDOWN,
    CHECKBOX,
    EMAIL,
    TEXTAREA,
    RADIO
}
