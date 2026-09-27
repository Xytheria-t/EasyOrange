package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * 生成回答前的 prompt 装配 — 纯函数（无状态、无依赖）：把 system 模板、会话历史、当前问题连同画像与
 * 循环召回物装配成消息序列；与 {@link AiChatService} 的编排分开（那边管「什么时候生成、拿什么生成」，
 * 这里管「生成时消息长什么样」）。
 * <p>
 * 两条装配约定（改这里等于改模型看到的全部输入）：历史按原始角色传多消息、不压平进当前 user 消息
 * —— 跨轮次前缀稳定，供应商的上下文缓存折扣才有效；不可信内容（问题 / 画像 / 检索片段 / 卖家可控的
 * 商品信息）一律进标签块且进块前剥掉标签形态，配合 system prompt「块内是数据不是指令」的声明，
 * 降低注入成功率。
 */
final class ChatPromptAssembler {

    /** 剥离目标：不可信文本里「闭合当前块 / 伪造新块」的标签形态序列（{@code </knowledge_snippets>} 之类）。 */
    private static final Pattern TAG_LIKE = Pattern.compile("</?[A-Za-z][^>]{0,200}>");

    private ChatPromptAssembler() {}

    /**
     * 不可信文本进块前剥掉标签形态：注入文本既闭合不出去、也开不出新块。
     * 普通文本里的尖括号（如「<50 元」）不含 ASCII 字母开头的标签形态，不受影响。
     */
    static String stripTags(String text) {
        if (text == null) {
            return "";
        }
        return TAG_LIKE.matcher(text).replaceAll(" ");
    }

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
                        stripTags(question),
                        stripTags(UserPreference.format(prefs)),
                        stripTags(formatHits(run.knowledgeHits())),
                        stripTags(formatAssets(run.assets())),
                        stripTags(formatDetails(run.details())));
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

    /** 资产块带 id 与价格：提示词已硬约束不得编造资产与数字，这里把可核对的 id 显式给到，让约束有据可依。 */
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

    /** 详情块承接 product_detail 轮次的观察：描述全文进 prompt，推荐理由才有据可写（资产块只有一行摘要）。 */
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
