package com.cartethyia.easyorange.ai.application.toolcall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 一次 tool call 的参数解析视图 — arguments JSON 的只读投影；参数取值约束见各链路工具面的 {@code @ToolParam}，同源不重写。
 * <p>
 * 字段取两条链路工具参数的并集而非按工具分型 — 按名分派要引「名字→解析器」注册表加穷尽 switch，编译器看不见
 * 运行期的名字，这个规模下平铺更省。全字段 {@link Nullable}（每次只有 1-2 个有值）；工具名与原始串刻意不入
 * 此记录 — 二者均由 {@link ToolCallDecision} 从原生 tool call 取同一来源，省掉「解析后再补全」的半初始化态。
 * <p>
 * {@code ignoreUnknown} 是容错口径：模型带回 schema 外字段时只应表现为本工具参数缺失，不该作废整轮已合法的 tool call；钉在类上，不寄望全局 ObjectMapper 默认值。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolCallArguments(
        @Nullable String thought,
        @Nullable String query,
        @Nullable String productId,
        @Nullable List<String> productIds,
        @Nullable String preferenceKey,
        @Nullable String preferenceValue) {}
