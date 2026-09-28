package com.cartethyia.easyorange.ai.application.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 一次 tool call 的参数解析视图 — arguments JSON 的只读投影，编排器据此取「本步理由」与工具入参摘要
 * （trace 落库 / SSE step 事件消费）。各参数的取值约束见 {@link ChatTools} 的 {@code @ToolParam}
 * 描述，与此处字段同源，不重写。
 * <p>
 * 放编排器包内而非 domain：形状由 {@code ChatTools} 的注解决定，domain 反过来依赖 application 的注解
 * 是倒置的依赖方向；且主代码里只有 {@link ToolCallLoop} 一个消费者，包级私有即足够，不进领域模型。
 * <p>
 * 工具名与 arguments 原始串刻意不在此记录内：前者来自 tool call 的 function name，后者就是被解析的
 * 那个串本身，两者都由 {@link ToolCallDecision} 从原生 tool call 取，一个来源。所以这里
 * 不需要「解析后再补全」的两段式构造 —— 补全会让记录存在「已解析但未补全」的半初始化态，而调用方
 * 拿到的类型签名却看不出来。
 * <p>
 * 六个分量取 7 个工具 {@code @ToolParam} 的并集而非按工具分型：工具名只在运行期由模型给出，按名分派
 * 要引入「名字 → 解析器」注册表加穷尽 switch，编译器看不见运行期的名字、换不来编译期检查，7 个工具的
 * 规模下平铺是更省的一侧。代价是每次只有 1-2 个分量有值，故全部标 {@link Nullable}（一次
 * knowledge_search 只有 thought + query 有值）。
 * <p>
 * 显式 ignoreUnknown 是本类的容错口径：模型可能带回 schema 外的字段（拼错的参数名、提示注入塞进来的
 * tool 字段），那应当只表现为「本工具的参数字段缺失」，而不是让整轮已合法的 tool call 解析失败被作废 ——
 * 钉在类上，不寄望全局 ObjectMapper 的默认值。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ToolCallArguments(
        @Nullable String thought,
        @Nullable String query,
        @Nullable String productId,
        @Nullable List<String> productIds,
        @Nullable String preferenceKey,
        @Nullable String preferenceValue) {}
