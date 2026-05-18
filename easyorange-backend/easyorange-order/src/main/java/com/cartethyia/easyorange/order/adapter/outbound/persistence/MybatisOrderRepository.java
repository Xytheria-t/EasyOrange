package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.output.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisOrderRepository extends BaseRepository<OrderMapper, OrderDO> implements OrderRepository {

    private final OrderDataConverter converter;

    public MybatisOrderRepository(OrderMapper orderMapper, OrderDataConverter converter) {
        super(orderMapper);
        this.converter = converter;
    }

    @Override
    public void save(OrderAggregate aggregate) {
        mapper.insert(converter.toDataObject(aggregate));
    }

    @Override
    public void update(OrderAggregate aggregate) {
        mapper.updateById(converter.toDataObject(aggregate));
    }

    @Override
    public Optional<OrderAggregate> findById(OrderId id) {
        OrderDO orderDO = mapper.selectById(id.value());
        return Optional.ofNullable(converter.toAggregate(orderDO));
    }

    @Override
    public List<OrderAggregate> findByBuyerId(UserId buyerId) {
        return lambdaQuery()
                .eq(OrderDO::getBuyerId, buyerId.value())
                .orderByDesc(OrderDO::getCreateTime)
                .list()
                .stream().map(converter::toAggregate).toList();
    }

    @Override
    public List<OrderAggregate> findBySellerId(UserId sellerId) {
        return lambdaQuery()
                .eq(OrderDO::getSellerId, sellerId.value())
                .orderByDesc(OrderDO::getCreateTime)
                .list()
                .stream().map(converter::toAggregate).toList();
    }

    @Override
    public List<OrderAggregate> findExpiredOrders(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return lambdaQuery()
                .eq(OrderDO::getStatus, OrderStatus.PENDING_PAYMENT.getCode())
                .lt(OrderDO::getCreateTime, threshold)
                .list()
                .stream().map(converter::toAggregate).toList();
    }

    @Override
    public List<OrderAggregate> findByStatus(Integer status) {
        return lambdaQuery()
                .eq(OrderDO::getStatus, status)
                .orderByDesc(OrderDO::getCreateTime)
                .list()
                .stream().map(converter::toAggregate).toList();
    }

    @Override
    public List<OrderAggregate> findShippedOrdersBefore(LocalDateTime threshold) {
        return lambdaQuery()
                .eq(OrderDO::getStatus, OrderStatus.SHIPPED.getCode())
                .lt(OrderDO::getUpdateTime, threshold)
                .list()
                .stream().map(converter::toAggregate).toList();
    }
}