package com.cartethyia.easyorange.admin.domain.model;

import java.util.List;

/**
 * 批量审核结果 — 「部分成功 + 失败明细」是应用层的既成事实，不是展示层拼出来的。
 * <p>
 * 只存 total / success / errors：failed 恒等于 {@code errors.size()}，存两份就是两份可能对不上的真相。
 */
public record BatchAuditResult(int total, int success, List<String> errors) {

    public BatchAuditResult {
        errors = errors == null ? List.of() : errors;
    }
}
