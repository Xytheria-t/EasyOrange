package com.cartethyia.easyorange.order.domain.saga;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.entity.Order;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.product.domain.aggregate.ProductAggregate;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.application.handler.ProductQueryHandler;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单创建 Saga 编排器
 * 实现分布式事务：订单创建 → 扣减库存 → 创建支付
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderSaga {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentCommandHandler paymentCommandHandler;
    private final ProductQueryHandler productQueryHandler;
    private final DomainEventPublisher eventPublisher;

    /**
     * 执行订单创建 Saga
     * 
     * @param command 订单创建命令
     * @return 订单创建结果
     * @throws OrderCreationException 当任何步骤失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResult execute(CreateOrderCommand command) {
        List<CompensatingAction> compensations = new ArrayList<>();
        
        try {
            // Step 1: 创建订单
            OrderCreatedEvent orderEvent = createOrder(command);
            compensations.add(() -> cancelOrder(orderEvent.getOrderId()));
            
            // Step 2: 扣减库存
            StockDecreasedEvent stockEvent = decreaseStock(command.getProductId());
            compensations.add(() -> restoreStock(command.getProductId()));
            
            // Step 3: 创建支付
            CreatePaymentCommand paymentCommand = new CreatePaymentCommand(
                    orderEvent.getOrderId(),
                    orderEvent.getAmount(),
                    command.getPaymentMethod() != null ? command.getPaymentMethod() : 1,
                    "ORDER",
                    "订单支付"
            );
            paymentCommandHandler.handle(paymentCommand);
            
            String orderNo = "ORD" + orderEvent.getOrderId();
            log.info("订单创建 Saga 完成 orderId={} orderNo={}", orderEvent.getOrderId(), orderNo);
            
            return new CreateOrderResult(orderEvent.getOrderId(), orderNo);
            
        } catch (Exception e) {
            log.error("订单创建 Saga 失败，执行补偿逻辑 command={}", command, e);
            compensate(compensations, e);
            throw new OrderCreationException("订单创建失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * Step 1: 创建订单
     */
    private OrderCreatedEvent createOrder(CreateOrderCommand command) {
        Long buyerId = SecurityContextUtil.getCurrentUserIdOrThrow();
        
        // 查询商品信息
        ProductVO productVO = productQueryHandler.getProductById(command.getProductId());
        
        BizRequire.notNull(productVO, "商品不存在");
        BizRequire.isTrue(ProductStatus.ONLINE.getCode().equals(productVO.getStatus()), "商品已下架");
        BizRequire.ne(productVO.getSellerId(), buyerId, "不能购买自己的商品");
        BizRequire.isTrue(productVO.getStock() != null && productVO.getStock() > 0, "商品库存不足");
        
        // 创建订单聚合根
        OrderCreatedEvent event = OrderAggregate.createOrder(
                buyerId,
                productVO.getSellerId(),
                command.getProductId(),
                productVO.getPrice(),
                command.getAddress(),
                command.getPhone(),
                command.getRemark()
        );
        
        // 保存订单
        OrderAggregate aggregate = OrderAggregate.from(
                event.getOrderId(),
                "ORD" + event.getOrderId(),
                event.getBuyerId(),
                event.getSellerId(),
                event.getProductId(),
                event.getAmount(),
                OrderStatus.PENDING_PAYMENT.getCode(),
                0,
                command.getAddress(),
                command.getPhone(),
                command.getRemark()
        );
        
        Order order = aggregate.toEntity();
        orderRepository.save(order);
        
        // 发布订单创建事件
        eventPublisher.publish(event);
        log.info("订单创建成功 orderId={} orderNo={}", order.getId(), order.getOrderNo());
        
        return event;
    }
    
    /**
     * Step 2: 扣减库存
     */
    private StockDecreasedEvent decreaseStock(Long productId) {
        ProductAggregate product = ProductAggregate.load(
                productRepository.findById(productId),
                null,
                null
        );
        
        BizRequire.notNull(product.getProduct(), "商品不存在");
        
        StockDecreasedEvent event = product.decrementStock();
        productRepository.save(product.getProduct());
        
        log.info("库存扣减成功 productId={}", productId);
        
        return event;
    }
    
    /**
     * 补偿操作接口
     */
    @FunctionalInterface
    private interface CompensatingAction {
        void compensate();
    }
    
    /**
     * 执行补偿逻辑
     */
    private void compensate(List<CompensatingAction> compensations, Exception cause) {
        // 倒序执行补偿（后进先出）
        for (int i = compensations.size() - 1; i >= 0; i--) {
            try {
                compensations.get(i).compensate();
            } catch (Exception e) {
                log.error("补偿操作失败 index={}", i, e);
                // 补偿失败记录日志，但不中断后续补偿
            }
        }
    }
    
    /**
     * 补偿：取消订单
     */
    private void cancelOrder(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("订单不存在，无法补偿：" + orderId));
            
            if (OrderStatus.canCancel(order.getStatus())) {
                OrderAggregate aggregate = OrderAggregate.fromEntity(order);
                OrderAggregate cancelledAggregate = aggregate.withStatus(OrderStatus.CANCELLED.getCode());
                orderRepository.update(cancelledAggregate.toEntity());
                log.info("订单补偿成功 orderId={}", orderId);
            } else {
                log.warn("订单状态不允许取消补偿 orderId={} status={}", orderId, order.getStatus());
            }
        } catch (Exception e) {
            log.error("订单补偿失败 orderId={}", orderId, e);
            throw new RuntimeException("订单补偿失败", e);
        }
    }
    
    /**
     * 补偿：恢复库存
     */
    private void restoreStock(Long productId) {
        try {
            ProductAggregate product = ProductAggregate.load(
                    productRepository.findById(productId),
                    null,
                    null
            );
            
            if (product.getProduct() != null) {
                product.restoreStock();
                productRepository.save(product.getProduct());
                log.info("库存补偿成功 productId={}", productId);
            }
        } catch (Exception e) {
            log.error("库存补偿失败 productId={}", productId, e);
            throw new RuntimeException("库存补偿失败", e);
        }
    }
    
}
