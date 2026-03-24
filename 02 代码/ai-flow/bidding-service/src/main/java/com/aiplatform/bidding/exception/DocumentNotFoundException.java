package com.aiplatform.bidding.exception;

public class DocumentNotFoundException extends RuntimeException {
    private final String documentId;
    public DocumentNotFoundException(String documentId) { super("Document not found: " + documentId); this.documentId = documentId; }
    public String getDocumentId() { return documentId; }
}