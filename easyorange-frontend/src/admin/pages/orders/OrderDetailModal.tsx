import { AdminDetailModal, InfoCell } from '@/admin/components/AdminDetailModal';
import { useAdminOrderDetail } from '../../hooks';
import type { AdminOrderDetail } from '../../types/admin';

export interface OrderDetailModalProps {
    open: boolean;
    orderId: string | null;
    onClose: () => void;
}

const STATUS_CONFIG: Record<number, { bg: string; color: string; dot: string; label: string }> = {
    0: {
        bg: 'linear-gradient(135deg, rgba(249,158,11,0.10), rgba(251,191,36,0.06))',
        color: '#D97706',
        dot: '#F59E0B',
        label: '待付款',
    },
    1: {
        bg: 'linear-gradient(135deg, rgba(59,130,246,0.10), rgba(96,165,250,0.06))',
        color: '#2563EB',
        dot: '#3B82F6',
        label: '待发货',
    },
    2: {
        bg: 'linear-gradient(135deg, rgba(139,92,246,0.10), rgba(167,139,250,0.06))',
        color: '#7C3AED',
        dot: '#8B5CF6',
        label: '已发货',
    },
    3: {
        bg: 'linear-gradient(135deg, rgba(16,185,129,0.10), rgba(52,211,153,0.06))',
        color: '#059669',
        dot: '#10B981',
        label: '已完成',
    },
    4: {
        bg: 'linear-gradient(135deg, rgba(156,163,175,0.10), rgba(209,213,219,0.06))',
        color: '#6B7280',
        dot: '#9CA3AF',
        label: '已取消',
    },
    5: {
        bg: 'linear-gradient(135deg, rgba(244,63,94,0.10), rgba(251,113,133,0.06))',
        color: '#E11D48',
        dot: '#F43F5E',
        label: '退款中',
    },
};

