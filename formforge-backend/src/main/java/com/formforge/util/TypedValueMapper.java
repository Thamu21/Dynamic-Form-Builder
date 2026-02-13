package com.formforge.util;

import com.formforge.entity.enums.FieldType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Maps string values to typed values based on field type.
 * Used when storing submissions in typed columns.
 */
public class TypedValueMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Determines which typed column to use for a field type.
     */
    public static TypedValue mapValue(FieldType fieldType, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return switch (fieldType) {
            case TEXT, EMAIL, TEXTAREA, DROPDOWN, RADIO -> new TypedValue(value, null, null, null);
            case NUMBER -> parseNumber(value);
            case DATE -> parseDate(value);
            case CHECKBOX -> parseBoolean(value);
        };
    }

    private static TypedValue parseNumber(String value) {
        try {
            Double number = Double.parseDouble(value.trim());
            return new TypedValue(null, number, null, null);
        } catch (NumberFormatException e) {
            // Store as text if not parseable
            return new TypedValue(value, null, null, null);
        }
    }

    private static TypedValue parseDate(String value) {
        try {
            // Try ISO date format first
            LocalDateTime date = LocalDateTime.parse(value.trim(), DATETIME_FORMATTER);
            return new TypedValue(null, null, date, null);
        } catch (DateTimeParseException e) {
            try {
                // Try date-only format
                LocalDateTime date = LocalDateTime.parse(value.trim() + "T00:00:00", DATETIME_FORMATTER);
                return new TypedValue(null, null, date, null);
            } catch (DateTimeParseException e2) {
                // Store as text if not parseable
                return new TypedValue(value, null, null, null);
            }
        }
    }

    private static TypedValue parseBoolean(String value) {
        String trimmed = value.trim().toLowerCase();
        Boolean bool = "true".equals(trimmed) || "yes".equals(trimmed) || "1".equals(trimmed) || "on".equals(trimmed);
        return new TypedValue(null, null, null, bool);
    }

    public record TypedValue(String text, Double number, LocalDateTime date, Boolean bool) {
        public boolean hasText() {
            return text != null;
        }

        public boolean hasNumber() {
            return number != null;
        }

        public boolean hasDate() {
            return date != null;
        }

        public boolean hasBoolean() {
            return bool != null;
        }
    }
}
