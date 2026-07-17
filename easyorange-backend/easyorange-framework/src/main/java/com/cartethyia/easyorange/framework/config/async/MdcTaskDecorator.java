package com.cartethyia.easyorange.framework.config.async;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * 复制主线程 MDC 到异步线程，保证 traceId 等上下文在跨线程时不丢失；
 * 子线程执行完成后清理 MDC，避免线程复用污染。
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(@NonNull Runnable runnable) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            if (context != null) {
                MDC.setContextMap(context);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
