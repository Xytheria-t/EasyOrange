package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台分类响应 — 字段名与 C 端 {@code product.CategoryResponse} 统一用 {@code id}。
 * <p>
 * 用 {@code id} 而非 {@code categoryId}，是与 C 端同名字段对齐——同一个概念两套命名会逼前端维护两套类型。
 * <p>
 * {@code children} 做成可空字段而不另立树形 DTO：树结构只是本 DTO 的一个字段，不值得再养一套平行类型。
 *
 * @param children 子分类；仅 {@code /tree} 接口非空，列表接口为空列表
 */
public record CategoryResponse(
        String id,
        String name,
        /** 一级分类为 null。 */
        String parentId,
        String parentName,
        Integer level,
        Integer sortOrder,
        Integer status,
        Long productCount,
        LocalDateTime createTime,
        List<CategoryResponse> children) {}
