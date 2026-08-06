package com.cartethyia.easyorange.ai.service;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NaturalLanguageDetector {

    private static final Set<String> INTENT_WORDS =
            Set.of("找", "推荐", "适合", "可以", "预算", "以内", "左右", "哪个", "怎么", "什么", "好", "吗", "能", "要");

    private static final int MIN_LENGTH = 5;

    public boolean isNaturalLanguage(String keyword) {
        if (keyword == null || keyword.length() < MIN_LENGTH || keyword.isBlank()) {
            return false;
        }
        return INTENT_WORDS.stream().anyMatch(w -> keyword.contains(w));
    }
}
