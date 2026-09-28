package com.cartethyia.easyorange.message.application.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 敏感词词表配置 —— 挂在 application 层的组装根上（domain 只收不可变词表，不认识 Spring）。
 * <p>
 * 取舍：词表外置成配置项而不是写死在领域类里，运营换词库不用发版；紧凑构造器兜住 null，
 * 保证「没配也能跑」。
 * <p>
 * 边界：默认词表是<b>基线词库</b>——覆盖 C2C 交易场景的高危词，不等于完整的运营词库；
 * 生产应按合规要求维护并在配置中心下发 {@code message.sensitive-words.words}。
 * 配置缺失或为空时回落基线词库而非「不过滤」：安全控制不允许被一条空配置静默关掉。
 *
 * @param words 敏感词列表，大小写不敏感匹配
 */
@ConfigurationProperties(prefix = "message.sensitive-words")
public record SensitiveWordProperties(List<String> words) {

    /** 基线词库：交易诈骗 / 违禁品 / 代办类黑产话术。仅作无配置时的可运行兜底。 */
    private static final List<String> DEFAULT_WORDS =
            List.of("诈骗", "刷单", "兼职刷单", "代开发票", "发票代开", "走私", "赌博", "毒品", "枪支", "身份证代办", "银行卡代办", "高利贷", "洗钱", "刷流水");

    public SensitiveWordProperties {
        words = words == null || words.isEmpty() ? DEFAULT_WORDS : List.copyOf(words);
    }
}
