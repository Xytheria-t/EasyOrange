package com.cartethyia.easyorange.product.adapter.outbound.persistence.category;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.product.domain.aggregate.Category;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 分类 DO ↔ 聚合的双向翻译 — 唯一持有 {@code CategoryDO ↔ Category} 字段映射的地方。
 * <p>
 * 只管「新增」与「读取」两个方向：更新走 {@code CategoryRepositoryImpl} 的
 * {@code LambdaUpdateWrapper}，因为 MyBatis-Plus 的 {@code updateById} 会跳过 null 字段，
 * 而「移到一级」必须能把 {@code parent_id} 写成 NULL。
 */
@Component
@RequiredArgsConstructor
public class CategoryDataMapper {

    private final IdGenerator idGenerator;

    /** 聚合 → DO（新增时分配 UUID v7 主键：{@code BaseDO.id} 是 {@code IdType.INPUT}，MP 不兜底）。 */
    public CategoryDO toEntity(Category category) {
        CategoryDO entity = CategoryDO.builder()
                .name(category.getName().value())
                .parentId(category.getParentId() != null ? category.getParentId().value() : null)
                .level(category.getLevel())
                .icon(category.getIcon())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .build();
        entity.setId(category.getId() != null ? category.getId().value() : idGenerator.generateId());
        if (category.getCreateTime() != null) {
            entity.setCreateTime(category.getCreateTime());
        }
        return entity;
    }

    /** DO → 聚合。 */
    public Category toAggregate(CategoryDO entity) {
        return Category.builder()
                .id(CategoryId.of(entity.getId()))
                .name(CategoryName.of(entity.getName()))
                .parentId(entity.getParentId() != null ? CategoryId.of(entity.getParentId()) : null)
                .level(entity.getLevel())
                .icon(entity.getIcon())
                .sortOrder(entity.getSortOrder())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
