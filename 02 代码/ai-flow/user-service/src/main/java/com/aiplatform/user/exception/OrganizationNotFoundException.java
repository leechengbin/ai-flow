package com.aiplatform.user.exception;

public class OrganizationNotFoundException extends RuntimeException {
    public OrganizationNotFoundException(String id) {
        super("Organization not found: id=" + id);
    }
}