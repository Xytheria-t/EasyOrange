package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.domain.port.output.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.port.output.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisOrderReadRepository implements OrderReadRepository {

    private final OrderMapper orderMapper;
    private final OrderDataConverter converter;

    @Override
    public Optional<OrderReadModel> findById(OrderId id) {
        OrderDO orderDO = orderMapper.selectById(id.value());
        return Optional.ofNullable(converter.toReadModel(orderDO));
    }

    @Override
    public PageResult<OrderReadModel> findPage(OrderQueryCondition condition) {
        Page<OrderDO> page = new Page<>(condition.pageNum(), condition.pageSize());
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(StringUtils.isNotBlank(condition.orderNo()), OrderDO::getOrderNo, condition.orderNo());
        wrapper.eq(condition.status() != null, OrderDO::getStatus, condition.status());
        wrapper.eq(condition.buyerId() != null, OrderDO::getBuyerId, condition.buyerId());
        wrapper.eq(condition.sellerId() != null, OrderDO::getSellerId, condition.sellerId());
        wrapper.eq(condition.productId() != null, OrderDO::getProductId, condition.productId());

        wrapper.orderByDesc(OrderDO::getCreateTime);

        Page<OrderDO> orderPage = orderMapper.selectPage(page, wrapper);

        return PageResult.of(
                orderPage.getRecords().stream().map(converter::toReadModel).toList(),
                orderPage.getTotal(), (int) orderPage.getCurrent(), (int) orderPage.getSize()
        );
    }

    @Override
    public long countByStatus(Integer status) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDO::getStatus, status);
        return orderMapper.selectCount(wrapper);
    }
}
