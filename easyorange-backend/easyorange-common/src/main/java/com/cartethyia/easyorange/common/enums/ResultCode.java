package com.cartethyia.easyorange.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用结果码枚举
 * <p>
 * 前缀规则：A = 成功/客户端语义（含认证授权） | B = 业务错误 | C = 服务端错误 | D = 第三方错误
 * </p>
 * <p>
 * A 段编号布局：A + 4 位 HTTP 状态码 + 可选子码数字。A0000 成功；0401=401 未登录 /
 * 04011=401 子码·登录已过期（0402 为 HTTP 402 语义，故子码不再占用真实 4xx）/ 0403=403 禁止 /
 * 0404=404 不存在 / 0405=405 方法不允许 / 0429=429 限流。HTTP 状态由码内数字自动推导（单一来源
 * 见 {@link IResultCode#resolveStatus(String)}），新增 A04xx 家族码无需改映射。
 * B 段通用码：B0001=通用失败（{@code Result.error(String)} 兜底）、B0002=未指定码的业务异常兜底、
 * B0003=参数校验失败（@Valid/约束/参数缺失/解析错误）、B0004=请求形态错误（如 415 媒体类型）、
 * B0005 预留。HTTP 状态映射单一来源见 {@link IResultCode#resolveStatus(String)}。
 * </p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode implements IResultCode {

    // A: 成功 / 认证授权
    SUCCESS("A0000", "成功"),
    UNAUTHORIZED("A0401", "未登录"),
    TOKEN_EXPIRED("A04011", "登录已过期"),
    FORBIDDEN("A0403", "禁止访问"),
    NOT_FOUND("A0404", "资源不存在"),
    METHOD_NOT_ALLOWED("A0405", "请求方法不允许"),
    TOO_MANY_REQUESTS("A0429", "请求过于频繁"),

    // B: 业务错误
    FAIL("B0001", "操作失败"),
    BUSINESS_ERROR("B0002", "业务异常"),
    VALIDATE_FAILED("B0003", "参数校验失败"),
    PARAM_ERROR("B0004", "参数错误"),
    CONCURRENT_UPDATE("B0006", "并发更新冲突"),

    // C: 服务端错误
    INTERNAL_SERVER_ERROR("C0500", "服务器内部错误"),

    // D: 第三方错误
    UPSTREAM_ERROR("D0502", "上游服务不可用");

    private final String code;
    private final String message;
}
