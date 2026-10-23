package com.cartethyia.easyorange.ai.adapter.inbound.mcp;

import com.cartethyia.easyorange.ai.domain.model.AssetDetail;

/**
 * MCP 资产详情工具结果 — found 区分「不存在/已下架」与调用故障；不用 null 作工具结果根值
 * （部分 client 对空结果渲染不友好）。
 *
 * @param found  是否存在且在售
 * @param detail 资产详情，found=false 时为 null
 */
public record AssetDetailResult(boolean found, AssetDetail detail) {}
