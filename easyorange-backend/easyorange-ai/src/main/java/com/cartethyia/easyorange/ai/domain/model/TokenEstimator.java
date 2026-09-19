package com.cartethyia.easyorange.ai.domain.model;

/**
 * 上下文 token 估算器（确定性，无模型依赖）— 供注入 prompt 前的 pre-flight 裁剪决策。
 * <p>
 * 估算口径：CJK（汉字 / 假名 / 谚文 / 全角标点，codePoint >= 0x2E80 一律按 CJK 计）
 * 0.7 token/字，其余字符 0.3 token/字符（约 3.3 字符/token），向上取整。
 * 系数比 DeepSeek 官方口径（中文 0.6 / 英文 0.3）整体偏保守约 15%——预算护栏宁高勿低，
 * 高估只会多裁一点历史，低估会放超长上下文出去。
 * <p>
 * 估算只用于裁剪决策，不用于计费口径：真实用量以 API usage 回报为准
 * （Langfuse trace 与 {@code @TokenBudget} 记账都是真实值）。
 */
public final class TokenEstimator {

    /** CJK 起始码点：覆盖 CJK 部首/汉字/假名/谚文/全角形式（0x2E80 起）。 */
    private static final int CJK_CODEPOINT_START = 0x2E80;

    private static final double TOKENS_PER_CJK_CHAR = 0.7;
    private static final double TOKENS_PER_OTHER_CHAR = 0.3;

    private TokenEstimator() {}

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        int other = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (codePoint >= CJK_CODEPOINT_START) {
                cjk++;
            } else {
                other++;
            }
            i += Character.charCount(codePoint);
        }
        return (int) Math.ceil(cjk * TOKENS_PER_CJK_CHAR + other * TOKENS_PER_OTHER_CHAR);
    }
}
