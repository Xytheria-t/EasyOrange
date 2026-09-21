package com.cartethyia.easyorange.ai.application.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

@DisplayName("ChatPromptAssembler (生成 prompt 装配) -> 测试")
class ChatPromptAssemblerTest {

    private static final String SYSTEM_PROMPT = "系统提示词";

    private static List<Message> assemble(
            String question,
            List<ChatTurn> history,
            List<KnowledgeHit> hits,
            List<AssetHit> assets,
            List<AssetDetail> details) {
        return ChatPromptAssembler.assemble(
                SYSTEM_PROMPT,
                question,
                history,
                List.of(new UserPreference("condition", "九五新以上")),
                new AgentLoopRunner.Result(hits, assets, details, AgentLoopRunner.OUTCOME_FINISHED, 1));
    }

    private static String currentUserMessage(List<Message> messages) {
        return messages.getLast().getText();
    }

    @Test
    @DisplayName("消息序列 -> system + 历史按原始角色 + 当前 user 收尾")
    void assemble_messageOrder() {
        List<Message> messages = assemble(
                "继续", List.of(ChatTurn.user("上一轮问题"), ChatTurn.assistant("上一轮回答")), List.of(), List.of(), List.of());

        assertThat(messages)
                .extracting(Message::getMessageType)
                .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
        assertThat(messages.getFirst().getText()).isEqualTo(SYSTEM_PROMPT);
        assertThat(messages.get(1).getText()).isEqualTo("上一轮问题");
        assertThat(messages.get(2).getText()).isEqualTo("上一轮回答");
    }

    @Test
    @DisplayName("当前 user 消息 -> 五类标签块齐备，历史不重复注入")
    void assemble_currentUserMessageBlocks() {
        List<Message> messages = assemble("怎么退款？", List.of(ChatTurn.user("上一轮问题")), List.of(), List.of(), List.of());

        assertThat(currentUserMessage(messages))
                .contains(
                        "<user_question>",
                        "怎么退款？",
                        "<user_profile>",
                        "condition: 九五新以上",
                        "<knowledge_snippets>",
                        "<candidate_assets>",
                        "<asset_details>")
                // 历史不进当前 user 消息（前缀稳定才能命中供应商上下文缓存）
                .doesNotContain("上一轮问题");
    }

    @Test
    @DisplayName("知识片段 -> 带序号与标题，便于回答里的 [来源:标题] 溯源对上号")
    void assemble_knowledgeHits() {
        List<Message> messages = assemble(
                "怎么退款？",
                List.of(),
                List.of(new KnowledgeHit("kb-0002", "退款规则", "签收后 7 天内无理由", 0.95)),
                List.of(),
                List.of());

        assertThat(currentUserMessage(messages)).contains("[1] (退款规则)", "签收后 7 天内无理由");
    }

    @Test
    @DisplayName("资产块 -> 带 id 与价格（校验锚点），缺失字段退化为可读文本")
    void assemble_assetsWithFallbackText() {
        List<Message> messages = assemble(
                "想找 5000 以内的笔记本",
                List.of(),
                List.of(),
                List.of(
                        new AssetHit("p-1", "MacBook Air M1", new BigDecimal("4200.00"), "数码", "九五新", 0.83),
                        new AssetHit("p-2", "二手显示器", null, null, null, 0.5)),
                List.of());

        assertThat(currentUserMessage(messages))
                .contains("[p-1] MacBook Air M1 | ¥4200 | 数码 | 九五新")
                .contains("[p-2] 二手显示器 | ¥面议 | 未分类 | 成色未标注");
    }

    @Test
    @DisplayName("详情块 -> 描述全文进 prompt，无描述有占位")
    void assemble_assetDetails() {
        List<Message> messages = assemble(
                "推荐一台笔记本",
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new AssetDetail(
                                "p-1",
                                "MacBook Air M1",
                                "M1 芯片，95 新无磕碰",
                                BigDecimal.valueOf(4200),
                                "数码",
                                "九五新",
                                "上海",
                                "liming",
                                "ONLINE"),
                        new AssetDetail("p-2", "二手显示器", null, null, null, null, null, null, "ONLINE")));

        assertThat(currentUserMessage(messages))
                .contains("[p-1] MacBook Air M1\n描述：M1 芯片，95 新无磕碰")
                .contains("[p-2] 二手显示器\n描述：无描述");
    }

    @Test
    @DisplayName("召回物为空 -> 三个块各有缺省占位，不留空块")
    void assemble_emptyBlocks() {
        List<Message> messages = assemble("在吗？", List.of(), List.of(), List.of(), List.of());

        assertThat(currentUserMessage(messages))
                .contains("(无检索结果)")
                .contains("(无可推荐资产)")
                .contains("(无)");
    }
}
