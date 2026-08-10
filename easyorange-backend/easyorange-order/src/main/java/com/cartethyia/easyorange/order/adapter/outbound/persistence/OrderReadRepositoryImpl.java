package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class OrderReadRepositoryImpl extends BaseRepository<OrderMapper, OrderDO> implements OrderReadRepository {

    private final OrderDataMapper dataMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderReadRepositoryImpl(
            OrderMapper orderMapper,
            @Qualifier("orderDataMapper") OrderDataMapper dataMapper,
            OrderItemMapper orderItemMapper) {
        super(orderMapper);
        this.dataMapper = dataMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public Optional<OrderReadModel> findById(OrderId id) {
        OrderDO orderDO = mapper.selectById(id.value());
        if (orderDO == null) {
            return Optional.empty();
        }
        List<OrderItemReadModel> items = findItemsByOrderId(id.value());
        return Optional.ofNullable(dataMapper.toReadModel(orderDO, items));
    }

    @Override
    public PageResult<OrderReadModel> findPage(OrderQueryCondition condition) {
        Page<OrderDO> page = new Page<>(condition.pageNum(), condition.pageSize());
        var wrapper = new LambdaQueryWrapper<OrderDO>();

        wrapper.eq(StringUtils.isNotBlank(condition.orderNo()), OrderDO::getOrderNo, condition.orderNo());
        wrapper.eq(condition.status() != null, OrderDO::getStatus, condition.status());
        wrapper.eq(condition.buyerId() != null, OrderDO::getBuyerId, condition.buyerId());
        wrapper.eq(condition.sellerId() != null, OrderDO::getSellerId, condition.sellerId());

        wrapper.orderByDesc(OrderDO::getCreateTime);

        Page<OrderDO> orderPage = mapper.selectPage(page, wrapper);

        List<String> orderIds =
                orderPage.getRecords().stream().map(OrderDO::getId).toList();
        Map<String, List<OrderItemReadModel>> itemsByOrderId = loadItemsByOrderId(orderIds);
        List<OrderReadModel> records = orderPage.getRecords().stream()
                .map(orderDO ->
                        dataMapper.toReadModel(orderDO, itemsByOrderId.getOrDefault(orderDO.getId(), List.of())))
                .toList();

        return PageResult.of(records, orderPage.getTotal(), (int) orderPage.getCurrent(), (int) orderPage.getSize());
    }

    /** 批加载订单明细（一次 IN 查询，避免分页 N+1），按 orderId 分组。 */
    private Map<String, List<OrderItemReadModel>> loadItemsByOrderId(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        return orderItemMapper
                .selectList(new LambdaQueryWrapper<OrderItemDO>().in(OrderItemDO::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(
                        OrderItemDO::getOrderId, Collectors.mapping(dataMapper::toItemReadModel, Collectors.toList())));
    }

    @Override
    public long countByStatus(OrderStatus status) {
        if (status == null) {
            return lambdaQuery().count();
        }
        return lambdaQuery().eq(OrderDO::getStatus, status).count();
    }

    @Override
    public List<OrderItemReadModel> findItemsByOrderId(String orderId) {
        return orderItemMapper
                .selectList(new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, orderId))
                .stream()
                .map(dataMapper::toItemReadModel)
                .toList();
    }
}
