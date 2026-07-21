package com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.product.application.query.SellerInfo;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.ProductImageDO;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Mapper
public interface ProductMapper extends BaseMapper<ProductDO> {

    // -- 搜索 --

    record ProductSearchCriteria(
        String keyword,
        ProductStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ConditionLevel conditionLevel,
        Boolean hasDiscount
    ) {}

    Page<ProductDO> searchByFullText(Page<ProductDO> page, @Param("c") ProductSearchCriteria criteria);

    // -- 批量查询 --

    List<ProductDetailDO> selectDetailsByProductIds(@Param("productIds") List<String> productIds);

    List<ProductImageDO> selectImagesByProductIds(@Param("productIds") List<String> productIds);

    List<CategoryDO> selectCategoriesByIds(@Param("categoryIds") List<String> categoryIds);

    List<SellerInfo> selectSellersByIds(@Param("sellerIds") Set<String> sellerIds);

    // -- 写入 --

    record ViewCountEntry(String productId, int count) {}

    void batchAddViewCounts(@Param("entries") List<ViewCountEntry> entries);

    void updateSearchText(@Param("productId") String productId,
                          @Param("searchText") String searchText);

}
