package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 资产详情适配器 — 实现 ai 模块定义的 {@link AssetDetailPort}：Agent 循环 product_detail
 * 工具按 ID 查在售资产详情。经 product 模块查询仓储读 {@link ProductReadModel}
 * （端口由消费方定义，本模块翻译实现）。
 * <p>
 * 读模型只带 {@code eo_product} 单表字段，所以描述（副表）与卖家名（用户表）在这里**按件补**——
 * 详情是单件工具调用，多两次查询可接受（列表路径不许这么干）。地区按商品详情页同口径脱敏后交给模型：
 * 观察文本会进供应商请求、也会被回答引用，精确到门牌没有必要。
 */
@Component
@RequiredArgsConstructor
public class AssetDetailAdapter implements AssetDetailPort {

    private final ProductQueryRepository productQueryRepository;

    @Override
    public Optional<AssetDetail> findDetail(String productId) {
        return Optional.ofNullable(productQueryRepository.findProductById(productId))
                .map(this::toDetail);
    }

    /**
     * 批量查详情 — compare_assets 一次对比多件候选，逐个 findDetail 会放大成 3N 次查询
     * （每件再分查描述与卖家）。这里先把入参一次性取回商品，描述与卖家各走一次批量，
     * 固定 3 次查询，与候选数量无关。
     */
    @Override
    public List<AssetDetail> findDetails(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<ProductReadModel> models = productQueryRepository.findProductsByIds(productIds);
        if (models.isEmpty()) {
            return List.of();
        }
        Map<String, String> descriptions =
                productQueryRepository
                        .findDetailsByProductIds(
                                models.stream().map(ProductReadModel::id).toList())
                        .stream()
                        .collect(Collectors.toMap(
                                ProductQueryRepository.ProductDetailInfo::productId,
                                ProductQueryRepository.ProductDetailInfo::description));
        Map<String, String> sellerNames = productQueryRepository
                .findSellersByIds(models.stream()
                        .map(ProductReadModel::sellerId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(
                        SellerReadModel::id,
                        seller -> seller.nickName() != null ? seller.nickName() : seller.username(),
                        (a, b) -> a));
        return models.stream()
                .map(model -> toDetail(model, descriptions, sellerNames))
                .toList();
    }

    private AssetDetail toDetail(ProductReadModel model) {
        return new AssetDetail(
                model.id(),
                model.title(),
                findDescription(model.id()),
                model.price(),
                model.categoryName(),
                model.conditionDesc(),
                MaskUtils.maskAddress(model.location(), 6),
                findSellerName(model.sellerId()),
                model.status());
    }

    private AssetDetail toDetail(
            ProductReadModel model, Map<String, String> descriptions, Map<String, String> sellerNames) {
        return new AssetDetail(
                model.id(),
                model.title(),
                descriptions.get(model.id()),
                model.price(),
                model.categoryName(),
                model.conditionDesc(),
                MaskUtils.maskAddress(model.location(), 6),
                model.sellerId() == null ? null : sellerNames.get(model.sellerId()),
                model.status());
    }

    private String findDescription(String productId) {
        return productQueryRepository.findDetailsByProductIds(List.of(productId)).stream()
                .findFirst()
                .map(ProductQueryRepository.ProductDetailInfo::description)
                .orElse(null);
    }

    /** 卖家展示名与商品 VO 同口径：昵称优先，退用户名。 */
    private String findSellerName(String sellerId) {
        if (sellerId == null) {
            return null;
        }
        return productQueryRepository.findSellersByIds(Set.of(sellerId)).stream()
                .findFirst()
                .map(seller -> seller.nickName() != null ? seller.nickName() : seller.username())
                .orElse(null);
    }
}
