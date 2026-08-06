package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.product.application.query.ProductQueryHandler;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class OrderProductQueryAdapter implements ProductQueryPort {

    private final ProductQueryHandler productQueryHandler;

    @Override
    public Optional<ProductDetail> getProductById(String productId) {
        try {
            ProductVO product = productQueryHandler.getProductById(productId);
            return Optional.ofNullable(product).map(this::toDetail);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<ProductDetail> getProductsByIds(List<String> productIds) {
        List<ProductVO> products = productQueryHandler.getProductsByIds(productIds);
        if (products == null) {
            return List.of();
        }
        return products.stream().map(this::toDetail).toList();
    }

    private ProductDetail toDetail(ProductVO p) {
        return new ProductDetail(
                p.getId(),
                p.getTitle(),
                p.getPrice(),
                p.getStatus(),
                p.getImages(),
                p.getDescription(),
                p.getConditionDesc());
    }
}
