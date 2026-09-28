package com.cartethyia.easyorange.admin.domain.model;

/**
 * 分类更新命令 — 一次请求里「改属性」与「改挂载点」是同一件事的两面。
 * <p>
 * 合成一个命令而不是五个散参：{@code parentId} 参与「是否移动」的判定，调用方自己拆开传
 * 迟早会漏掉它，而漏掉的后果是静默丢失挂载点更新。
 * <p>
 * 不加 Lombok Builder：构造点只有 Controller 一处，构造器直给最省事。
 */
public record CategoryUpdateCommand(
        String name,
        /** 父分类 id；null 表示移到一级。 */
        String parentId,
        String icon,
        Integer sortOrder,
        Integer status) {}
