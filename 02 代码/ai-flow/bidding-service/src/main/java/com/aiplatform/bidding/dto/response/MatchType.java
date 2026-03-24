package com.aiplatform.bidding.dto.response;

public enum MatchType {
    EXACT,      // 完全匹配
    SEMANTIC,   // 语义匹配
    PARTIAL,    // 部分匹配
    MISSING     // 完全缺失
}