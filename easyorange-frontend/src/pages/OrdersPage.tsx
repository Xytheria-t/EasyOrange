import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Package, Clock, Truck, CheckCircle, XCircle, ChevronRight, RefreshCw } from 'lucide-react';
import { useMyOrders, useCancelOrder, usePayOrder, useReceiveOrder } from '@/hooks';
import { getOrderStatusLabel, getOrderStatusFromCode } from '@/types';
import type { Order, OrderStatus } from '@/types';

const STATUS_TAB_MAP: { id: string; label: string; icon: typeof Package; statusCode?: number }[] = [
  { id: 'all', label: '全部', icon: Package },
  { id: 'PENDING_PAYMENT', label: '待付款', icon: Clock, statusCode: 0 },
  { id: 'PAID', label: '待发货', icon: Package, statusCode: 1 },
  { id: 'SHIPPED', label: '已发货', icon: Truck, statusCode: 2 },
  { id: 'COMPLETED', label: '已完成', icon: CheckCircle, statusCode: 4 },
  { id: 'CANCELLED', label: '已取消', icon: XCircle, statusCode: 5 },
];

export function OrdersPage() {
  const [activeTab, setActiveTab] = useState('all');
  const navigate = useNavigate();

  const queryParams = useMemo(() => {
    const tab = STATUS_TAB_MAP.find(t => t.id === activeTab);
    if (!tab?.statusCode && tab?.statusCode !== 0) return {};
    return { status: tab.statusCode };
  }, [activeTab]);

  const { data, isLoading, isError, refetch } = useMyOrders(queryParams);
  const cancelOrder = useCancelOrder();
  const payOrder = usePayOrder();
  const receiveOrder = useReceiveOrder();

  const orders = data?.records ?? [];

  const handleCancel = async (id: number) => {
    try {
      await cancelOrder.mutateAsync({ id });
    } catch {
      // error handled by mutation state
    }
  };

  const handlePay = async (id: number) => {
    try {
      await payOrder.mutateAsync(id);
    } catch {
      // error handled by mutation state
    }
  };

  const handleReceive = async (id: number) => {
    try {
      await receiveOrder.mutateAsync(id);
    } catch {
      // error handled by mutation state
    }
  };

  return (
    <div className="py-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">订单管理</h1>
        <p className="mt-1 text-sm text-gray-500">查看和管理您的订单</p>
      </div>

      <div className="mb-6 flex gap-2 overflow-x-auto pb-2">
        {STATUS_TAB_MAP.map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 whitespace-nowrap rounded-xl px-4 py-2 text-sm font-medium transition-all ${
                activeTab === tab.id
                  ? 'bg-primary-600 text-white shadow-sm'
                  : 'bg-white text-gray-600 hover:bg-gray-50 shadow-sm'
              }`}
            >
              <Icon size={16} />
              {tab.label}
            </button>
          );
        })}
      </div>

      {isLoading && (
        <div className="flex items-center justify-center py-16">
          <RefreshCw size={24} className="animate-spin text-primary-600" />
          <span className="ml-2 text-gray-500">加载中...</span>
        </div>
      )}

      {isError && (
        <div className="rounded-2xl bg-white p-8 shadow-sm ring-1 ring-gray-200/50 text-center">
          <p className="text-gray-500">加载失败，请重试</p>
          <button
            onClick={() => refetch()}
            className="mt-4 rounded-xl bg-primary-600 px-4 py-2 text-sm text-white hover:bg-primary-700"
          >
            重试
          </button>
        </div>
      )}

      {!isLoading && !isError && orders.length === 0 && (
        <div className="rounded-2xl bg-white p-8 shadow-sm ring-1 ring-gray-200/50">
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <div className="flex h-20 w-20 items-center justify-center rounded-full bg-gray-100">
              <Package size={36} className="text-gray-400" />
            </div>
            <h3 className="mt-4 text-lg font-semibold text-gray-900">暂无订单</h3>
            <p className="mt-2 max-w-sm text-sm text-gray-500">
              您还没有购买任何商品，快去挑选心仪的宝贝吧！
            </p>
            <button
              onClick={() => navigate('/products')}
              className="mt-6 rounded-xl bg-primary-600 px-6 py-3 font-semibold text-white shadow-sm hover:bg-primary-700 transition-all"
            >
              去购物
            </button>
          </div>
        </div>
      )}

      {!isLoading && !isError && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map((order) => (
            <OrderCard
              key={order.id}
              order={order}
              onCancel={handleCancel}
              onPay={handlePay}
              onReceive={handleReceive}
              onClick={() => navigate(`/orders/${order.id}`)}
              isActionLoading={
                cancelOrder.isPending ||
                payOrder.isPending ||
                receiveOrder.isPending
              }
            />
          ))}
        </div>
      )}
    </div>
  );
}

interface OrderCardProps {
  order: Order;
  onCancel: (id: number) => void;
  onPay: (id: number) => void;
  onReceive: (id: number) => void;
  onClick: () => void;
  isActionLoading: boolean;
}

function OrderCard({ order, onCancel, onPay, onReceive, onClick, isActionLoading }: OrderCardProps) {
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

  return (
    <div
      className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-gray-200/50 hover:shadow-md transition-shadow cursor-pointer"
      onClick={onClick}
    >
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs text-gray-400 font-mono">{order.orderNo}</span>
        <span className={`text-xs font-medium px-2.5 py-1 rounded-full ${statusColorMap[statusKey] ?? 'bg-gray-100 text-gray-500'}`}>
          {statusLabel}
        </span>
      </div>

      <div className="flex gap-4">
        <div className="flex-shrink-0 w-20 h-20 rounded-xl overflow-hidden bg-gray-100">
          {order.productImage ? (
            <img
              src={order.productImage}
              alt={order.productTitle}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <Package size={24} className="text-gray-300" />
            </div>
          )}
        </div>

        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-medium text-gray-900 truncate">{order.productTitle}</h3>
          <p className="mt-1 text-xs text-gray-400">卖家：{order.sellerUsername}</p>
          <div className="mt-2 flex items-center justify-between">
            <span className="text-base font-bold text-primary-600">¥{order.amount.toFixed(2)}</span>
            <span className="text-xs text-gray-400">x{order.quantity}</span>
          </div>
        </div>

        <div className="flex items-center">
          <ChevronRight size={18} className="text-gray-300" />
        </div>
      </div>

      <div className="mt-3 pt-3 border-t border-gray-100 flex items-center justify-between">
        <span className="text-xs text-gray-400">{order.createTime}</span>
        <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
          {statusKey === 'PENDING_PAYMENT' && (
            <>
              <button
                onClick={() => onCancel(order.id)}
                disabled={isActionLoading}
                className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-50"
              >
                取消订单
              </button>
              <button
                onClick={() => onPay(order.id)}
                disabled={isActionLoading}
                className="rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-primary-700 disabled:opacity-50"
              >
                立即支付
              </button>
            </>
          )}
          {statusKey === 'SHIPPED' && (
            <button
              onClick={() => onReceive(order.id)}
              disabled={isActionLoading}
              className="rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-primary-700 disabled:opacity-50"
            >
              确认收货
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
