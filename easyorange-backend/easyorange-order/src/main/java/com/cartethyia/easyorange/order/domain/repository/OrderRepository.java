package com.cartethyia.easyorange.order.domain.repository;

import com.cartethyia.easyorange.order.entity.Order;

import java.util.List;
import java.util.Optional;

/**
 * 订单仓储接口
 */
public interface OrderRepository {

    /**
     * 保存订单
     */
    void save(Order order);

    /**
     * 更新订单
     */
    void update(Order order);

    /**
     * 根据ID查询订单
     */
    Optional<Order> findById(Long id);

    /**
     * 根据买家ID查询订单
     */
    List<Order> findByBuyerId(Long buyerId);

    /**
     * 根据卖家ID查询订单
     */
    List<Order> findBySellerId(Long sellerId);

    /**
     * 查询超时未付款的订单
     */
    List<Order> findExpiredOrders(int timeoutMinutes);

    /**
     * 根据状态查询订单
     */
    List<Order> findByStatus(Integer status);
}
