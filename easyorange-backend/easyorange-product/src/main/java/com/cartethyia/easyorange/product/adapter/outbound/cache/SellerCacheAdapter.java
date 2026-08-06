package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.product.application.port.cache.SellerCachePort;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class SellerCacheAdapter implements SellerCachePort {

    private final ProductQueryRepository productQueryRepository;

    private final Cache<String, SellerReadModel> sellerCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    @Override
    public Map<String, SellerReadModel> getSellers(Set<String> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return Map.of();
        }

        var result = new HashMap<String, SellerReadModel>();
        var missingIds = new HashSet<String>();

        for (var id : sellerIds) {
            var cached = sellerCache.getIfPresent(id);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            var fetched = productQueryRepository.findSellersByIds(missingIds).stream()
                    .collect(Collectors.toMap(SellerReadModel::id, s -> s, (a, _) -> a));
            sellerCache.putAll(fetched);
            result.putAll(fetched);
        }

        return result;
    }
}
