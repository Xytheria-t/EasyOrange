package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;

/**
 * 成色码 → 展示文本：索引里存的是码（{@code "2"}），给人看的是中文描述（{@code "几乎全新"}）。
 * <p>
 * 搜索读模型与 AI 资产命中两条 ES 读取路径共用同一份映射；码表里没有的取值**原样透出**，不吞信息
 * —— 脏索引值宁可显示成原码，也不要变成 null 让前端空白。
 */
final class ConditionLevelText {

    private ConditionLevelText() {}

    static String of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return ConditionLevel.fromCode(code).getDesc();
        } catch (Exception e) {
            // 码表外取值（历史脏数据 / 索引里混进的自由文本）不算错误，按原码展示
            return code;
        }
    }
}
