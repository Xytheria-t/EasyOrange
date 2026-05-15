import { useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Package, Clock, Truck, CheckCircle, XCircle, ChevronRight, RefreshCw, ShoppingBag, Sparkles, Loader2 } from 'lucide-react';
import { useMyOrders, useCancelOrder, usePayOrder, useReceiveOrder } from '@/hooks';
import { getOrderStatusLabel, getOrderStatusFromCode } from '@/constants';
import type { Order, OrderStatus } from '@/types';
import { useUIStore } from '@/store';
import './payment.css';

const STATUS_TAB_MAP: { id: string; label: string; icon: typeof Package; statusCode?: number }[] = [
  { id: 'all', label: '全部', icon: Package, statusCode: undefined },
  { id: 'PENDING_PAYMENT', label: '待付款', icon: Clock, statusCode: 0 },
  { id: 'PAID', label: '待发货', icon: Package, statusCode: 1 },
  { id: 'SHIPPED', label: '已发货', icon: Truck, statusCode: 2 },
  { id: 'COMPLETED', label: '已完成', icon: CheckCircle, statusCode: 3 },
  { id: 'CANCELLED', label: '已取消', icon: XCircle, statusCode: 4 },
];

const STATUS_STYLE_MAP: Record<OrderStatus, { bg: string; text: string; border: string; glow: string; dot: string }> = {
  PENDING_PAYMENT: {
    bg: 'rgba(251, 191, 36, 0.08)',
    text: '#D97706',
    border: 'rgba(251, 191, 36, 0.2)',
    glow: '0 0 20px rgba(251, 191, 36, 0.15)',
    dot: '#FBBF24',
  },
  PAID: {
    bg: 'rgba(59, 130, 246, 0.08)',
    text: '#2563EB',
    border: 'rgba(59, 130, 246, 0.2)',
    glow: '0 0 20px rgba(59, 130, 246, 0.15)',
    dot: '#3B82F6',
  },
  SHIPPED: {
    bg: 'rgba(139, 92, 246, 0.08)',
    text: '#7C3AED',
    border: 'rgba(139, 92, 246, 0.2)',
    glow: '0 0 20px rgba(139, 92, 246, 0.15)',
    dot: '#8B5CF6',
  },
  COMPLETED: {
    bg: 'rgba(16, 185, 129, 0.08)',
    text: '#059669',
    border: 'rgba(16, 185, 129, 0.2)',
    glow: '0 0 20px rgba(16, 185, 129, 0.15)',
    dot: '#10B981',
  },
  CANCELLED: {
    bg: 'rgba(168, 160, 152, 0.08)',
    text: '#787068',
    border: 'rgba(168, 160, 152, 0.2)',
    glow: '0 0 20px rgba(168, 160, 152, 0.1)',
    dot: '#A8A098',
  },
  REFUNDED: {
    bg: 'rgba(244, 63, 94, 0.08)',
    text: '#E11D48',
    border: 'rgba(244, 63, 94, 0.2)',
    glow: '0 0 20px rgba(244, 63, 94, 0.15)',
    dot: '#F43F5E',
  },
};

