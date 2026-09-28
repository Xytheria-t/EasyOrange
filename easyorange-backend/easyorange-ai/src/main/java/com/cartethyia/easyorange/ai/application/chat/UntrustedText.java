package com.cartethyia.easyorange.ai.application.chat;

import java.util.regex.Pattern;

/**
 * 不可信文本进标签块前的净化 — 剥离「闭合当前块 / 伪造新块」的标签形态序列。
 * <p>
 * 两条链路都要过它：决策消息（{@link ToolCallLoop} 构造的首条 user 消息，决定调哪个工具）与生成消息
 * （{@link ChatPromptAssembler}）。只在一处剥等于给另一处留了注入口 —— 决策上下文里被伪造的块能改写
 * 工具选择，其中包含唯一的写路径（remember_preference 写用户画像）。
 */
final class UntrustedText {

    /** 剥离目标：不可信文本里的标签形态序列（{@code </knowledge_hits>}、{@code <system>} 之类）。 */
    private static final Pattern TAG_LIKE = Pattern.compile("</?[A-Za-z][^>]{0,200}>");

    private UntrustedText() {}

    /**
     * 不可信文本进块前剥掉标签形态：注入文本既闭合不出去、也开不出新块。
     * 普通文本里的尖括号（如「&lt;50 元」）不含 ASCII 字母开头的标签形态，不受影响。
     */
    static String stripTags(String text) {
        if (text == null) {
            return "";
        }
        return TAG_LIKE.matcher(text).replaceAll(" ");
    }
}
