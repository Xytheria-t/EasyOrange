package com.cartethyia.easyorange.admin.util;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BatchQueryUtil {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(CommonConstant.DATETIME_FORMAT);

    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    public Map<String, UserDO> batchGetUsers(List<String> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(UserDO::getId, u -> u, (a, b) -> a));
    }

    public Map<String, ProductDO> batchGetProducts(List<String> productIds) {
        if (productIds.isEmpty()) return Map.of();
        return productMapper.selectByIds(productIds).stream()
                .collect(Collectors.toMap(ProductDO::getId, p -> p, (a, b) -> a));
    }
}
