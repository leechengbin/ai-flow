package com.aiplatform.user.domain.enums;

public enum UserStatus {
    ACTIVE,      // Normal active user
    SUSPENDED,   // Temporarily disabled
    LOCKED,      // Too many failed login attempts
    INACTIVE,    // Deactivated but not deleted
    DELETED      // Soft deleted
}
