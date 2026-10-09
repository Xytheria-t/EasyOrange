package com.cartethyia.easyorange.ai.application.support;

import java.util.regex.Pattern;

/**
 * 不可信文本进标签块前的净化 — 剥掉标签形态序列（{@code </knowledge_hits>}、{@code <system>}），
 * 使它既闭合不掉当前块、也开不出新块。
 * <p>
 * chat 与 listing 的决策、生成装配都要过它：只剥一处，另一处就是注入口 —— 决策上下文决定调哪个
 * 工具，且含唯一的画像写路径。
 */
public final class UntrustedText {

    /** 标签形态：ASCII 字母开头、{@code >} 收尾；一路吞到第一个 {@code >}，内嵌的标签形态一并带走。 */
    private static final Pattern TAG_LIKE = Pattern.compile("</?[A-Za-z][^>]*>");
    // ponytail: 超长且无 > 的输入是 O(n²) 扫描；注入面长度都有上限（问题 / 描述 ≤ 2000 字符），放开上限时换线性扫描

    private UntrustedText() {}

    /** 普通文本里的尖括号（如「&lt;50 元」）不以字母开头，不构成标签形态，原样保留；null 收敛成空串。 */
    public static String stripTags(String text) {
        if (text == null) {
            return "";
        }
        return TAG_LIKE.matcher(text).replaceAll(" ");
    }
}
