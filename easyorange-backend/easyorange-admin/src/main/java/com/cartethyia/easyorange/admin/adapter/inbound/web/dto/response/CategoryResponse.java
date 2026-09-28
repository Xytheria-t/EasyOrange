package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台分类响应 — 字段名与 C 端 {@code product.CategoryResponse} 统一用 {@code id}。
 * <p>
 * 旧版这里叫 {@code categoryId}、C 端叫 {@code id}，同一个概念两套命名逼着前端维护两套类型。
 * <p>
 * {@code children} 取代了原先单独的 {@code CategoryTreeResponse}：树结构只是本 DTO 的一个
 * 可空字段，不必再养一套平行类型。
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
