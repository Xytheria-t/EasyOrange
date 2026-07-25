package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class PlatformStatsController {

    private final UserRepository userRepository;
    private final ProductQueryRepository productQueryRepository;
    private final OrderReadRepository orderReadRepository;

    @GetMapping("/platform")
    public Result<PlatformStatsVO> getPlatformStats() {
        long activeUsers = userRepository.count();
        long onlineProducts = productQueryRepository.countByStatus(ProductStatus.ONLINE.getCode());
        long completedOrders = orderReadRepository.countByStatus(OrderStatus.COMPLETED);

        return Result.success(new PlatformStatsVO(activeUsers, onlineProducts, completedOrders));
    }

    public record PlatformStatsVO(long activeUsers, long onlineProducts, long completedOrders) {
    }
}