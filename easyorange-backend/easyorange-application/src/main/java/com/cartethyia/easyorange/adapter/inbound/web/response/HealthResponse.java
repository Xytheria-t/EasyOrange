package com.cartethyia.easyorange.adapter.inbound.web.response;

import java.time.Instant;

/**
 * 健康检查视图 — 探活只关心 200 + status 字段，时间戳用 UTC ISO-8601。
 */
public record HealthResponse(String status, Instant timestamp) {}