function OrdersPage() {
  const [activeTab, setActiveTab] = useState('all');
  const navigate = useNavigate();

  const queryParams = useMemo(() => {
    const tab = STATUS_TAB_MAP.find(t => t.id === activeTab);
    if (!tab?.statusCode && tab?.statusCode !== 0) {return {};}
    return { status: tab.statusCode };
  }, [activeTab]);

  const { data, isLoading, isError, refetch } = useMyOrders(queryParams);
  const cancelOrder = useCancelOrder();
  const payOrder = usePayOrder();
  const receiveOrder = useReceiveOrder();
  const addToast = useUIStore((s) => s.addToast);

  const [cancellingId, setCancellingId] = useState<string | null>(null);

  const orders = data?.records ?? [];

  const getErrorMessage = useCallback((err: unknown): string => {
    if (err instanceof Error) {
      const msg = err.message;
      if (msg.includes('B3007') || msg.includes('无法取消')) {return '该订单当前无法取消，可能已支付或已发货';}
      if (msg.includes('B3001') || msg.includes('不存在')) {return '订单信息已变更，请刷新页面重试';}
      if (msg.includes('B3003') || msg.includes('非订单所有者')) {return '您没有权限操作此订单';}
      return msg;
    }
    return '操作失败，请重试';
  }, []);

  const handleCancel = async (id: string) => {
    setCancellingId(id);
    try {
      await cancelOrder.mutateAsync({ id });
      addToast({ type: 'success', message: '订单已取消' });
    } catch (err: unknown) {
      addToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setCancellingId(null);
    }
  };

  const handlePay = async (id: string) => {
    try {
      await payOrder.mutateAsync(id);
      addToast({ type: 'success', message: '支付请求已提交' });
    } catch (err: unknown) {
      addToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  const handleReceive = async (id: string) => {
    try {
      await receiveOrder.mutateAsync(id);
      addToast({ type: 'success', message: '已确认收货' });
    } catch (err: unknown) {
      addToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  return (
    <div className="orders-page-premium">
      <div className="orders-hero">
        <div className="orders-hero-bg" />
        <div className="orders-hero-content">
          <h1 className="orders-hero-title">
            <Sparkles size={20} className="orders-hero-icon" />
            我的订单
          </h1>
          <p className="orders-hero-subtitle">追踪每一笔交易，掌控购物旅程</p>
        </div>
      </div>

      <div className="orders-tabs-premium">
        {STATUS_TAB_MAP.map((tab, index) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`orders-tab-item ${isActive ? 'orders-tab-active' : ''}`}
              style={{ animationDelay: `${index * 60}ms` }}
            >
              <Icon size={15} className="orders-tab-icon" />
              <span>{tab.label}</span>
              {isActive && <div className="orders-tab-indicator" />}
            </button>
          );
        })}
      </div>

      {isLoading && (
        <div className="orders-loading">
          <div className="orders-loading-spinner">
            <RefreshCw size={28} />
          </div>
          <span className="orders-loading-text">正在加载订单...</span>
        </div>
      )}

      {isError && (
        <div className="orders-error-card">
          <div className="orders-error-icon">!</div>
          <p className="orders-error-text">加载失败，请稍后重试</p>
          <button onClick={() => refetch()} className="orders-error-btn">
            重新加载
          </button>
        </div>
      )}

      {!isLoading && !isError && orders.length === 0 && (
        <div className="orders-empty-premium">
          <div className="orders-empty-visual">
            <div className="orders-empty-orb orders-empty-orb-1" />
            <div className="orders-empty-orb orders-empty-orb-2" />
            <div className="orders-empty-icon-wrap">
              <ShoppingBag size={40} />
            </div>
          </div>
          <h3 className="orders-empty-title">暂无订单</h3>
          <p className="orders-empty-desc">
            还没有购买任何商品<br />去发现心仪的宝贝吧
          </p>
          <button
            onClick={() => navigate('/products')}
            className="orders-empty-cta"
          >
            探索好物
            <ChevronRight size={16} />
          </button>
        </div>
      )}

      {!isLoading && !isError && orders.length > 0 && (
        <div className="orders-list-premium">
          {orders.map((order, index) => (
            <OrderCard
              key={order.id}
              order={order}
              onCancel={handleCancel}
              onPay={handlePay}
              onReceive={handleReceive}
              onClick={() => navigate(`/orders/${order.id}`)}
              isCancelling={cancellingId === order.id}
              index={index}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default OrdersPage;

interface OrderCardProps {
  order: Order;
  onCancel: (id: string) => void;
  onPay: (id: string) => void;
  onReceive: (id: string) => void;
  onClick: () => void;
  isCancelling: boolean;
  index: number;
}

function OrderCard({ order, onCancel, onPay, onReceive, onClick, isCancelling, index }: OrderCardProps) {
  const statusKey = getOrderStatusFromCode(order.status);
  const statusLabel = getOrderStatusLabel(order.status);
  const statusStyle = STATUS_STYLE_MAP[statusKey] ?? STATUS_STYLE_MAP.CANCELLED;

  return (
    <div
      className="order-card-premium"
      style={{ animationDelay: `${index * 80}ms` }}
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(e) => e.key === 'Enter' && onClick()}
    >
      <div className="order-card-shine" />

      <div className="order-card-header-premium">
        <span className="order-card-order-no">{order.orderNo}</span>
        <span
          className="order-card-status-badge"
          style={{
            background: statusStyle.bg,
            color: statusStyle.text,
            borderColor: statusStyle.border,
            boxShadow: statusStyle.glow,
          }}
        >
          <span
            className="order-card-status-dot"
            style={{ background: statusStyle.dot }}
          />
          {statusLabel}
        </span>
      </div>

      <div className="order-card-body-premium">
        <div className="order-card-image-wrap">
          <div className="order-card-image-glow" />
          {order.productImage ? (
            <img
              src={order.productImage}
              alt={order.productTitle}
              className="order-card-image-premium"
            />
          ) : (
            <div className="order-card-image-placeholder">
              <Package size={24} />
            </div>
          )}
        </div>

        <div className="order-card-info-premium">
          <h3 className="order-card-title-premium">{order.productTitle}</h3>
          <p className="order-card-seller-premium">卖家：{order.sellerUsername}</p>
          <div className="order-card-price-row">
            <span className="order-card-price-premium">¥{order.amount.toFixed(2)}</span>
            <span className="order-card-qty">×{order.quantity}</span>
          </div>
        </div>

        <ChevronRight size={18} className="order-card-arrow-premium" />
      </div>

      <div className="order-card-footer-premium">
        <span className="order-card-time-premium">{order.createTime}</span>
        <div
          className="order-card-actions-premium"
          role="group"
          aria-label="订单操作"
        >
          {statusKey === 'PENDING_PAYMENT' && (
            <>
              <button
                onClick={() => onCancel(order.id)}
                disabled={isCancelling}
                className="order-btn-secondary"
              >
                {isCancelling ? <Loader2 size={14} className="animate-spin" /> : null}
                取消订单
              </button>
              <button
                onClick={() => onPay(order.id)}
                disabled={isCancelling}
                className="order-btn-primary"
              >
                立即支付
              </button>
            </>
          )}
          {statusKey === 'SHIPPED' && (
            <button
              onClick={() => onReceive(order.id)}
              className="order-btn-primary"
            >
              确认收货
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
