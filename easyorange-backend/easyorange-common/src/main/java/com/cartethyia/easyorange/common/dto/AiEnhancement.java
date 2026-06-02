package com.cartethyia.easyorange.common.dto;

import java.util.List;
import java.util.Map;

public record AiEnhancement(
    String intentExplanation,
    Map<Long, List<String>> productTags,
    String marketAnalysis,
    List<String> suggestedQuestions
) {}
