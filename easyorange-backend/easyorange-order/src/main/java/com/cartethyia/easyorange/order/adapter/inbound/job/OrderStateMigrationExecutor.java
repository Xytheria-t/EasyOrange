package com.cartethyia.easyorange.order.adapter.inbound.job;

import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 订单定时状态迁移执行器 — 「扫描候选 → 非阻塞分布式锁 → 事务内迁移 → 统计」通用管线。
 * <p>
 * 超时取消与自动确认收货两个定时任务共用同一执行骨架，只提供查询与迁移动作：
 * waitTimeout=0 非阻塞获取锁（拿不到即跳过，不阻塞扫描）；迁移在事务内执行，
 * 状态更新 + 领域事件（Outbox）原子提交，避免「更新已提交、事件未落 Outbox」的崩溃窗口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStateMigrationExecutor {

    private final DistributedLockPort lockPort;
    private final TransactionTemplate transactionTemplate;

    /**
     * 对候选订单逐个执行状态迁移，锁争用/异常仅跳过当前订单，不影响其余。
     *
     * @param jobName     任务名（日志）
     * @param lockPrefix  订单级锁前缀
     * @param candidates  扫描出的候选订单
     * @param action      状态迁移动作（事务内执行）
     * @return 成功迁移数
     */
    public int execute(String jobName, String lockPrefix, List<Order> candidates, MigrationAction action) {
        if (candidates.isEmpty()) {
            return 0;
        }
        int migrated = 0;
        for (Order aggregate : candidates) {
            String lockKey = lockPrefix + aggregate.id().value();
            try {
                // watchdog 覆盖单次迁移的全部时长
                boolean done = lockPort.executeWithLocks(
                        List.of(lockKey), 0L, () -> transactionTemplate.execute(status -> action.migrate(aggregate)));
                if (done) {
                    migrated++;
                }
            } catch (LockAcquisitionException e) {
                log.warn("{}获取锁失败/被中断，跳过 orderId={}", jobName, aggregate.id().value());
            } catch (Exception e) {
                log.error("{}失败: orderId={}", jobName, aggregate.id().value(), e);
            }
        }
        log.info("{}检查完成: 检查 {} 条, 迁移 {} 条", jobName, candidates.size(), migrated);
        return migrated;
    }

    @FunctionalInterface
    public interface MigrationAction {

        boolean migrate(Order aggregate);
    }
}
