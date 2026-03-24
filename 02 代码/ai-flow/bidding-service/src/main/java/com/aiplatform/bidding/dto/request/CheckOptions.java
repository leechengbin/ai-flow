package com.aiplatform.bidding.dto.request;

public record CheckOptions(
    boolean checkCoverage,
    boolean checkFormat,
    boolean checkStarred
) {
    public CheckOptions {
        if (checkCoverage == false && checkFormat == false && checkStarred == false) {
            // Default: check all
            checkCoverage = true;
            checkFormat = true;
            checkStarred = true;
        }
    }
}
