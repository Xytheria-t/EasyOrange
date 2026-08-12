package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class OrderRepositoryImpl extends BaseRepository<OrderMapper, OrderDO> implements OrderRepository {

    private final OrderDataMapper dataMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderRepositoryImpl(
            OrderMapper orderMapper,
            @Qualifier("orderDataMapper") OrderDataMapper dataMapper,
            OrderItemMapper orderItemMapper) {
        super(orderMapper);
        this.dataMapper = dataMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public void save(Order aggregate) {
        mapper.insert(dataMapper.toDataObject(aggregate));
        batchInsertItems(aggregate.id().value(), aggregate.items());
    }

    @Override
    public void update(Order aggregate) {
        mapper.updateById(dataMapper.toDataObject(aggregate));
        // 订单项为不可变快照，整组物理替换；逻辑删除会让每次状态流转累积 del_flag=1 脏行。
        orderItemMapper.deleteByOrderId(aggregate.id().value());
        batchInsertItems(aggregate.id().value(), aggregate.items());
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        OrderDO orderDO = mapper.selectById(id.value());
        if (orderDO == null) {
            return Optional.empty();
        }
        List<OrderItem> items = findItemsByOrderId(id.value());
        return Optional.ofNullable(dataMapper.toAggregate(orderDO, items));
    }

    @Override
    public List<Order> findExpiredOrders(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return lambdaQuery()
                .eq(OrderDO::getStatus, OrderStatus.PENDING_PAYMENT)
                .lt(OrderDO::getCreateTime, threshold)
                .list()
                .stream()
                .map(dataMapper::toAggregate)
                .toList();
    }

    @Override
    public List<Order> findShippedOrdersBefore(LocalDateTime threshold) {
        return lambdaQuery()
                .eq(OrderDO::getStatus, OrderStatus.SHIPPED)
                .lt(OrderDO::getUpdateTime, threshold)
                .list()
                .stream()
                .map(dataMapper::toAggregate)
                .toList();
    }

    @Override
    public List<OrderItem> findItemsByOrderId(String orderId) {
        return orderItemMapper
                .selectList(new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, orderId))
                .stream()
                .map(dataMapper::toOrderItem)
                .toList();
    }

    private void batchInsertItems(String orderId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        orderItemMapper.batchInsert(
                items.stream().map(item -> dataMapper.toItemDO(orderId, item)).toList());
    }
}
