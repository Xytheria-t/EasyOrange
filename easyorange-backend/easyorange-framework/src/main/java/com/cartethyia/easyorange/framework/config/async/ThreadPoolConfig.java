package com.cartethyia.easyorange.framework.config.async;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 线程池配置 — 仅保留 @Scheduled 定时任务的调度器。
 * 所有 IO 密集型异步任务（@Async、领域事件发布、WebSocket、AI 搜索等）
 * 已迁移到 Java 21+ 虚拟线程（spring.threads.virtual.enabled=true），
 * 不再需要自定义 ThreadPoolTaskExecutor。
 * <p>
 * 注意：此 bean 必须由框架声明，不能依赖 Boot 的 TaskSchedulingAutoConfiguration —
 * 虚拟线程开启时 Boot 会构建 {@code SimpleAsyncTaskScheduler}（同名 {@code taskScheduler}），
 * 而 WebSocketConfig 注入的是 {@code ThreadPoolTaskScheduler} 做心跳调度，类型不匹配会导致启动失败
 * （全仓只有本类声明该类型，注入按类型即可唯一解析，无需 {@code @Qualifier}）。
 *
 * @see org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
 */
@AutoConfiguration
public class ThreadPoolConfig {

    private static final boolean WAIT_FOR_TASKS_TO_COMPLETE = true;

    @Bean("taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-");
        scheduler.setTaskDecorator(new MdcTaskDecorator());
        scheduler.setWaitForTasksToCompleteOnShutdown(WAIT_FOR_TASKS_TO_COMPLETE);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setRejectedExecutionHandler(new LoggingRejectedExecutionHandler("scheduled-", false));
        return scheduler;
    }
}