const PAYMENT_STATUS: Record<number, string> = {
    0: '未支付',
    1: '已支付',
    2: '已退款',
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
    const { data: order, isLoading } = useAdminOrderDetail(orderId ?? '');

    if (!open || !orderId) {
        return null;
    }

    const orderData = order as AdminOrderDetail | undefined;
    const statusCfg = STATUS_CONFIG[orderData?.status ?? 0] ?? STATUS_CONFIG[0];

    return (
        <AdminDetailModal
            open={open}
            onClose={onClose}
            title="订单详情"
            maxWidth={480}
            loading={isLoading}
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
                    <div className="flex items-center gap-4 rounded-2xl border border-[rgba(251,191,36,0.08)] bg-[linear-gradient(135deg,rgba(251,191,36,0.05),rgba(249,115,22,0.03))] p-4">
                        <div
                            className="shrink-0 whitespace-nowrap rounded-[10px] px-3 py-[0.35rem] text-[0.82rem] font-bold"
                            style={{ background: statusCfg.bg, color: statusCfg.color }}
                        >
                            {statusCfg.label}
                        </div>
                        <div className="min-w-0 flex-1">
                            <span
                                className="text-[1.35rem] font-bold text-[#EA580C]"
                                style={{ fontFamily: "'DM Sans', sans-serif" }}
                            >
                                ¥{orderData.totalAmount.toFixed(2)}
                            </span>
                        </div>
                        <span
                            className="shrink-0 text-[0.78rem] font-semibold tracking-[0.03em] text-[#9B9590]"
                            style={{ fontFamily: "'DM Sans', monospace" }}
                        >
                            {orderData.orderNo}
                        </span>
                    </div>

                    {/* Product info */}
                    {orderData.items?.length === 1 ? (
                        (() => {
                            const item = orderData.items[0];
                            return (
                                <div className="flex gap-[0.85rem] rounded-[14px] border border-[rgba(229,224,219,0.4)] bg-white/60 p-[0.85rem]">
                                    {item.productImage ? (
                                        <img
                                            src={item.productImage}
                                            alt=""
                                            className="h-[60px] w-[60px] shrink-0 rounded-xl object-cover"
                                        />
                                    ) : (
                                        <div className="flex h-[60px] w-[60px] shrink-0 items-center justify-center rounded-xl bg-[linear-gradient(135deg,#F5F2EE,#EDE8E3)] text-[#B5AEA8]">
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
                                        <p className="overflow-hidden text-ellipsis whitespace-nowrap text-[0.93rem] font-bold leading-tight text-[#2A2520]">
                                            {item.productName || '—'}
                                        </p>
                                        <p className="mt-0.5 text-[0.81rem] text-[#9B9590]">
                                            单价: ¥{item.unitPrice.toFixed(2)}
                                        </p>
                                        <p className="mt-0.5 text-[0.81rem] text-[#9B9590]">数量: {item.quantity}</p>
                                    </div>
                                </div>
                            );
                        })()
                    ) : (
                        <div className="flex flex-col gap-2">
                            {orderData.items?.map(item => (
                                <div
                                    key={item.itemId}
                                    className="flex gap-[0.85rem] rounded-[14px] border border-[rgba(229,224,219,0.4)] bg-white/60 p-[0.85rem]"
                                >
                                    {item.productImage ? (
                                        <img
                                            src={item.productImage}
                                            alt=""
                                            className="h-[60px] w-[60px] shrink-0 rounded-xl object-cover"
                                        />
                                    ) : (
                                        <div className="flex h-[60px] w-[60px] shrink-0 items-center justify-center rounded-xl bg-[linear-gradient(135deg,#F5F2EE,#EDE8E3)] text-[#B5AEA8]">
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
                                        <p className="overflow-hidden text-ellipsis whitespace-nowrap text-[0.93rem] font-bold leading-tight text-[#2A2520]">
                                            {item.productName || '—'}
                                        </p>
                                        <p className="mt-0.5 text-[0.81rem] text-[#9B9590]">
                                            单价: ¥{item.unitPrice.toFixed(2)} × {item.quantity}
                                        </p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Info grid - row 1 */}
                    <div className="grid grid-cols-2 gap-3">
                        <InfoCell label="认领方" value={orderData.buyer?.nickname || '—'} />
                        <InfoCell label="资产方" value={orderData.seller?.nickname || '—'} />
                        <InfoCell label="支付状态" value={PAYMENT_STATUS[orderData.paymentStatus] ?? '未知'} />
                        <InfoCell
                            label="支付金额"
                            value={orderData.paidAmount != null ? `¥${orderData.paidAmount.toFixed(2)}` : '—'}
                        />
                    </div>

                    {/* Time info */}
                    <div className="grid grid-cols-2 gap-3">
                        <InfoCell label="下单时间" value={formatDateTime(orderData.createTime)} />
                        <InfoCell label="支付时间" value={formatDateTime(orderData.payTime)} />
                        <InfoCell label="更新时间" value={formatDateTime(orderData.updateTime)} />
                        <InfoCell label="取消时间" value={formatDateTime(orderData.cancelTime)} />
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
                        <div className="rounded-[14px] border border-[rgba(229,224,219,0.35)] bg-[linear-gradient(135deg,rgba(251,191,36,0.03),rgba(195,155,211,0.02))] px-4 py-[0.85rem]">
                            <p className="mb-[0.45rem] flex items-center gap-[0.35rem] text-[0.78rem] font-semibold text-[#6B6460]">
                                <svg
                                    aria-hidden="true"
                                    width="14"
                                    height="14"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    stroke="#F97316"
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
                            <p className="text-[0.87rem] leading-relaxed text-[#2A2520]">
                                {orderData.shippingAddress.receiverName} {orderData.shippingAddress.phone}
                                <br />
                                {orderData.shippingAddress.detailAddress}
                            </p>
                        </div>
                    )}

                    {/* Remark / Cancel reason */}
                    {orderData.remark && (
                        <div className="rounded-[14px] border border-[rgba(229,224,219,0.4)] bg-white/60 px-4 py-[0.85rem]">
                            <p className="mb-[0.35rem] text-[0.78rem] font-semibold text-[#6B6460]">备注</p>
                            <p className="whitespace-pre-wrap text-[0.87rem] leading-relaxed text-[#4A4540]">
                                {orderData.remark}
                            </p>
                        </div>
                    )}
                    {orderData.cancelReason && (
                        <div className="rounded-[14px] border border-[rgba(244,63,94,0.15)] bg-[linear-gradient(135deg,rgba(244,63,94,0.04),rgba(251,113,133,0.02))] px-4 py-[0.85rem]">
                            <p className="mb-[0.35rem] text-[0.78rem] font-semibold text-[#E11D48]">取消原因</p>
                            <p className="whitespace-pre-wrap text-[0.87rem] leading-relaxed text-[#4A4540]">
                                {orderData.cancelReason}
                            </p>
                        </div>
                    )}
                </div>
            ) : null}
        </AdminDetailModal>
    );
}
