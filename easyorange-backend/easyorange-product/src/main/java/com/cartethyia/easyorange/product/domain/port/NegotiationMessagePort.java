package com.cartethyia.easyorange.product.domain.port;

/**
 * 议价话术生成端口 — 根据议价决策生成自然语言话术。
 * <p>
 * 本接口定义在 product 模块（消费方），由 application 模块的适配器实现，
 * 委托给 ai 模块的 {@code DeepSeekNegotiationMessageAdapter}。
 */
public interface NegotiationMessagePort {

    /**
     * 根据议价上下文生成自然话术。
     *
     * @param context 议价上下文（含决策类型、价格、原因等）
     * @return 自然语言话术
     */
    String generateMessage(NegotiationContext context);
}
