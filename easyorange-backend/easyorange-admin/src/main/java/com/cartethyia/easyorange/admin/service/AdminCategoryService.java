package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryTreeResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.CategoryMapper;
import com.cartethyia.easyorange.product.domain.repository.query.CategoryQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryQueryRepository categoryQueryRepository;

    private static final int MAX_CATEGORY_LEVEL = 3;

    public List<CategoryResponse> listCategories(Long parentId) {
        List<CategoryDO> entities;
        if (parentId != null) {
            entities = categoryQueryRepository.findByParentId(parentId);
        } else {
            entities = ChainWrappers.lambdaQueryChain(categoryMapper)
                .eq(CategoryDO::getDelFlag, 0)
                .orderByAsc(CategoryDO::getSortOrder)
                .list();
        }

        Map<Long, Long> productCountMap = countProductMaps(entities);
        Map<Long, String> parentNameMap = buildParentNameMap(entities);

        return entities.stream()
            .map(cat -> toCategoryResponse(cat, productCountMap, parentNameMap))
            .collect(Collectors.toList());
    }

    public List<CategoryTreeResponse> categoryTree() {
        List<CategoryDO> all = ChainWrappers.lambdaQueryChain(categoryMapper)
            .eq(CategoryDO::getStatus, 1)
            .eq(CategoryDO::getDelFlag, 0)
            .orderByAsc(CategoryDO::getSortOrder)
            .list();

        Map<Long, List<CategoryDO>> groupedByParent = all.stream()
            .collect(Collectors.groupingBy(
                cat -> cat.getParentId() != null ? cat.getParentId() : 0L,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        Function<Long, List<CategoryTreeResponse>> buildChildren = new Function<>() {
            @Override
            public List<CategoryTreeResponse> apply(Long pid) {
                List<CategoryDO> children = groupedByParent.getOrDefault(pid, List.of());
                return children.stream()
                    .map(cat -> new CategoryTreeResponse(
                        cat.getId(),
                        cat.getName(),
                        cat.getLevel(),
                        cat.getSortOrder(),
                        cat.getStatus(),
                        apply(cat.getId())
                    ))
                    .collect(Collectors.toList());
            }
        };

        return buildChildren.apply(0L);
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        int level = 1;
        if (request.parentId() != null) {
            CategoryDO parent = categoryMapper.selectById(request.parentId());
            if (parent == null || parent.getDelFlag() != 0) {
                throw BusinessException.of("父分类不存在");
            }
            level = parent.getLevel() + 1;
            if (level > MAX_CATEGORY_LEVEL) {
                throw BusinessException.of("分类层级不能超过" + MAX_CATEGORY_LEVEL + "级");
            }
            checkDuplicateName(request.name(), request.parentId());
        } else {
            checkDuplicateNameAtRoot(request.name());
        }

        CategoryDO entity = CategoryDO.builder()
            .name(request.name())
            .parentId(request.parentId())
            .level(level)
            .sortOrder(request.sortOrder())
            .status(1)
            .build();

        categoryMapper.insert(entity);

        return toCategoryResponse(entity, Map.of(), Map.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        CategoryDO entity = categoryMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("分类不存在");
        }

        if (request.parentId() != null && !Objects.equals(request.parentId(), entity.getParentId())) {
            CategoryDO parent = categoryMapper.selectById(request.parentId());
            if (parent == null || parent.getDelFlag() != 0) {
                throw BusinessException.of("父分类不存在");
            }
            int newLevel = parent.getLevel() + 1;
            if (newLevel > MAX_CATEGORY_LEVEL) {
                throw BusinessException.of("移动后分类层级不能超过" + MAX_CATEGORY_LEVEL + "级");
            }
            entity.setLevel(newLevel);
            entity.setParentId(request.parentId());
        } else if (request.parentId() == null) {
            entity.setParentId(null);
            entity.setLevel(1);
        }

        if (!Objects.equals(request.name(), entity.getName())) {
            Long checkParentId = entity.getParentId() != null ? entity.getParentId() : null;
            if (checkParentId != null) {
                checkDuplicateName(request.name(), checkParentId);
            } else {
                checkDuplicateNameAtRoot(request.name());
            }
            entity.setName(request.name());
        }

        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }

        categoryMapper.updateById(entity);

        Map<Long, Long> productCountMap = countProductMaps(List.of(entity));
        return toCategoryResponse(entity, productCountMap, Map.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        CategoryDO entity = categoryMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("分类不存在");
        }
        entity.setStatus(status);
        categoryMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        CategoryDO entity = categoryMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("分类不存在");
        }

        long childCount = ChainWrappers.lambdaQueryChain(categoryMapper)
            .eq(CategoryDO::getParentId, id)
            .eq(CategoryDO::getDelFlag, 0)
            .count();
        if (childCount > 0) {
            throw BusinessException.of("该分类下存在子分类，无法删除");
        }

        long productCount = countProductsByCategoryId(id);
        if (productCount > 0) {
            throw BusinessException.of("该分类下存在关联商品，无法删除");
        }

        entity.setDelFlag(2);
        categoryMapper.updateById(entity);
    }

    private void checkDuplicateName(String name, Long parentId) {
        CategoryDO existing = categoryQueryRepository.findByName(name);
        if (existing != null && existing.getDelFlag() == 0 && Objects.equals(existing.getParentId(), parentId)) {
            throw BusinessException.of("同级下已存在同名分类");
        }
    }

    private void checkDuplicateNameAtRoot(String name) {
        CategoryDO existing = categoryQueryRepository.findByName(name);
        if (existing != null && existing.getDelFlag() == 0 && existing.getParentId() == null) {
            throw BusinessException.of("已存在同名的一级分类");
        }
    }

    private Map<Long, Long> countProductMaps(List<CategoryDO> categories) {
        if (categories == null || categories.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = categories.stream().map(CategoryDO::getId).collect(Collectors.toList());
        return categoryQueryRepository.countProductsByCategoryIds(ids);
    }

    private Long countProductsByCategoryId(Long categoryId) {
        Map<Long, Long> result = categoryQueryRepository.countProductsByCategoryIds(List.of(categoryId));
        return result.getOrDefault(categoryId, 0L);
    }

    private Map<Long, String> buildParentNameMap(List<CategoryDO> categories) {
        Set<Long> parentIds = categories.stream()
            .map(CategoryDO::getParentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (parentIds.isEmpty()) {
            return Map.of();
        }

        List<CategoryDO> parents = categoryQueryRepository.findByIds(new ArrayList<>(parentIds));
        return parents.stream()
            .collect(Collectors.toMap(CategoryDO::getId, CategoryDO::getName, (a, b) -> a));
    }

    private CategoryResponse toCategoryResponse(CategoryDO dobj, Map<Long, Long> productCountMap, Map<Long, String> parentNameMap) {
        return new CategoryResponse(
            dobj.getId(),
            dobj.getName(),
            dobj.getParentId(),
            dobj.getParentId() != null ? parentNameMap.get(dobj.getParentId()) : null,
            dobj.getLevel(),
            dobj.getSortOrder(),
            dobj.getStatus(),
            productCountMap.getOrDefault(dobj.getId(), 0L),
            dobj.getCreateTime(),
            dobj.getUpdateTime()
        );
    }
}
