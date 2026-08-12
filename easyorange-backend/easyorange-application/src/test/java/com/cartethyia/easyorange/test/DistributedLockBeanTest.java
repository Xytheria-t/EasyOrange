package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * 分布式锁收敛守卫：order/payment/超时任务共用 framework 的单一锁端口，
 * 运行时必须恰好一个 {@link DistributedLockPort} Bean（防止重复实现回归）。
 */
class DistributedLockBeanTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void exactlyOneDistributedLockPortBean() {
        Map<String, DistributedLockPort> beans = applicationContext.getBeansOfType(DistributedLockPort.class);
        assertThat(beans).hasSize(1);
    }
}
