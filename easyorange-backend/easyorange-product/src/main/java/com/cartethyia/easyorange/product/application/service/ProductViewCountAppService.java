package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.application.port.cache.ViewCountPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductViewCountAppService {

    private final ViewCountPort viewCountPort;

    public void incrementViewCount(String productId) {
        if (productId == null) return;
        try {
            viewCountPort.increment(productId);
        } catch (Exception e) {
            log.warn("记录浏览量失败: productId={}", productId, e);
        }
    }
}
