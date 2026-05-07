package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.output.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisOrderRepository implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderDataConverter converter;

    @Override
    public void save(OrderAggregate aggregate) {
        orderMapper.insert(converter.toDataObject(aggregate));
    }

    @Override
    public void update(OrderAggregate aggregate) {
        orderMapper.updateById(converter.toDataObject(aggregate));
    }

    @Override
    public Optional<OrderAggregate> findById(OrderId id) {
        OrderDO orderDO = orderMapper.selectById(id.value());
        return Optional.ofNullable(converter.toAggregate(orderDO));
    }

    @Override
    public List<OrderAggregate> findByBuyerId(UserId buyerId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<OrderDO>()
                        .eq(OrderDO::getBuyerId, buyerId.value())
                        .orderByDesc(OrderDO::getCreateTime)
        ).stream().map(converter::toAggregate).toList();
    }

    @Override
    public List<OrderAggregate> findBySellerId(UserId sellerId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<OrderDO>()
                        .eq(OrderDO::getSellerId, sellerId.value())
                        .orderByDesc(OrderDO::getCreateTime)
        ).stream().map(converter::toAggregate).toList();
    }

    @Override
    public List<OrderAggregate> findExpiredOrders(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return orderMapper.selectList(
                new LambdaQueryWrapper<OrderDO>()
                        .eq(OrderDO::getStatus, OrderStatus.PENDING_PAYMENT.getCode())
                        .lt(OrderDO::getCreateTime, threshold)
        ).stream().map(converter::toAggregate).toList();
    }

    @Override
    public List<OrderAggregate> findByStatus(Integer status) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<OrderDO>()
                        .eq(OrderDO::getStatus, status)
                        .orderByDesc(OrderDO::getCreateTime)
        ).stream().map(converter::toAggregate).toList();
    }

    @Override
    public List<OrderAggregate> findShippedOrdersBefore(LocalDateTime threshold) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<OrderDO>()
                        .eq(OrderDO::getStatus, OrderStatus.SHIPPED.getCode())
                        .lt(OrderDO::getUpdateTime, threshold)
        ).stream().map(converter::toAggregate).toList();
    }
}
