package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 资产详情适配器 — 实现 ai 模块定义的 {@link AssetDetailPort}：Agent 循环 product_detail
 * 工具按 ID 查在售资产详情。经 product 模块查询仓储读 {@link ProductReadModel}
 * （与 {@code FavoriteProductInfoAdapter} 同向：端口由消费方定义，本模块翻译实现）。
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

    private AssetDetail toDetail(ProductReadModel model) {
        return new AssetDetail(
                model.id(),
                model.title(),
                model.description(),
                model.price(),
                model.categoryName(),
                model.conditionDesc(),
                model.location(),
                model.username(),
                model.status());
    }
}
