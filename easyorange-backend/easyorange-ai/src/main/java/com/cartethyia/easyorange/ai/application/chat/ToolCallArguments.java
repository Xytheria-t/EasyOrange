package com.cartethyia.easyorange.ai.application.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 一次 tool call 的参数解析视图 — arguments JSON 的只读投影；参数取值约束见 {@link ChatTools} 的 {@code @ToolParam}，同源不重写。
 * <p>
 * 放编排器包内而非 domain：形状由 {@code ChatTools} 注解决定，反向依赖是倒置的，消费者只有
 * {@link ToolCallLoop} 一个；六分量取 7 个工具参数的并集而非按工具分型 — 按名分派要引「名字→解析器」注册表加穷尽 switch，编译器看不见运行期的名字，这个规模下平铺更省。
 * <p>
 * 六分量全 {@link Nullable}（每次只有 1-2 个有值）；工具名与原始串刻意不入此记录 — 二者均由
 * {@link ToolCallDecision} 从原生 tool call 取同一来源，省掉「解析后再补全」的半初始化态。
 * <p>
 * {@code ignoreUnknown} 是容错口径：模型带回 schema 外字段时只应表现为本工具参数缺失，不该作废整轮已合法的 tool call；钉在类上，不寄望全局 ObjectMapper 默认值。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ToolCallArguments(
        @Nullable String thought,
        @Nullable String query,
        @Nullable String productId,
        @Nullable List<String> productIds,
        @Nullable String preferenceKey,
        @Nullable String preferenceValue) {}
