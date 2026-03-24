package com.aiplatform.user.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserStatusEnumTest {
    @Test
    void userStatus_shouldHaveCorrectValues() {
        assertEquals(5, UserStatus.values().length);
        assertNotNull(UserStatus.ACTIVE);
        assertNotNull(UserStatus.SUSPENDED);
        assertNotNull(UserStatus.LOCKED);
        assertNotNull(UserStatus.INACTIVE);
        assertNotNull(UserStatus.DELETED);
    }

    @Test
    void valueOf_shouldReturnCorrectEnum() {
        assertEquals(UserStatus.ACTIVE, UserStatus.valueOf("ACTIVE"));
        assertEquals(UserStatus.SUSPENDED, UserStatus.valueOf("SUSPENDED"));
    }

    @Test
    void valueOf_shouldThrowForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> UserStatus.valueOf("INVALID"));
    }
}