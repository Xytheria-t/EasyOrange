package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.common.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class OrderRepositoryImpl extends BaseRepository<OrderMapper, OrderDO> implements OrderRepository {

    /** 定时任务单次扫描上限 — 分批处理防止一次加载全表，剩余批次由下一轮 cron 继续。 */
    private static final int SCAN_BATCH_LIMIT = 500;

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
        // 订单项是创建后不可变的快照，仅 save() 写入；状态流转只更新订单主表，
        // 不再整组删除重插（原实现会在聚合根缺行项时把订单项物理清空）。
        // version 参与 WHERE 条件（@Version 乐观锁），0 行命中说明并发冲突，整体回滚避免发布过期事件。
        if (mapper.updateById(dataMapper.toDataObject(aggregate)) == 0) {
            throw new ConcurrentUpdateException("订单更新冲突: id=" + aggregate.id().value());
        }
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
        List<OrderDO> orderDOs = lambdaQuery()
                .eq(OrderDO::getStatus, OrderStatus.PENDING_PAYMENT)
                .lt(OrderDO::getCreateTime, threshold)
                .orderByAsc(OrderDO::getCreateTime)
                .last("LIMIT " + SCAN_BATCH_LIMIT)
                .list();
        return toAggregatesWithItems(orderDOs);
    }

    @Override
    public List<Order> findShippedOrdersBefore(LocalDateTime threshold) {
        List<OrderDO> orderDOs = lambdaQuery()
                .eq(OrderDO::getStatus, OrderStatus.SHIPPED)
                .lt(OrderDO::getUpdateTime, threshold)
                .orderByAsc(OrderDO::getCreateTime)
                .last("LIMIT " + SCAN_BATCH_LIMIT)
                .list();
        return toAggregatesWithItems(orderDOs);
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

    /**
     * 批量重建聚合根并附带行项 — 定时任务产生的领域事件依赖行项提取 productIds，
     * 缺行项会导致库存恢复/售出标记等副作用静默失效。
     */
    private List<Order> toAggregatesWithItems(List<OrderDO> orderDOs) {
        if (orderDOs.isEmpty()) {
            return List.of();
        }
        List<String> orderIds = orderDOs.stream().map(OrderDO::getId).toList();
        Map<String, List<OrderItem>> itemsByOrderId =
                orderItemMapper
                        .selectList(new LambdaQueryWrapper<OrderItemDO>().in(OrderItemDO::getOrderId, orderIds))
                        .stream()
                        .collect(Collectors.groupingBy(
                                OrderItemDO::getOrderId,
                                Collectors.mapping(dataMapper::toOrderItem, Collectors.toList())));
        return orderDOs.stream()
                .map(orderDO ->
                        dataMapper.toAggregate(orderDO, itemsByOrderId.getOrDefault(orderDO.getId(), List.of())))
                .toList();
    }
}
