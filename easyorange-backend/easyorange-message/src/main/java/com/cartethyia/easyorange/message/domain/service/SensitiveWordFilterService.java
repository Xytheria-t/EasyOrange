package com.cartethyia.easyorange.message.domain.service;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 敏感词过滤服务 —— 纯领域服务。
 * <p>
 * 不含仓储/外部依赖，只表达「消息内容需过滤敏感词」这一领域规则。
 * 与 {@code OfflineMessageStoreService}（应用层编排）区分：本类留在 domain 层。
 */
@Slf4j
public class SensitiveWordFilterService {

    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "敏感词示例"
    );

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * 将内容中所有敏感词替换为 "***"；过滤前先去掉首尾空白并归一化连续空白。
     *
     * @param content 待过滤文本；可为 null 或空白
     * @return 敏感词被替换后的文本；若入参为 null/空白则原样返回
     */
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

    /**
     * 判断内容是否包含敏感词。
     *
     * @param content 待检查文本；可为 null 或空白
     * @return 至少命中一个敏感词返回 true，否则 false
     */
    public boolean containsSensitive(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String lower = content.toLowerCase();
        return SENSITIVE_WORDS.stream().anyMatch(lower::contains);
    }
}
