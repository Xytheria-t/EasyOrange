package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryTreeResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryPort.CategoryRecord;
import com.cartethyia.easyorange.common.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final AdminCategoryPort adminCategoryPort;

    private static final int MAX_CATEGORY_LEVEL = 3;

    public List<CategoryResponse> listCategories(String parentId) {
        List<CategoryRecord> entities = adminCategoryPort.listCategories(parentId);

        Map<String, Long> productCountMap = countProductMaps(entities);
        Map<String, String> parentNameMap = buildParentNameMap(entities);

        return entities.stream()
                .map(cat -> toCategoryResponse(cat, productCountMap, parentNameMap))
                .collect(Collectors.toList());
    }

    public List<CategoryTreeResponse> categoryTree() {
        Map<String, List<CategoryRecord>> groupedByParent = adminCategoryPort.listCategories(null).stream()
                .filter(cat -> Objects.equals(cat.status(), 1))
                .collect(Collectors.groupingBy(
                        cat -> cat.parentId() != null ? cat.parentId() : "0", LinkedHashMap::new, Collectors.toList()));
        return buildChildren(groupedByParent, "0");
    }

    private List<CategoryTreeResponse> buildChildren(
            Map<String, List<CategoryRecord>> groupedByParent, String parentId) {
        return groupedByParent.getOrDefault(parentId, List.of()).stream()
                .map(cat -> new CategoryTreeResponse(
                        cat.id(),
                        cat.name(),
                        cat.level(),
                        cat.sortOrder(),
                        cat.status(),
                        buildChildren(groupedByParent, cat.id())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        int level = 1;
        if (request.parentId() != null) {
            CategoryRecord parent = adminCategoryPort.getCategory(request.parentId());
            if (parent == null) {
                throw BusinessException.of("父分类不存在");
            }
            level = parent.level() + 1;
            if (level > MAX_CATEGORY_LEVEL) {
                throw BusinessException.of("分类层级不能超过" + MAX_CATEGORY_LEVEL + "级");
            }
            checkDuplicateName(request.name(), request.parentId());
        } else {
            checkDuplicateNameAtRoot(request.name());
        }

        CategoryRecord created =
                adminCategoryPort.createCategory(request.name(), request.parentId(), request.sortOrder(), level);

        return toCategoryResponse(created, Map.of(), Map.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse updateCategory(String id, CategoryUpdateRequest request) {
        CategoryRecord entity = adminCategoryPort.getCategory(id);
        if (entity == null) {
            throw BusinessException.of("分类不存在");
        }

        String parentId = entity.parentId();
        Integer level = entity.level();

        if (request.parentId() != null && !Objects.equals(request.parentId(), entity.parentId())) {
            CategoryRecord parent = adminCategoryPort.getCategory(request.parentId());
            if (parent == null) {
                throw BusinessException.of("父分类不存在");
            }
            int newLevel = parent.level() + 1;
            if (newLevel > MAX_CATEGORY_LEVEL) {
                throw BusinessException.of("移动后分类层级不能超过" + MAX_CATEGORY_LEVEL + "级");
            }
            level = newLevel;
            parentId = request.parentId();
        } else if (request.parentId() == null) {
            parentId = null;
            level = 1;
        }

        String name = entity.name();
        if (!Objects.equals(request.name(), entity.name())) {
            if (parentId != null) {
                checkDuplicateName(request.name(), parentId);
            } else {
                checkDuplicateNameAtRoot(request.name());
            }
            name = request.name();
        }

        CategoryRecord updated = new CategoryRecord(
                entity.id(),
                name,
                parentId,
                level,
                entity.icon(),
                request.sortOrder() != null ? request.sortOrder() : entity.sortOrder(),
                entity.status(),
                entity.createTime());

        adminCategoryPort.updateCategory(updated);

        Map<String, Long> productCountMap = countProductMaps(List.of(updated));
        return toCategoryResponse(updated, productCountMap, Map.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String id, Integer status) {
        CategoryRecord entity = adminCategoryPort.getCategory(id);
        if (entity == null) {
            throw BusinessException.of("分类不存在");
        }
        adminCategoryPort.updateCategory(new CategoryRecord(
                entity.id(),
                entity.name(),
                entity.parentId(),
                entity.level(),
                entity.icon(),
                entity.sortOrder(),
                status,
                entity.createTime()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id) {
        CategoryRecord entity = adminCategoryPort.getCategory(id);
        if (entity == null) {
            throw BusinessException.of("分类不存在");
        }

        if (adminCategoryPort.countCategoryChildren(id) > 0) {
            throw BusinessException.of("该分类下存在子分类，无法删除");
        }

        if (countProductsByCategoryId(id) > 0) {
            throw BusinessException.of("该分类下存在关联商品，无法删除");
        }

        adminCategoryPort.deleteCategory(id);
    }

    private void checkDuplicateName(String name, String parentId) {
        CategoryRecord existing = adminCategoryPort.findCategoryByName(name);
        if (existing != null && Objects.equals(existing.parentId(), parentId)) {
            throw BusinessException.of("同级下已存在同名分类");
        }
    }

    private void checkDuplicateNameAtRoot(String name) {
        CategoryRecord existing = adminCategoryPort.findCategoryByName(name);
        if (existing != null && existing.parentId() == null) {
            throw BusinessException.of("已存在同名的一级分类");
        }
    }

    private Map<String, Long> countProductMaps(List<CategoryRecord> categories) {
        if (categories == null || categories.isEmpty()) {
            return Map.of();
        }
        List<String> ids = categories.stream().map(CategoryRecord::id).collect(Collectors.toList());
        return adminCategoryPort.countProductsByCategoryIds(ids);
    }

    private Long countProductsByCategoryId(String categoryId) {
        Map<String, Long> result = adminCategoryPort.countProductsByCategoryIds(List.of(categoryId));
        return result.getOrDefault(categoryId, 0L);
    }

    private Map<String, String> buildParentNameMap(List<CategoryRecord> categories) {
        Set<String> parentIds = categories.stream()
                .map(CategoryRecord::parentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (parentIds.isEmpty()) {
            return Map.of();
        }

        List<CategoryRecord> parents =
                adminCategoryPort.getCategoriesByIds(parentIds.stream().toList());
        return parents.stream().collect(Collectors.toMap(CategoryRecord::id, CategoryRecord::name, (a, b) -> a));
    }

    private CategoryResponse toCategoryResponse(
            CategoryRecord cat, Map<String, Long> productCountMap, Map<String, String> parentNameMap) {
        return new CategoryResponse(
                cat.id(),
                cat.name(),
                cat.parentId(),
                cat.parentId() != null ? parentNameMap.get(cat.parentId()) : null,
                cat.level(),
                cat.sortOrder(),
                cat.status(),
                productCountMap.getOrDefault(cat.id(), 0L),
                cat.createTime(),
                null);
    }
}
