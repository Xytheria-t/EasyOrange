package com.cartethyia.easyorange.ai.adapter.outbound;

import com.cartethyia.easyorange.product.domain.port.NegotiationContext;
import com.cartethyia.easyorange.product.domain.port.NegotiationMessagePort;
import com.cartethyia.easyorange.ai.port.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class DeepSeekNegotiationMessageAdapter implements NegotiationMessagePort {

    private final LlmPort llmPort;

    private static final String SYSTEM_PROMPT = """
            你是 EasyOrange — AI 替卖家运营的 C2C 平台的AI议价助手。
            根据议价决策结果，生成自然、友好的中文话术。
            要求：
            1. 语气亲切但不卑微
            2. 简短（不超过50字）
            3. 不要暴露你是AI
            4. ACCEPT: 告知出价被接受，引导付款
            5. COUNTER: 提出还价，说明理由
            6. REJECT: 委婉拒绝，鼓励合理出价
            """;

    @Override
    public String generateMessage(NegotiationContext context) {
        String userMessage = buildUserMessage(context);
        try {
            return llmPort.generateText(SYSTEM_PROMPT, userMessage);
        } catch (Exception e) {
            log.warn("LLM话术生成失败，使用兜底话术: {}", e.getMessage());
            return fallbackMessage(context);
        }
    }

    private String buildUserMessage(NegotiationContext ctx) {
        return switch (ctx.decisionType()) {
            case "ACCEPT" -> String.format(
                    "商品「%s」，买家出价¥%s，决策：接受。生成接受话术。",
                    ctx.productName(), ctx.buyerOffer());
            case "COUNTER" -> String.format(
                    "商品「%s」，买家出价¥%s，决策：还价¥%s，理由：%s。生成还价话术。",
                    ctx.productName(), ctx.buyerOffer(), ctx.counterPrice(), ctx.reason());
            case "REJECT" -> String.format(
                    "商品「%s」，买家出价¥%s，决策：拒绝，理由：%s。生成拒绝话术。",
                    ctx.productName(), ctx.buyerOffer(), ctx.reason());
            default -> "未知决策";
        };
    }

    private String fallbackMessage(NegotiationContext ctx) {
        return switch (ctx.decisionType()) {
            case "ACCEPT" -> String.format("好的，¥%s成交！请尽快付款哦~", ctx.acceptedPrice());
            case "COUNTER" -> String.format("¥%s有点低啦，¥%s可以吗？", ctx.buyerOffer(), ctx.counterPrice());
            case "REJECT" -> String.format("抱歉，¥%s太低啦，再考虑考虑？", ctx.buyerOffer());
            default -> "暂时无法处理";
        };
    }
}
