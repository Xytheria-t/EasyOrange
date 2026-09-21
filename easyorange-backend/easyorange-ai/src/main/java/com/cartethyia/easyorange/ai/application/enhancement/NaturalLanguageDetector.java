package com.cartethyia.easyorange.ai.application.enhancement;

import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 自然语言查询识别 — 搜索增强管道入口的零 LLM 门槛：关键词含意图词（找 / 推荐 / 预算 / 以内 …）
 * 且长度达标才算自然语言，才继续走 {@code AiSearchEnhancerAdapter} 的 4 路增强。
 * <p>
 * 裸关键词（{@code iPhone 14} / {@code 笔记本}）在这里就被挡掉：管道的固定开销是那一次 LLM 意图识别，
 * 先用本地规则滤掉「本来就不需要问模型」的检索词，比把这次判断也交给模型便宜得多（纯规则、亚毫秒）。
 * <p>
 * 规则即调优点：命中任一意图词即可（不看词序、不做分词）；长度门槛 {@value #MIN_LENGTH} 挡的是
 * 「找电脑」这类短查询 —— 它们靠标题匹配就能答，增强只是多一层等待。
 */
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
