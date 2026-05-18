package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.order.domain.port.output.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.port.output.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisOrderReadRepository extends BaseRepository<OrderMapper, OrderDO> implements OrderReadRepository {

    private final OrderDataConverter converter;

    public MybatisOrderReadRepository(OrderMapper orderMapper, OrderDataConverter converter) {
        super(orderMapper);
        this.converter = converter;
    }

    @Override
    public Optional<OrderReadModel> findById(OrderId id) {
        OrderDO orderDO = mapper.selectById(id.value());
        return Optional.ofNullable(converter.toReadModel(orderDO));
    }

    @Override
    public PageResult<OrderReadModel> findPage(OrderQueryCondition condition) {
        Page<OrderDO> page = new Page<>(condition.pageNum(), condition.pageSize());
        var wrapper = lambdaQuery();

        wrapper.eq(StringUtils.isNotBlank(condition.orderNo()), OrderDO::getOrderNo, condition.orderNo());
        wrapper.eq(condition.status() != null, OrderDO::getStatus, condition.status());
        wrapper.eq(condition.buyerId() != null, OrderDO::getBuyerId, condition.buyerId());
        wrapper.eq(condition.sellerId() != null, OrderDO::getSellerId, condition.sellerId());
        wrapper.eq(condition.productId() != null, OrderDO::getProductId, condition.productId());

        wrapper.orderByDesc(OrderDO::getCreateTime);

        Page<OrderDO> orderPage = wrapper.page(page);

        return PageResult.of(
                orderPage.getRecords().stream().map(converter::toReadModel).toList(),
                orderPage.getTotal(), (int) orderPage.getCurrent(), (int) orderPage.getSize()
        );
    }

    @Override
    public long countByStatus(Integer status) {
        return lambdaQuery()
                .eq(OrderDO::getStatus, status)
                .count();
    }
}