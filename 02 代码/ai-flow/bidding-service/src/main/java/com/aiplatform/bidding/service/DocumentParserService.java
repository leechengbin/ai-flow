package com.aiplatform.bidding.service;

import com.aiplatform.bidding.dto.response.ParsedDocumentDto;

public interface DocumentParserService {
    ParsedDocumentDto parse(byte[] content, String fileName);
    boolean supports(String fileType);
}