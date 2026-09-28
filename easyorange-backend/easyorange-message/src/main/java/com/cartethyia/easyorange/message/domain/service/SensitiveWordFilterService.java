package com.cartethyia.easyorange.message.domain.service;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 敏感词过滤服务 —— 纯领域服务：不含仓储 / 外部依赖，只表达「消息内容需过滤敏感词」这一领域规则。
 * <p>
 * 取舍：词表由 {@code SensitiveWordProperties} 注入（{@code message.sensitive-words.words}），
 * 领域类只接收不可变词表；发布换词库不需要改代码，运营侧改配置即可。
 * <p>
 * 边界：默认词表是<b>可运行的基线词库</b>（诈骗 / 刷单 / 代开发票等交易场景高危词），
 * 不是完整的运营词库——真正的词库需要按业务与合规要求维护并通过配置下发。
 * 词表为空时不做替换也不抛错（过滤器只做替换这一件事，不替调用方决定策略），
 * 但正常装配路径不会给出空词表：配置缺失由 {@code SensitiveWordProperties} 兜回基线。
 * 与 {@code OfflineMessageAppService}（应用层编排）区分：本类留在 domain 层。
 */
@Slf4j
public class SensitiveWordFilterService {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /** 构造期预编译：过滤在发送主链路上每条消息都要跑，不把正则编译留到调用期。 */
    private final List<Pattern> sensitiveWords;

    public SensitiveWordFilterService(Collection<String> words) {
        this.sensitiveWords = words == null
                ? List.of()
                : words.stream()
                        .filter(word -> word != null && !word.isBlank())
                        .map(word -> Pattern.compile("(?i)" + Pattern.quote(word)))
                        .toList();
    }

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

        for (Pattern sensitiveWord : sensitiveWords) {
            result = sensitiveWord.matcher(result).replaceAll("***");
        }

        boolean wasFiltered = !result.equals(normalized);
        if (wasFiltered) {
            log.info(
                    "action=sensitive_word_filtered wordCount={} originalLength={} filteredLength={}",
                    sensitiveWords.size(),
                    content.length(),
                    result.length());
        }

        return result;
    }
}
