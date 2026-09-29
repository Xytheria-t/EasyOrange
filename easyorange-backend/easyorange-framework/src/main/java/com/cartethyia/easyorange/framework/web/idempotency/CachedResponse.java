package com.cartethyia.easyorange.framework.web.idempotency;

/** 幂等命中后逐字节回放的成功响应（body 序列化自控制器返回值，contentType 可为 null）。 */
public record CachedResponse(int status, String contentType, byte[] body) {}
