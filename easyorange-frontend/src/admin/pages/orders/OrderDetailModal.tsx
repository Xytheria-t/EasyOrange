import { AdminDetailModal, InfoCell } from '@/admin/components/AdminDetailModal';
import { statusVisual } from '@/admin/components/StatusBadge';
import { useAdminOrderDetail } from '../../hooks';
import type { AdminOrderDetail } from '../../types/admin';

export interface OrderDetailModalProps {
    open: boolean;
    orderId: string | null;
    onClose: () => void;
}

const PAYMENT_STATUS: Record<string, string> = {
    UNPAID: '未支付',
    PAID: '已支付',
    REFUNDED: '已退款',
};

function formatDateTime(dateString: string | null) {
    if (!dateString) {
        return '—';
    }
    return new Date(dateString).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    });
}

export function OrderDetailModal({ open, orderId, onClose }: OrderDetailModalProps) {
    const { data: order, isLoading, isError, error, refetch } = useAdminOrderDetail(orderId ?? '');

    if (!open || !orderId) {
        return null;
    }

    const orderData = order as AdminOrderDetail | undefined;
    const statusCfg = statusVisual('order', orderData?.status);
    const isRefunded = orderData?.status === 'REFUNDED';
    // 只有真正取消 / 退款的订单才有这两个字段；此前对所有订单都读 cancelTime，
    // 未取消的订单会显示一个空的「取消时间」，让人以为订单被关过
    const isCancelled = orderData?.status === 'CANCELLED';
    const closeReason =
        orderData && (isRefunded || isCancelled)
            ? isRefunded
                ? orderData.refundReason
                : orderData.cancelReason
            : null;
    const closeTime =
        orderData && (isRefunded || isCancelled)
            ? formatDateTime(isRefunded ? orderData.refundTime : orderData.cancelTime)
            : null;

    return (
        <AdminDetailModal
            open={open}
            onClose={onClose}
            title="订单详情"
            maxWidth={480}
            loading={isLoading}
            error={isError ? error : null}
            onRetry={() => {
                refetch();
            }}
            notFound={!orderData}
            notFoundText="订单不存在或已被删除"
            icon={
                <svg
                    aria-hidden="true"
                    width="13"
                    height="13"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                >
                    <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="16" y1="13" x2="8" y2="13" />
                    <line x1="16" y1="17" x2="8" y2="17" />
                </svg>
            }
        >
            {orderData ? (
                <div className="flex flex-col gap-5">
                    {/* Status & amount bar */}
                    <div className="admin-order-bar">
                        <div
                            className="admin-order-status"
                            style={{ background: statusCfg.bg, color: statusCfg.color }}
                        >
                            {statusCfg.label}
                        </div>
                        <div className="min-w-0 flex-1">
                            <span className="admin-order-amount">¥{orderData.totalAmount.toFixed(2)}</span>
                        </div>
                        <span className="admin-order-no">{orderData.orderNo}</span>
                    </div>

                    {/* Product info */}
                    {orderData.items?.length === 1 ? (
                        (() => {
                            const item = orderData.items[0];
                            return (
                                <div className="admin-item-row">
                                    {item.productImage ? (
                                        <img
                                            src={item.productImage}
                                            alt=""
                                            className="admin-item-thumb"
                                            loading="lazy"
                                            decoding="async"
                                        />
                                    ) : (
                                        <div className="admin-item-thumb admin-item-thumb--empty">
                                            <svg
                                                aria-hidden="true"
                                                width="22"
                                                height="22"
                                                fill="none"
                                                viewBox="0 0 24 24"
                                                stroke="currentColor"
                                            >
                                                <path
                                                    strokeLinecap="round"
                                                    strokeLinejoin="round"
                                                    strokeWidth={1.5}
                                                    d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                                                />
                                            </svg>
                                        </div>
                                    )}
                                    <div className="flex min-w-0 flex-1 flex-col justify-center">
                                        <p className="admin-item-name">{item.productName || '—'}</p>
                                        <p className="admin-item-meta">单价: ¥{item.unitPrice.toFixed(2)}</p>
                                        <p className="admin-item-meta">数量: {item.quantity}</p>
                                    </div>
                                </div>
                            );
                        })()
                    ) : !orderData.items?.length ? (
                        // 此前空数组会走进 map 分支渲染出空白区，看不出是「无商品」还是「没加载出来」
                        <div className="admin-empty-hint">该订单没有商品明细</div>
                    ) : (
                        <div className="flex flex-col gap-2">
                            {orderData.items?.map(item => (
                                <div key={item.itemId} className="admin-item-row">
                                    {item.productImage ? (
                                        <img
                                            src={item.productImage}
                                            alt=""
                                            className="admin-item-thumb"
                                            loading="lazy"
                                            decoding="async"
                                        />
                                    ) : (
                                        <div className="admin-item-thumb admin-item-thumb--empty">
                                            <svg
                                                aria-hidden="true"
                                                width="22"
                                                height="22"
                                                fill="none"
                                                viewBox="0 0 24 24"
                                                stroke="currentColor"
                                            >
                                                <path
                                                    strokeLinecap="round"
                                                    strokeLinejoin="round"
                                                    strokeWidth={1.5}
                                                    d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                                                />
                                            </svg>
                                        </div>
                                    )}
                                    <div className="flex min-w-0 flex-1 flex-col justify-center">
                                        <p className="admin-item-name">{item.productName || '—'}</p>
                                        <p className="admin-item-meta">
                                            单价: ¥{item.unitPrice.toFixed(2)} × {item.quantity}
                                        </p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Info grid - row 1 */}
                    <div className="admin-field-grid">
                        <InfoCell label="认领方" value={orderData.buyer?.nickname || '—'} />
                        <InfoCell label="资产方" value={orderData.seller?.nickname || '—'} />
                        <InfoCell label="支付状态" value={PAYMENT_STATUS[orderData.paymentStatus] ?? '未知'} />
                        <InfoCell
                            label="支付金额"
                            value={orderData.paidAmount != null ? `¥${orderData.paidAmount.toFixed(2)}` : '—'}
                        />
                    </div>

                    {/* 时间信息：取消 / 退款时间只在对应状态下出现 */}
                    <div className="admin-field-grid">
                        <InfoCell label="下单时间" value={formatDateTime(orderData.createTime)} />
                        <InfoCell label="支付时间" value={formatDateTime(orderData.payTime)} />
                        <InfoCell label="更新时间" value={formatDateTime(orderData.updateTime)} />
                        {isRefunded || isCancelled ? (
                            <InfoCell label={isRefunded ? '退款时间' : '取消时间'} value={closeTime} />
                        ) : null}
                    </div>

                    {/* Payment No */}
                    {(orderData.paymentNo || orderData.refundedAmount) && (
                        <div className="grid grid-cols-2 gap-3">
                            {orderData.paymentNo && (
                                <InfoCell
                                    label="支付单号"
                                    value={<span className="font-mono text-[0.8rem]">{orderData.paymentNo}</span>}
                                />
                            )}
                            {orderData.refundedAmount != null && (
                                <InfoCell label="退款金额" value={`¥${orderData.refundedAmount.toFixed(2)}`} />
                            )}
                        </div>
                    )}

                    {/* Shipping address */}
                    {orderData.shippingAddress && (
                        <div className="admin-address-panel">
                            <p className="admin-note-title mb-[0.45rem]">
                                <svg
                                    aria-hidden="true"
                                    width="14"
                                    height="14"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    className="admin-stat-icon"
                                    strokeWidth="2"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                                    />
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                                    />
                                </svg>
                                收货地址
                            </p>
                            <p className="admin-address-body">
                                {orderData.shippingAddress.receiverName} {orderData.shippingAddress.phone}
                                <br />
                                {orderData.shippingAddress.detailAddress}
                            </p>
                        </div>
                    )}

                    {/* Remark / Cancel reason */}
                    {orderData.remark && (
                        <div className="admin-remark-panel">
                            <p className="admin-remark-title">备注</p>
                            <p className="admin-danger-body">{orderData.remark}</p>
                        </div>
                    )}
                    {closeReason && (
                        <div className="admin-danger-panel">
                            <p className="admin-danger-title">{isRefunded ? '退款原因' : '取消原因'}</p>
                            <p className="admin-danger-body">{closeReason}</p>
                        </div>
                    )}
                </div>
            ) : null}
        </AdminDetailModal>
    );
}
