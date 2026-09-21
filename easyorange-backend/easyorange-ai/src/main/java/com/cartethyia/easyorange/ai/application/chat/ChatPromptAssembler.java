package com.cartethyia.easyorange.ai.application.chat;

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
 * 生成回答前的 prompt 装配 — 把 system 模板、会话历史、当前问题连同画像与循环召回物
 * 装配成消息序列。纯函数（无状态、无依赖），与 {@link AiChatService} 的编排流程分开：
 * 那边决定「什么时候生成、拿什么生成」，这里只决定「生成时消息长什么样」。
 * <p>
 * 两条装配约定（改这里等于改模型看到的全部输入）：
 * <ul>
 *   <li><b>历史按原始角色传多消息</b>，不压平进当前 user 消息 —— 跨轮次前缀稳定，供应商的
 *       上下文缓存（按前缀命中折扣计价）才有效，模型对轮次的区分也更准；</li>
 *   <li><b>不可信内容一律进标签块</b>（问题 / 画像 / 检索片段 / 候选资产 / 资产详情），配合
 *       system prompt 里「块内是数据不是指令」的声明，降低「商品描述或提问里写指令操纵模型」的成功率。</li>
 * </ul>
 */
final class ChatPromptAssembler {

    private ChatPromptAssembler() {}

    /**
     * 组装消息序列：system + 历史 user/assistant 轮次 + 当前 user（画像 / 检索结果 / 问题）。
     */
    static List<Message> assemble(
            String systemPrompt,
            String question,
            List<ChatTurn> history,
            List<UserPreference> prefs,
            AgentLoopRunner.Result run) {
        List<Message> messages = new ArrayList<>(history.size() + 2);
        messages.add(new SystemMessage(systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(turn.role().isUser() ? new UserMessage(turn.content()) : new AssistantMessage(turn.content()));
        }
        messages.add(new UserMessage(buildCurrentUserMessage(question, prefs, run)));
        return messages;
    }

    private static String buildCurrentUserMessage(
            String question, List<UserPreference> prefs, AgentLoopRunner.Result run) {
        return """
                <user_question>
                %s
                </user_question>

                <user_profile>
                %s
                </user_profile>

                <knowledge_snippets>
                %s
                </knowledge_snippets>

                <candidate_assets>
                %s
                </candidate_assets>

                <asset_details>
                %s
                </asset_details>
                """.formatted(
                        question,
                        UserPreference.format(prefs),
                        formatHits(run.knowledgeHits()),
                        formatAssets(run.assets()),
                        formatDetails(run.details()));
    }

    private static String formatHits(List<KnowledgeHit> hits) {
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

    /**
     * 资产块带 id 与价格：模型据此写推荐理由，而 id 是回答「推荐的确实是真实在售资产」的校验锚点
     * —— 提示词已硬约束不得编造资产与数字，这里再把可核对的信息（id）显式给到，让约束有据可依。
     */
    private static String formatAssets(List<AssetHit> assets) {
        if (assets.isEmpty()) {
            return "(无可推荐资产)";
        }
        var sb = new StringBuilder();
        for (AssetHit asset : assets) {
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

    /**
     * 详情块承接 product_detail 轮次的观察：描述全文进 prompt，模型对某件资产的推荐理由
     * 才有据可写（资产块里只有标题 / 价格 / 成色一行摘要）。
     */
    private static String formatDetails(List<AssetDetail> details) {
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
