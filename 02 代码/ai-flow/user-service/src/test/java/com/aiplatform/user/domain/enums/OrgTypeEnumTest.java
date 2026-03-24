package com.aiplatform.user.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrgTypeEnumTest {
    @Test
    void orgType_shouldHaveCorrectValues() {
        assertEquals(4, OrgType.values().length);
        assertNotNull(OrgType.COMPANY);
        assertNotNull(OrgType.DEPARTMENT);
        assertNotNull(OrgType.TEAM);
        assertNotNull(OrgType.GROUP);
    }

    @Test
    void valueOf_shouldReturnCorrectEnum() {
        assertEquals(OrgType.COMPANY, OrgType.valueOf("COMPANY"));
        assertEquals(OrgType.DEPARTMENT, OrgType.valueOf("DEPARTMENT"));
    }
}