package com.cartethyia.easyorange.product.application.port.query;

import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;

public interface AiSearchEnhancerPort {

    /**
     * 尝试对检索结果做 AI 增强。
     * <p>
     * <b>实现契约：本方法永不抛异常。</b>调用点在商品检索主链路上（不做异常兜底），
     * AI 侧任何失败都必须收敛为 {@code degraded} 标记，让检索退化为「无 AI 增强」而非失败。
     * 同理，实现方不得把降级（超时/部分失败）结果写入缓存。
     *
     * @return 增强结果与降级标记，见 {@link EnhanceOutcome}
     */
    EnhanceOutcome tryEnhance(String keyword, List<ProductReadModel> topProducts);

    /**
     * 增强结果 + 降级标记。
     *
     * @param enhancement 增强数据；{@code null} 表示本次没有可展示的结果
     * @param degraded    {@code true} = 尝试过但失败（供应商超时/异常），向用户提示「AI 分析暂不可用」；
     *                    {@code false} = 成功，或本次不适用（非自然语言关键词 / 无检索结果），不提示——
     *                    两者必须区分，否则短关键词搜索会每次都误报降级
     */
    record EnhanceOutcome(AiEnhancement enhancement, boolean degraded) {
        public static EnhanceOutcome of(AiEnhancement enhancement) {
            return new EnhanceOutcome(enhancement, false);
        }

        /** 不适用：无需尝试，不构成降级 */
        public static EnhanceOutcome notApplicable() {
            return new EnhanceOutcome(null, false);
        }

        /** 尝试但失败：无结果可展示，前端据此显示降级提示（degraded 组件的工厂方法不能同名，会与访问器冲突） */
        public static EnhanceOutcome failed() {
            return new EnhanceOutcome(null, true);
        }
    }
}
