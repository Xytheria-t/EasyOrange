package com.cartethyia.easyorange.framework.config.async;

import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * 复制主线程 MDC 到异步线程，保证 traceId 等上下文在跨线程时不丢失；
 * 子线程执行完成后清理 MDC，避免线程复用污染。
 */
@NullMarked
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
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
