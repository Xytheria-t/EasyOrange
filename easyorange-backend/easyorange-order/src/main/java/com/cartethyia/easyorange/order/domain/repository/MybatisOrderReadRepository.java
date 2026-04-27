package com.cartethyia.easyorange.order.domain.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.dto.request.QueryOrderRequest;
import com.cartethyia.easyorange.order.entity.Order;
import com.cartethyia.easyorange.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisOrderReadRepository implements OrderReadRepository {

    private final OrderMapper orderMapper;

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orderMapper.selectById(id));
    }

    @Override
    public List<Order> findByBuyerId(Long buyerId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getBuyerId, buyerId)
                        .orderByDesc(Order::getCreateTime)
        );
    }

    @Override
    public List<Order> findBySellerId(Long sellerId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getSellerId, sellerId)
                        .orderByDesc(Order::getCreateTime)
        );
    }

    @Override
    public PageResult<Order> findPage(QueryOrderRequest request) {
        Page<Order> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(StringUtils.isNotBlank(request.getOrderNo()), Order::getOrderNo, request.getOrderNo());
        wrapper.eq(request.getStatus() != null, Order::getStatus, request.getStatus());
        wrapper.eq(request.getBuyerId() != null, Order::getBuyerId, request.getBuyerId());
        wrapper.eq(request.getSellerId() != null, Order::getSellerId, request.getSellerId());
        wrapper.eq(request.getProductId() != null, Order::getProductId, request.getProductId());

        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = orderMapper.selectPage(page, wrapper);
        return PageResult.fromIPage(orderPage);
    }
}
