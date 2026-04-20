package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisProductRepository extends ServiceImpl<ProductMapper, Product> implements ProductRepository {

    @Override
    public Product findById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<Product> findByIds(List<Long> ids) {
        return baseMapper.selectBatchIds(ids);
    }

    @Override
    public List<Product> findByUserId(Long userId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getUserId, userId)
                        .orderByDesc(Product::getCreateTime)
        );
    }

    @Override
    public boolean save(Product product) {
        return baseMapper.insert(product) > 0;
    }

    @Override
    public boolean update(Product product) {
        return baseMapper.updateById(product) > 0;
    }

    @Override
    public void updateStock(Long productId, int delta) {
        if (delta > 0) {
            baseMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, productId)
                    .setSql("stock = stock + " + delta));
        } else if (delta < 0) {
            baseMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, productId)
                    .gt(Product::getStock, 0)
                    .setSql("stock = stock " + delta));
        }
    }

    @Override
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public boolean existsById(Long id) {
        return baseMapper.selectById(id) != null;
    }
}
