package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.support.UntrustedText;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * 生成回答前的 prompt 装配 — 纯函数：把 system 模板、会话历史、当前问题连同偏好与召回物装配成消息序列
 *（与 {@link AiChatAppService} 分开：那边管「什么时候生成、拿什么生成」，这里管「生成时消息长什么样」）。
 * <p>
 * 两条必须守住的约定：历史按原始角色传多消息 —— 跨轮前缀稳定，供应商的上下文缓存折扣才有效；不可信
 * 内容一律进标签块，且进块前过 {@link UntrustedText#stripTags}。
 */
final class ChatPromptAssembler {

    private ChatPromptAssembler() {}

    static List<Message> assemble(
            String systemPrompt,
            String question,
            List<ChatTurn> history,
            List<UserPreference> prefs,
            ToolCallLoop.Result run) {
        List<Message> messages = new ArrayList<>(history.size() + 2);
        messages.add(new SystemMessage(systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(turn.role().isUser() ? new UserMessage(turn.content()) : new AssistantMessage(turn.content()));
        }
        messages.add(new UserMessage(buildCurrentUserMessage(question, prefs, run)));
        return messages;
    }

    private static String buildCurrentUserMessage(
            String question, List<UserPreference> prefs, ToolCallLoop.Result run) {
        String questionText = UntrustedText.stripTags(question);
        String preferencesText = UntrustedText.stripTags(UserPreference.format(prefs));
        String knowledgeText = UntrustedText.stripTags(formatKnowledgeHits(run.knowledgeHits()));
        String assetText = UntrustedText.stripTags(formatAssetHits(run.assetHits()));
        String detailText = UntrustedText.stripTags(formatAssetDetails(run.details()));
        return """
                <user_question>
                %s
                </user_question>

                <user_preferences>
                %s
                </user_preferences>

                <knowledge_hits>
                %s
                </knowledge_hits>

                <asset_hits>
                %s
                </asset_hits>

                <asset_details>
                %s
                </asset_details>
                """.formatted(questionText, preferencesText, knowledgeText, assetText, detailText);
    }

    private static String formatKnowledgeHits(List<KnowledgeHit> hits) {
        if (hits.isEmpty()) {
            return "(无检索结果)";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            KnowledgeHit hit = hits.get(i);
            sb.append("[%d] (%s)\n%s\n".formatted(i + 1, hit.title(), hit.content()));
        }
        return sb.toString();
    }

    /** 资产块带 id 与价格：提示词已硬约束不得编造资产与数字，这里把可核对的 id 显式给到，让约束有据可依。 */
    private static String formatAssetHits(List<AssetHit> assetHits) {
        if (assetHits.isEmpty()) {
            return "(无可推荐资产)";
        }
        var sb = new StringBuilder();
        for (AssetHit asset : assetHits) {
            sb.append("[%s] %s | ¥%s | %s | %s\n"
                    .formatted(
                            asset.productId(),
                            asset.title(),
                            asset.price() == null
                                    ? "面议"
                                    : asset.price().stripTrailingZeros().toPlainString(),
                            asset.categoryName() == null ? "未分类" : asset.categoryName(),
                            asset.conditionDesc() == null ? "成色未标注" : asset.conditionDesc()));
        }
        return sb.toString();
    }

    /** 详情块承接 product_detail 轮次的观察：描述全文进 prompt，推荐理由才有据可写（资产块只有一行摘要）。 */
    private static String formatAssetDetails(List<AssetDetail> details) {
        if (details.isEmpty()) {
            return "(无)";
        }
        var sb = new StringBuilder();
        for (AssetDetail detail : details) {
            sb.append("[%s] %s\n描述：%s\n"
                    .formatted(
                            detail.productId(),
                            detail.title(),
                            detail.description() == null ? "无描述" : detail.description()));
        }
        return sb.toString();
    }
}
