package com.cartethyia.easyorange.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.entity.Category;
import com.cartethyia.easyorange.product.mapper.CategoryMapper;
import com.cartethyia.easyorange.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> getByParentId(Long parentId) {
        BizRequire.notNull(parentId, "父分类 ID 不能为空");
        BizRequire.positive(parentId, "父分类 ID 必须为正数");
        return lambdaQuery()
                .eq(Category::getParentId, parentId)
                .orderByAsc(Category::getSortOrder)
                .list();
    }

    @Override
    public List<Category> getByLevel(Integer level) {
        BizRequire.notNull(level, "分类层级不能为空");
        BizRequire.positive(level, "分类层级必须为正数");
        BizRequire.between(level, 1, 5, "分类层级必须在 1-5 之间");
        return lambdaQuery()
                .eq(Category::getLevel, level)
                .orderByAsc(Category::getSortOrder)
                .list();
    }

    @Override
    public Category getByName(String name) {
        BizRequire.notBlank(name, "分类名称不能为空");
        BizRequire.between(name.length(), 1, 50, "分类名称长度必须在 1-50 之间");
        return lambdaQuery()
                .eq(Category::getName, name)
                .one();
    }
}
