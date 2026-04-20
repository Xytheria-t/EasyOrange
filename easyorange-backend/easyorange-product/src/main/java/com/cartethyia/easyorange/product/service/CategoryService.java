package com.cartethyia.easyorange.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.product.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> getByParentId(Long parentId);

    List<Category> getByLevel(Integer level);

    Category getByName(String name);
}
