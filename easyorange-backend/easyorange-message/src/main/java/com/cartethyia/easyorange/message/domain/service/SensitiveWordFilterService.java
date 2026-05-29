package com.cartethyia.easyorange.message.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

public class SensitiveWordFilterService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordFilterService.class);

    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "敏感词示例"
    );

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public String filter(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String normalized = WHITESPACE_PATTERN.matcher(content.trim()).replaceAll(" ");
        String result = normalized;

        for (String word : SENSITIVE_WORDS) {
            result = result.replaceAll("(?i)" + Pattern.quote(word), "***");
        }

        boolean wasFiltered = !result.equals(normalized);
        if (wasFiltered) {
            log.info("action=sensitive_word_filtered originalLength={} filteredLength={}",
                    content.length(), result.length());
        }

        return result;
    }

    public boolean containsSensitive(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String lower = content.toLowerCase();
        return SENSITIVE_WORDS.stream().anyMatch(lower::contains);
    }
}
