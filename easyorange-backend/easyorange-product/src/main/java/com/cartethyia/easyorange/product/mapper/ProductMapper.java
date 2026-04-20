package com.cartethyia.easyorange.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.product.dto.vo.SellerInfo;
import com.cartethyia.easyorange.product.entity.Category;
import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 全文搜索商品（支持分页）
     */
    Page<Product> searchByFullText(Page<Product> page,
                                    @Param("keyword") String keyword,
                                    @Param("status") Integer status);

    /**
     * 批量查询商品详情
     */
    List<ProductDetail> selectDetailsByProductIds(@Param("productIds") List<Long> productIds);

    /**
     * 批量查询商品图片
     */
    List<ProductImage> selectImagesByProductIds(@Param("productIds") List<Long> productIds);

    /**
     * 批量查询分类
     */
    List<Category> selectCategoriesByIds(@Param("categoryIds") List<Long> categoryIds);

    /**
     * 批量查询卖家信息
     */
    List<SellerInfo> selectSellersByIds(@Param("sellerIds") Set<Long> sellerIds);
}
