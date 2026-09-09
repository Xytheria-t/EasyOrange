package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * AI 搜索增强工具抽象。
 * <p>
 * 每个工具 = 一条可独立执行的增强能力（LLM 调用或规则引擎），
 * 由 {@link SearchToolRegistry} 按 Spring 自动装配收集，注册零改动。
 * name 对应 OpenAI Function Calling 的 function name。
 */
public interface SearchTool<T> {

    /**
     * 工具并行执行器 — 每任务一个虚拟线程。
     * <p>
     * LLM 调用秒级阻塞，不能占用 {@code ForkJoinPool.commonPool()} 平台线程；
     * {@code spring.threads.virtual.enabled} 只作用于 Spring 线程基础设施，管不到 commonPool，故显式指定。
     */
    Executor VIRTUAL = Executors.newVirtualThreadPerTaskExecutor();

    /** 工具名（唯一，对应 function name）。 */
    String name();

    /** 并行执行本工具，返回异步结果。 */
    CompletableFuture<T> run(SearchToolContext context);
}
