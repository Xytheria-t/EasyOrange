package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import java.util.concurrent.CompletableFuture;

/**
 * AI 搜索增强工具抽象。
 * <p>
 * 每个工具 = 一条可独立执行的增强能力（LLM 调用或规则引擎），
 * 由 {@link SearchToolRegistry} 按 Spring 自动装配收集，注册零改动。
 * name/description 对应 OpenAI Function Calling 的 function name / description，
 * 未来若需要把工具暴露给模型自主调用，可基于它们生成 JSON Schema。
 */
public interface SearchTool<T> {

    /** 工具名（唯一，对应 function name）。 */
    String name();

    /** 工具用途描述（对应 function description）。 */
    String description();

    /** 工具类型：LLM 调用或本地规则引擎。 */
    SearchToolKind kind();

    /** 并行执行本工具，返回异步结果。 */
    CompletableFuture<T> run(SearchToolContext context);
}
