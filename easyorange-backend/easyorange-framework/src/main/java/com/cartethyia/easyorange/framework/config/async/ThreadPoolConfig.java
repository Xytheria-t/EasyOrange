package com.cartethyia.easyorange.framework.config.async;

import com.cartethyia.easyorange.framework.config.properties.ThreadPoolProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * 线程池配置
 */
@AutoConfiguration
@EnableAsync
@RequiredArgsConstructor
public class ThreadPoolConfig {

    private static final int MIN_SCHEDULER_POOL_SIZE = 2;
    private static final int MIN_EVENT_POOL_SIZE = 1;
    private static final boolean WAIT_FOR_TASKS_TO_COMPLETE = true;

    private final ThreadPoolProperties threadPoolProperties;

    @Bean("taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(calculateSchedulerPoolSize());
        scheduler.setThreadNamePrefix("scheduled-");
        applyCommonSchedulerConfig(scheduler);
        scheduler.initialize();
        return scheduler;
    }

    @Bean("domainEventExecutor")
    public Executor domainEventExecutor() {
        return createTaskExecutor(
                "domain-event-",
                Math.max(MIN_EVENT_POOL_SIZE, threadPoolProperties.getCorePoolSize() / 2),
                Math.max(MIN_EVENT_POOL_SIZE, threadPoolProperties.getMaxPoolSize() / 2),
                threadPoolProperties.getQueueCapacity() * 2
        );
    }

    private int calculateSchedulerPoolSize() {
        return Math.max(MIN_SCHEDULER_POOL_SIZE, threadPoolProperties.getCorePoolSize() / 2);
    }

    private ThreadPoolTaskExecutor createTaskExecutor(String threadNamePrefix,
                                                      int corePoolSize,
                                                      int maxPoolSize,
                                                      int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(threadPoolProperties.getKeepAliveSeconds());
        applyCommonExecutorConfig(executor);
        executor.initialize();
        return executor;
    }

    private void applyCommonExecutorConfig(ThreadPoolTaskExecutor executor) {
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(WAIT_FOR_TASKS_TO_COMPLETE);
        executor.setAwaitTerminationSeconds(threadPoolProperties.getAwaitTerminationSeconds());
        executor.setRejectedExecutionHandler(new LoggingRejectedExecutionHandler(
                executor.getThreadNamePrefix(), false));
    }

    private void applyCommonSchedulerConfig(ThreadPoolTaskScheduler scheduler) {
        scheduler.setTaskDecorator(new MdcTaskDecorator());
        scheduler.setWaitForTasksToCompleteOnShutdown(WAIT_FOR_TASKS_TO_COMPLETE);
        scheduler.setAwaitTerminationSeconds(threadPoolProperties.getAwaitTerminationSeconds());
        scheduler.setRejectedExecutionHandler(new LoggingRejectedExecutionHandler(
                scheduler.getThreadNamePrefix(), false));
    }
}
