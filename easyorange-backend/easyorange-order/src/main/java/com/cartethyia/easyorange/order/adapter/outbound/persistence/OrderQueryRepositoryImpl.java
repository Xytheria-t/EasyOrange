package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.application.port.query.OrderQueryRepository;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class OrderQueryRepositoryImpl extends BaseRepository<OrderMapper, OrderDO> implements OrderQueryRepository {

    private final OrderDataMapper dataMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderQueryRepositoryImpl(
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
        var wrapper = lambdaQuery();

        wrapper.eq(StringUtils.isNotBlank(condition.orderNo()), OrderDO::getOrderNo, condition.orderNo());
        wrapper.eq(condition.status() != null, OrderDO::getStatus, condition.status());
        wrapper.eq(condition.buyerId() != null, OrderDO::getBuyerId, condition.buyerId());
        wrapper.eq(condition.sellerId() != null, OrderDO::getSellerId, condition.sellerId());

        wrapper.orderByDesc(OrderDO::getCreateTime);

        Page<OrderDO> orderPage = wrapper.page(page);

        return PageResult.of(
                orderPage.getRecords().stream().map(dataMapper::toReadModel).toList(),
                orderPage.getTotal(),
                (int) orderPage.getCurrent(),
                (int) orderPage.getSize());
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
