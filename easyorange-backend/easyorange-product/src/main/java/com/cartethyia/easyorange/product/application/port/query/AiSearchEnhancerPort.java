package com.cartethyia.easyorange.product.application.port.query;

import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;
import java.util.Optional;

public interface AiSearchEnhancerPort {

    /**
     * 尝试对检索结果做 AI 增强。
     * <p>
     * <b>实现契约：本方法永不抛异常。</b>调用点在商品检索主链路上（不做异常兜底），
     * AI 侧任何失败都必须收敛为 {@link Optional#empty()}，让检索退化为「无 AI 增强」而非失败。
     * 同理，实现方不得把降级（超时/部分失败）结果写入缓存。
     */
    Optional<AiEnhancement> tryEnhance(String keyword, List<ProductReadModel> topProducts);
}
