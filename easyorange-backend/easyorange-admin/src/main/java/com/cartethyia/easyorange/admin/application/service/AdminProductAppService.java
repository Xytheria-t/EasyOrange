package com.cartethyia.easyorange.admin.application.service;

import com.cartethyia.easyorange.admin.domain.exception.AdminDomainException;
import com.cartethyia.easyorange.admin.domain.model.ProductDetailView;
import com.cartethyia.easyorange.admin.domain.model.ProductListView;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.PartyProfile;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台商品查询 / 上下架 — 出入参只用 {@code domain} 的端口记录与读模型。
 * <p>
 * <b>取舍</b>：图片不在商品记录里（独立存储），列表与详情都要显示，故按当页 id 批量取一次，
 * 随结果包成 {@link ProductListView} / {@link ProductDetailView} 返回；主图取哪一张留给 assembler 决定。
 * <p>
 * <b>边界</b>：商品不存在抛 {@link AdminDomainException#productNotFound}；状态转换是否合法由 product 侧的
 * 聚合守卫裁决，端口只翻状态码，本类不复制一份状态机。
 */
@Service
@RequiredArgsConstructor
public class AdminProductAppService {

    private final AdminProductPort adminProductPort;

    @Transactional(readOnly = true)
    public ProductListView listProducts(ProductQueryCondition condition) {
        ProductQueryResult page = adminProductPort.queryProducts(condition);
        List<String> productIds =
                page.records().stream().map(ProductSummary::id).toList();
        return new ProductListView(
                page, adminProductPort.getProductImages(productIds), adminProductPort.getPartyProfiles(productIds));
    }

    @Transactional(readOnly = true)
    public ProductDetailView getProductDetail(String id) {
        ProductDetail product = adminProductPort.getProductDetail(id);
        if (product == null) {
            throw AdminDomainException.productNotFound(id);
        }
        return new ProductDetailView(
                product,
                adminProductPort.getProductImages(List.of(id)).getOrDefault(id, List.of()),
                adminProductPort.getPartyProfiles(List.of(id)).getOrDefault(id, PartyProfile.empty()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProductStatus(String id, String status) {
        adminProductPort.applyProductStatus(id, status);
    }
}
