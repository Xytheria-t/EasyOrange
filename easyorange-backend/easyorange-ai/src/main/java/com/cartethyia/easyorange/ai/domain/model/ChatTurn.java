package com.cartethyia.easyorange.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 对话轮次 — 短期记忆的最小单元（Redis 会话窗口内按序存取）。
 * <p>
 * 角色用 {@link Role} 枚举而非自由字符串：它在「存进 Redis」「装配 prompt 多消息」「决策上下文
 * 文本」三处被消费，其中两处是角色比较 —— 字符串拼错一个字母不会报错，只会静默把助手的话
 * 当用户的话注入。
 */
public record ChatTurn(Role role, String content) {

    /** 会话窗口里的两个角色；JSON 形态是小写 code（与既有 Redis 数据同形）。 */
    public enum Role {
        USER("user"),
        ASSISTANT("assistant");

        private final String code;

        Role(String code) {
            this.code = code;
        }

        @JsonValue
        public String code() {
            return code;
        }

        public boolean isUser() {
            return this == USER;
        }

        @JsonCreator
        public static Role fromCode(String code) {
            for (Role role : values()) {
                if (role.code.equals(code)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Unknown chat role: " + code);
        }
    }

    public static ChatTurn user(String content) {
        return new ChatTurn(Role.USER, content);
    }

    public static ChatTurn assistant(String content) {
        return new ChatTurn(Role.ASSISTANT, content);
    }
}
