package com.cartethyia.easyorange.framework.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class AsyncManager {

    private static final int OPERATE_DELAY_TIME = 10;

    private final ThreadPoolTaskScheduler scheduler;

    public AsyncManager(@Qualifier("taskScheduler") ThreadPoolTaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 执行任务（延迟执行）
     */
    public void execute(Runnable task) {
        scheduler.schedule(task, Instant.now().plusMillis(OPERATE_DELAY_TIME));
    }

    /**
     * 执行任务（指定延迟时间）
     */
    public void execute(Runnable task, long delay, TimeUnit unit) {
        scheduler.schedule(task, Instant.now().plus(Duration.ofMillis(unit.toMillis(delay))));
    }

    /**
     * 立即执行任务
     */
    public void executeNow(Runnable task) {
        scheduler.execute(task);
    }
}
