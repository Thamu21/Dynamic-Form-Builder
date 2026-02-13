package com.formforge.entity.enums;

/**
 * User roles for authorization.
 * ADMIN: Full system access
 * CREATOR: Can create/manage forms and view responses
 * RESPONDER: Can only submit responses to published forms
 */
public enum UserRole {
    ADMIN,
    CREATOR,
    RESPONDER
}
