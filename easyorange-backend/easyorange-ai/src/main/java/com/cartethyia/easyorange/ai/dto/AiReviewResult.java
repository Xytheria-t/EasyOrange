package com.cartethyia.easyorange.ai.dto;

import java.util.List;

public record AiReviewResult(
        boolean suggestedAction,
        String suggestedActionDesc,
        int confidenceScore,
        List<String> riskFlags,
        String reasoning) {
    public static final int ACTION_PASS = 1;
    public static final int ACTION_REJECT = 2;
}
