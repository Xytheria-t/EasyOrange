package com.cartethyia.easyorange.product.adapter.outbound.persistence.category;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Category;
import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import com.cartethyia.easyorange.product.domain.repository.CategoryRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryRepositoryImpl extends BaseRepository<CategoryMapper, CategoryDO> implements CategoryRepository {

    private final CategoryDataMapper categoryDataMapper;

    public CategoryRepositoryImpl(CategoryMapper categoryMapper, CategoryDataMapper categoryDataMapper) {
        super(categoryMapper);
        this.categoryDataMapper = categoryDataMapper;
    }

    /** 新增分类 — 主键由 {@code CategoryDataMapper} 分配，绝不与 save 的 upsert 语义混用。 */
    @Override
    public Category insert(Category category) {
        CategoryDO entity = categoryDataMapper.toEntity(category);
        mapper.insert(entity);
        return category;
    }

    /**
     * 更新分类 — 保留主键与乐观锁版本，只覆盖可变字段。
     * <p>
     * 用 {@code LambdaUpdateWrapper} 而不是 {@code updateById}：后者走 MyBatis-Plus 默认的
     * {@code FieldStrategy.NOT_NULL}，会把 null 字段整个跳过 SET 子句 —— 而「移到一级」
     * 恰恰要把 {@code parent_id} 写成 NULL。wrapper 的 {@code set} 不受该策略约束，
     * 这也是这个 bug 当初能一路漏出去的原因（单测 mock 掉 mapper，看不到真实 SQL）。
     */
    @Override
    public Category update(Category category) {
        requireById(category.getId().value());

        var wrapper = Wrappers.<CategoryDO>lambdaUpdate()
                .eq(CategoryDO::getId, category.getId().value())
                .set(CategoryDO::getName, category.getName().value())
                .set(
                        CategoryDO::getParentId,
                        category.getParentId() != null ? category.getParentId().value() : null)
                .set(CategoryDO::getLevel, category.getLevel())
                .set(CategoryDO::getIcon, category.getIcon())
                .set(CategoryDO::getSortOrder, category.getSortOrder())
                .set(CategoryDO::getStatus, category.getStatus())
                .set(
                        CategoryDO::getUpdateTime,
                        category.getUpdateTime() != null ? category.getUpdateTime() : LocalDateTime.now());
        mapper.update(null, wrapper);
        return category;
    }

    @Override
    public void delete(CategoryId id) {
        mapper.deleteById(id.value());
    }

    @Override
    public Optional<Category> findById(CategoryId id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectById(id.value())).map(categoryDataMapper::toAggregate);
    }

    @Override
    public List<Category> findByIds(List<CategoryId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectByIds(ids.stream().map(CategoryId::value).toList()).stream()
                .map(categoryDataMapper::toAggregate)
                .toList();
    }

    @Override
    public List<Category> findChildren(CategoryId parentId) {
        return lambdaQuery()
                .eq(parentId != null, CategoryDO::getParentId, parentId != null ? parentId.value() : null)
                .isNull(parentId == null, CategoryDO::getParentId)
                .orderByAsc(CategoryDO::getSortOrder)
                .list()
                .stream()
                .map(categoryDataMapper::toAggregate)
                .toList();
    }

    @Override
    public List<CategoryId> findSubtreeIds(CategoryId root) {
        if (root == null) {
            return lambdaQuery().orderByAsc(CategoryDO::getLevel).orderByAsc(CategoryDO::getSortOrder).list().stream()
                    .map(CategoryDO::getId)
                    .map(CategoryId::of)
                    .toList();
        }
        return mapper.selectSubtreeIds(root.value()).stream()
                .map(CategoryId::of)
                .toList();
    }

    @Override
    public List<Category> findSiblings(CategoryId parentId, String excludeId) {
        return lambdaQuery()
                .eq(parentId != null, CategoryDO::getParentId, parentId != null ? parentId.value() : null)
                .isNull(parentId == null, CategoryDO::getParentId)
                .ne(excludeId != null && !excludeId.isBlank(), CategoryDO::getId, excludeId)
                .list()
                .stream()
                .map(categoryDataMapper::toAggregate)
                .toList();
    }

    @Override
    public long countAll() {
        return lambdaQuery().count();
    }

    @Override
    public List<Category> findAll() {
        return lambdaQuery().orderByAsc(CategoryDO::getSortOrder).list().stream()
                .map(categoryDataMapper::toAggregate)
                .toList();
    }

    private boolean existsById(String id) {
        return mapper.selectById(id) != null;
    }

    private CategoryDO requireById(String id) {
        CategoryDO entity = mapper.selectById(id);
        if (entity == null) {
            throw ProductDomainException.categoryNotFound(id);
        }
        return entity;
    }
}
