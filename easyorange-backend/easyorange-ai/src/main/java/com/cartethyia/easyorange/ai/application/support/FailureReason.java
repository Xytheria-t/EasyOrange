package com.cartethyia.easyorange.ai.application.support;

/** 异常 → 可日志化原因：message 为空时退回类名，决策/循环/工具三处共用一份口径。 */
public final class FailureReason {

    private FailureReason() {}

    public static String of(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
