import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { ConfirmModal } from '../../components/ConfirmModal';
import { useAdminCancelOrder, useAdminRefundOrder, useForceCompleteOrder } from '../../hooks';
import { notify } from '../../notify';
import type { AdminOrderDetail } from '../../types/admin';

/**
 * 订单干预入口 —— 取消 / 强制完成 / 退款。
 *
 * <p>此前三个 mutation（useAdminCancelOrder / useForceCompleteOrder / useAdminRefundOrder）与
 * 后端三个 PUT 端点都已就绪，但详情弹窗是纯只读的，管理端没有任何入口能触发它们。
 *
 * <p>可用动作直接对齐后端 {@code OrderAction} 的前置状态集与支付守卫：
 * 取消限待付款、强制取消覆盖待付款与已付款、退款限已付款与已发货且支付状态为已付。
 * 前端据此隐藏入口，后端状态机仍是唯一裁决方 —— 两边判据同源，改状态机时不会漏改 UI。
 */

type ActionKey = 'cancel' | 'forceComplete' | 'refund';

const ACTION_CONFIG: Record<ActionKey, { label: string; dialogTitle: string; reasonHint: string }> = {
    cancel: {
        label: '取消订单',
        dialogTitle: '取消订单',
        reasonHint: '取消原因会同步给买卖双方',
    },
    forceComplete: {
        label: '强制完成',
        dialogTitle: '强制完成订单',
        reasonHint: '买家已线下收货但未确认时使用，原因会记入订单',
    },
    refund: {
        label: '退款',
        dialogTitle: '退款',
        reasonHint: '退款会同步触发支付侧退款，原因会展示给买家',
    },
};

function availableActions(order: AdminOrderDetail): ActionKey[] {
    const actions: ActionKey[] = [];
    if (order.status === 'PENDING_PAYMENT') {
        actions.push('cancel');
    }
    if (order.status === 'PENDING_PAYMENT' || order.status === 'PAID') {
        actions.push('forceComplete');
    }
    if ((order.status === 'PAID' || order.status === 'SHIPPED') && order.paymentStatus === 'PAID') {
        actions.push('refund');
    }
    return actions;
}

export function OrderInterventionBar({ order }: { order: AdminOrderDetail }) {
    const [pendingAction, setPendingAction] = useState<ActionKey | null>(null);
    const [reason, setReason] = useState('');

    const cancelOrder = useAdminCancelOrder();
    const forceComplete = useForceCompleteOrder();
    const refundOrder = useAdminRefundOrder();

    const actions = availableActions(order);
    const isPending = cancelOrder.isPending || forceComplete.isPending || refundOrder.isPending;

    if (actions.length === 0) {
        return null;
    }

    const mutationOf = (action: ActionKey) => {
        if (action === 'cancel') {
            return cancelOrder;
        }
        if (action === 'forceComplete') {
            return forceComplete;
        }
        return refundOrder;
    };

    const closeDialog = () => {
        setPendingAction(null);
        setReason('');
    };

    const handleConfirm = async () => {
        if (!pendingAction) {
            return;
        }
        const mutation = mutationOf(pendingAction);
        const trimmed = reason.trim();

        if (!trimmed) {
            notify.error('请填写原因');
            return;
        }

        try {
            await mutation.mutateAsync({ id: order.orderId, data: { reason: trimmed } });
            notify.success(`${ACTION_CONFIG[pendingAction].label}成功`);
            closeDialog();
        } catch (e) {
            notify.failure(e, `${ACTION_CONFIG[pendingAction].label}失败，请稍后重试`);
        }
    };

    return (
        <>
            <div className="admin-footer-actions admin-footer-bar justify-end">
                {actions.map(action => (
                    <Button
                        key={action}
                        variant={action === 'forceComplete' ? 'outline' : 'destructive'}
                        disabled={isPending}
                        onClick={() => {
                            setPendingAction(action);
                            setReason('');
                        }}
                    >
                        {ACTION_CONFIG[action].label}
                    </Button>
                ))}
            </div>
            <ConfirmModal
                isOpen={pendingAction !== null}
                title={pendingAction ? ACTION_CONFIG[pendingAction].dialogTitle : ''}
                variant="danger"
                confirmText="确认"
                isLoading={isPending}
                content={
                    // ConfirmModal 把 content 渲染进 DialogDescription（<p>），只能放 phrasing content
                    <span className="block">
                        <span className="admin-note-body mb-2 block">
                            {pendingAction ? ACTION_CONFIG[pendingAction].reasonHint : ''}
                        </span>
                        <Textarea
                            rows={3}
                            value={reason}
                            onChange={e => setReason(e.target.value)}
                            placeholder="请填写原因"
                            className="admin-textarea focus-visible:ring-0 focus-visible:ring-offset-0"
                        />
                    </span>
                }
                onConfirm={handleConfirm}
                onCancel={closeDialog}
            />
        </>
    );
}
