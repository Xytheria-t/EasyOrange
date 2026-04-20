package com.cartethyia.easyorange.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
        return lambdaQuery()
                .eq(Category::getParentId, parentId)
                .orderByAsc(Category::getSortOrder)
                .list();
    }

    @Override
    public List<Category> getByLevel(Integer level) {
        return lambdaQuery()
                .eq(Category::getLevel, level)
                .orderByAsc(Category::getSortOrder)
                .list();
    }

    @Override
    public Category getByName(String name) {
        return lambdaQuery()
                .eq(Category::getName, name)
                .one();
    }
}
