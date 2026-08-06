package com.cartethyia.easyorange.framework.web.idempotency;

/**
 * 幂等缓存的一次成功响应快照。
 * <p>
 * 由 {@link com.cartethyia.easyorange.framework.web.filter.IdempotencyKeyFilter}
 * 抓取「序列化后的 HTTP 响应」而非控制器返回的类型化对象，重复请求按此逐字节回放。
 * </p>
 *
 * @param status       HTTP 状态码
 * @param contentType  响应 Content-Type（可为 null）
 * @param body         响应体字节
 */
public record CachedResponse(int status, String contentType, byte[] body) {}
