package com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.product.application.query.dto.SellerInfo;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductImageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Mapper
public interface ProductMapper extends BaseMapper<ProductDO> {

    Page<ProductDO> searchByFullText(Page<ProductDO> page,
                                    @Param("keyword") String keyword,
                                    @Param("status") Integer status);

    Page<ProductDO> searchByFullText(Page<ProductDO> page,
                                    @Param("keyword") String keyword,
                                    @Param("status") Integer status,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("conditionLevel") Integer conditionLevel,
                                    @Param("hasDiscount") Boolean hasDiscount);

    List<ProductDetailDO> selectDetailsByProductIds(@Param("productIds") List<String> productIds);

    List<ProductImageDO> selectImagesByProductIds(@Param("productIds") List<String> productIds);

    List<CategoryDO> selectCategoriesByIds(@Param("categoryIds") List<String> categoryIds);

    List<SellerInfo> selectSellersByIds(@Param("sellerIds") Set<String> sellerIds);

    void batchAddViewCounts(@Param("viewCounts") java.util.Map<String, Integer> viewCounts);

    void updateSearchText(@Param("productId") String productId,
                          @Param("searchText") String searchText);
}
