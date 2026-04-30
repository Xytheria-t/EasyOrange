import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Package, MapPin, Phone, RefreshCw, User, FileText } from 'lucide-react';
import { useOrderDetail, useCancelOrder, usePayOrder, useReceiveOrder, useRefundOrder } from '@/hooks';
import { getOrderStatusLabel, getOrderStatusFromCode } from '@/types';
import type { OrderStatus } from '@/types';

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const orderId = Number(id);

  const { data: order, isLoading, isError, refetch } = useOrderDetail(orderId);
  const cancelOrder = useCancelOrder();
  const payOrder = usePayOrder();
  const receiveOrder = useReceiveOrder();
  const refundOrder = useRefundOrder();

  const isActionLoading =
    cancelOrder.isPending ||
    payOrder.isPending ||
    receiveOrder.isPending ||
    refundOrder.isPending;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24">
        <RefreshCw size={24} className="animate-spin text-primary-600" />
        <span className="ml-2 text-gray-500">加载中...</span>
      </div>
    );
  }

  if (isError || !order) {
    return (
      <div className="py-6 text-center">
        <p className="text-gray-500">订单不存在或加载失败</p>
        <button
          onClick={() => refetch()}
          className="mt-4 rounded-xl bg-primary-600 px-4 py-2 text-sm text-white hover:bg-primary-700"
        >
          重试
        </button>
      </div>
    );
  }

  const statusKey = getOrderStatusFromCode(order.status);
  const statusLabel = getOrderStatusLabel(order.status);

  const statusColorMap: Record<OrderStatus, string> = {
    PENDING_PAYMENT: 'bg-amber-100 text-amber-700',
    PAID: 'bg-blue-100 text-blue-700',
    SHIPPED: 'bg-indigo-100 text-indigo-700',
    DELIVERED: 'bg-teal-100 text-teal-700',
    COMPLETED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-gray-100 text-gray-500',
    REFUNDED: 'bg-red-100 text-red-700',
  };

  const handleCancel = async () => {
    try { await cancelOrder.mutateAsync({ id: orderId }); } catch { /* handled */ }
  };

  const handlePay = async () => {
    try { await payOrder.mutateAsync(orderId); } catch { /* handled */ }
  };

  const handleReceive = async () => {
    try { await receiveOrder.mutateAsync(orderId); } catch { /* handled */ }
  };

  const handleRefund = async () => {
    try { await refundOrder.mutateAsync({ id: orderId }); } catch { /* handled */ }
  };

  return (
    <div className="py-6">
      <div className="flex items-center gap-3 mb-6">
        <button
          onClick={() => navigate(-1)}
          className="rounded-lg p-2 hover:bg-gray-100 transition-colors"
        >
          <ArrowLeft size={20} className="text-gray-600" />
        </button>
        <h1 className="text-2xl font-bold text-gray-900">订单详情</h1>
      </div>

      <div className="space-y-4">
        <div className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-gray-200/50">
          <div className="flex items-center justify-between mb-4">
            <span className="text-sm text-gray-400 font-mono">{order.orderNo}</span>
            <span className={`text-sm font-medium px-3 py-1 rounded-full ${statusColorMap[statusKey] ?? 'bg-gray-100 text-gray-500'}`}>
              {statusLabel}
            </span>
          </div>

          <div className="flex gap-4">
            <div className="flex-shrink-0 w-24 h-24 rounded-xl overflow-hidden bg-gray-100">
              {order.productImage ? (
                <img src={order.productImage} alt={order.productTitle} className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <Package size={28} className="text-gray-300" />
                </div>
              )}
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="text-base font-medium text-gray-900">{order.productTitle}</h3>
              <p className="mt-2 text-xl font-bold text-primary-600">¥{order.amount.toFixed(2)}</p>
              <p className="mt-1 text-xs text-gray-400">数量：{order.quantity}</p>
            </div>
          </div>
        </div>

        <div className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-gray-200/50">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">收货信息</h3>
          <div className="space-y-2.5">
            <div className="flex items-start gap-3">
              <MapPin size={16} className="text-gray-400 mt-0.5 flex-shrink-0" />
              <span className="text-sm text-gray-600">{order.address || '未填写'}</span>
            </div>
            <div className="flex items-center gap-3">
              <Phone size={16} className="text-gray-400 flex-shrink-0" />
              <span className="text-sm text-gray-600">{order.phone || '未填写'}</span>
            </div>
          </div>
        </div>

        <div className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-gray-200/50">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">交易信息</h3>
          <div className="space-y-2.5">
            <div className="flex items-center gap-3">
              <User size={16} className="text-gray-400 flex-shrink-0" />
              <span className="text-sm text-gray-600">买家：{order.buyerUsername}</span>
            </div>
            <div className="flex items-center gap-3">
              <User size={16} className="text-gray-400 flex-shrink-0" />
              <span className="text-sm text-gray-600">卖家：{order.sellerUsername}</span>
            </div>
            {order.remark && (
              <div className="flex items-start gap-3">
                <FileText size={16} className="text-gray-400 mt-0.5 flex-shrink-0" />
                <span className="text-sm text-gray-600">备注：{order.remark}</span>
              </div>
            )}
          </div>
        </div>

        <div className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-gray-200/50">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">时间信息</h3>
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-gray-400">创建时间</span>
              <span className="text-gray-600">{order.createTime}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-400">更新时间</span>
              <span className="text-gray-600">{order.updateTime}</span>
            </div>
          </div>
        </div>

        <div className="flex gap-3 pt-2">
          {statusKey === 'PENDING_PAYMENT' && (
            <>
              <button
                onClick={handleCancel}
                disabled={isActionLoading}
                className="flex-1 rounded-xl border border-gray-300 py-3 text-sm font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-50"
              >
                取消订单
              </button>
              <button
                onClick={handlePay}
                disabled={isActionLoading}
                className="flex-1 rounded-xl bg-primary-600 py-3 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50"
              >
                立即支付
              </button>
            </>
          )}
          {statusKey === 'PAID' && (
            <button
              onClick={handleRefund}
              disabled={isActionLoading}
              className="flex-1 rounded-xl border border-gray-300 py-3 text-sm font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-50"
            >
              申请退款
            </button>
          )}
          {statusKey === 'SHIPPED' && (
            <button
              onClick={handleReceive}
              disabled={isActionLoading}
              className="flex-1 rounded-xl bg-primary-600 py-3 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50"
            >
              确认收货
            </button>
          )}
          {statusKey === 'DELIVERED' && (
            <button
              onClick={handleReceive}
              disabled={isActionLoading}
              className="flex-1 rounded-xl bg-primary-600 py-3 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50"
            >
              确认完成
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
