package com.cartethyia.easyorange.framework.config.async;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingRejectedExecutionHandler implements RejectedExecutionHandler {

    private final String executorName;
    private final boolean discard;

    public LoggingRejectedExecutionHandler(String executorName, boolean discard) {
        this.executorName = executorName;
        this.discard = discard;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        log.warn(
                "线程池 [{}] 任务被拒绝 - 活跃线程: {}, 队列大小: {}, 已完成: {}, 是否丢弃: {}",
                executorName,
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount(),
                discard);

        if (discard) {
            log.warn("线程池 [{}] 丢弃任务: {}", executorName, r.getClass().getSimpleName());
        } else {
            log.warn("线程池 [{}] 由调用线程执行任务: {}", executorName);
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
}
