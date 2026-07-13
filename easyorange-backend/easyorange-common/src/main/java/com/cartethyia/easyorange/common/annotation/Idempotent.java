package com.cartethyia.easyorange.common.annotation;

import java.lang.annotation.*;

/**
 * 标记 Controller 方法支持 Idempotency-Key 幂等协议。
 * <p>
 * 客户端在请求头中携带 {@code Idempotency-Key}（UUID），
 * 服务端缓存成功响应结果，相同 key 的重复请求直接返回缓存结果。
 * </p>
 * <p>
 * 适用场景：下单、创建支付、创建商品、提交举报等天然要求幂等的写操作。
 * 幂等 key 过期后（默认 24h）客户端可安全重试。
 * </p>
 * <pre>{@code
 * @PostMapping("/orders")
 * @Idempotent
 * public Result<String> createOrder(@RequestBody @Valid CreateOrderRequest req) {
 *     return Result.success(orderCommandHandler.handle(req));
 * }
 * }</pre>
 * <p>
 * 注意：此注解与 {@link SkipRepeatSubmit} 不同——前者提供协议级别的完整幂等（含响应缓存），
 * 后者仅做短时间（3s）防止连点。
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等 key 所在的请求头名称。
     */
    String headerName() default "Idempotency-Key";

    /**
     * 成功响应缓存的 TTL（秒），过期后可重试。
     * 默认 24 小时。
     */
    long ttlSeconds() default 86400;
}
