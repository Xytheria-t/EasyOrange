package com.cartethyia.easyorange.user.domain.port;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

/**
 * 用户领域事件端口 - 发布用户相关领域事件
 * <p>
 * 职责：
 * <ul>
 *   <li>发布用户注册事件</li>
 *   <li>发布密码修改事件</li>
 *   <li>发布忘记密码事件</li>
 * </ul>
 * 
 * <p>实现类通常委托给通用的 {@link com.cartethyia.easyorange.common.event.DomainEventPublisher}
 */
public interface UserEventPort extends OutboundPort {

    /**
     * 发布领域事件
     * 
     * @param event 领域事件
     */
    void publish(BaseDomainEvent event);
}
