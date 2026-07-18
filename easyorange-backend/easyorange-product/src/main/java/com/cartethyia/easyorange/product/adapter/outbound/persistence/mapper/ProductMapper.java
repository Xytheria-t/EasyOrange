package com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.product.application.query.SellerInfo;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductImageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface ProductMapper extends BaseMapper<ProductDO> {

    // -- 搜索 --

    Page<ProductDO> searchByFullText(Page<ProductDO> page,
                                     @Param("keyword") String keyword,
                                     @Param("status") Integer status,
                                     @Param("minPrice") BigDecimal minPrice,
                                     @Param("maxPrice") BigDecimal maxPrice,
                                     @Param("conditionLevel") Integer conditionLevel,
                                     @Param("hasDiscount") Boolean hasDiscount);

    // -- 批量查询 --

    List<ProductDetailDO> selectDetailsByProductIds(@Param("productIds") List<String> productIds);

    List<ProductImageDO> selectImagesByProductIds(@Param("productIds") List<String> productIds);

    List<CategoryDO> selectCategoriesByIds(@Param("categoryIds") List<String> categoryIds);

    List<SellerInfo> selectSellersByIds(@Param("sellerIds") Set<String> sellerIds);

    // -- 写入 --

    void batchAddViewCounts(@Param("viewCounts") Map<String, Integer> viewCounts);

    void updateSearchText(@Param("productId") String productId,
                          @Param("searchText") String searchText);

    int updateStatus(@Param("id") String id, @Param("status") Integer status, @Param("version") Integer version);
}
