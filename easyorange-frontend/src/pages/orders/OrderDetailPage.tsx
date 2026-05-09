import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Package, MapPin, Phone, RefreshCw, User, FileText, Truck, CheckCircle, Clock, XCircle, CreditCard } from 'lucide-react';
import { useOrderDetail, useCancelOrder, usePayOrder, useReceiveOrder, useRefundOrder } from '@/hooks';
import { getOrderStatusLabel, getOrderStatusFromCode } from '@/types';
import type { OrderStatus } from '@/types';

const STATUS_HERO_MAP: Record<OrderStatus, { gradient: string; icon: typeof Clock; hint: string }> = {
  PENDING_PAYMENT: {
    gradient: 'linear-gradient(135deg, #FBBF24 0%, #F97316 50%, #EA580C 100%)',
    icon: Clock,
    hint: '请尽快完成支付，超时订单将自动取消',
  },
  PAID: {
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #6366F1 50%, #8B5CF6 100%)',
    icon: Package,
    hint: '卖家正在准备发货，请耐心等待',
  },
  SHIPPED: {
    gradient: 'linear-gradient(135deg, #8B5CF6 0%, #A855F7 50%, #C39BD3 100%)',
    icon: Truck,
    hint: '商品正在配送中，请注意查收',
  },
  COMPLETED: {
    gradient: 'linear-gradient(135deg, #10B981 0%, #059669 50%, #047857 100%)',
    icon: CheckCircle,
    hint: '交易已完成，感谢您的购买',
  },
  CANCELLED: {
    gradient: 'linear-gradient(135deg, #A8A098 0%, #787068 50%, #5C544C 100%)',
    icon: XCircle,
    hint: '订单已取消',
  },
  REFUNDED: {
    gradient: 'linear-gradient(135deg, #F43F5E 0%, #E11D48 50%, #BE123C 100%)',
    icon: CreditCard,
    hint: '退款处理中，请留意账户变动',
  },
};

const TIMELINE_STEPS = [
  { key: 'PENDING_PAYMENT', label: '下单' },
  { key: 'PAID', label: '付款' },
  { key: 'SHIPPED', label: '发货' },
  { key: 'COMPLETED', label: '完成' },
] as const;

const STATUS_ORDER: Record<OrderStatus, number> = {
  PENDING_PAYMENT: 0,
  PAID: 1,
  SHIPPED: 2,
  COMPLETED: 3,
  CANCELLED: -1,
  REFUNDED: -1,
};

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
      <div className="order-detail-loading">
        <div className="order-detail-loading-spinner">
          <RefreshCw size={32} />
        </div>
        <span className="order-detail-loading-text">加载订单详情...</span>
      </div>
    );
  }

  if (isError || !order) {
    return (
      <div className="order-detail-error">
        <div className="order-detail-error-icon">!</div>
        <p className="order-detail-error-text">订单不存在或加载失败</p>
        <button onClick={() => refetch()} className="order-detail-error-btn">
          重新加载
        </button>
      </div>
    );
  }

  const statusKey = getOrderStatusFromCode(order.status);
  const statusLabel = getOrderStatusLabel(order.status);
  const heroStyle = STATUS_HERO_MAP[statusKey] ?? STATUS_HERO_MAP.CANCELLED;
  const StatusIcon = heroStyle.icon;
  const currentStep = STATUS_ORDER[statusKey];

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
    <div className="order-detail-premium">
      <div className="order-detail-nav">
        <button
          onClick={() => navigate(-1)}
          className="order-detail-back-btn"
        >
          <ArrowLeft size={20} />
        </button>
        <h1 className="order-detail-nav-title">订单详情</h1>
        <div className="order-detail-nav-spacer" />
      </div>

      <div className="order-detail-status-hero" style={{ background: heroStyle.gradient }}>
        <div className="order-detail-status-hero-glow" />
        <div className="order-detail-status-hero-content">
          <div className="order-detail-status-hero-icon">
            <StatusIcon size={28} />
          </div>
          <div className="order-detail-status-hero-text">
            <h2 className="order-detail-status-hero-label">{statusLabel}</h2>
            <p className="order-detail-status-hero-hint">{heroStyle.hint}</p>
          </div>
        </div>
      </div>

      {currentStep >= 0 && (
        <div className="order-detail-timeline-premium">
          {TIMELINE_STEPS.map((step, index) => {
            const isCompleted = currentStep > index;
            const isCurrent = currentStep === index;
            return (
              <div
                key={step.key}
                className={`order-detail-timeline-step ${isCompleted ? 'timeline-completed' : ''} ${isCurrent ? 'timeline-current' : ''}`}
              >
                <div className="timeline-step-dot-wrap">
                  <div className="timeline-step-dot">
                    {isCompleted && <CheckCircle size={10} />}
                  </div>
                  {index < TIMELINE_STEPS.length - 1 && (
                    <div className={`timeline-step-line ${isCompleted ? 'line-completed' : ''}`} />
                  )}
                </div>
                <span className="timeline-step-label">{step.label}</span>
              </div>
            );
          })}
        </div>
      )}

      <div className="order-detail-sections">
        <div className="order-detail-section">
          <div className="order-detail-section-header">
            <Package size={16} className="order-detail-section-icon" />
            <h3 className="order-detail-section-title">商品信息</h3>
          </div>
          <div className="order-detail-product-premium">
            <div className="order-detail-product-image-wrap">
              <div className="order-detail-product-image-glow" />
              {order.productImage ? (
                <img src={order.productImage} alt={order.productTitle} className="order-detail-product-image" />
              ) : (
                <div className="order-detail-product-image-placeholder">
                  <Package size={32} />
                </div>
              )}
            </div>
            <div className="order-detail-product-info">
              <h4 className="order-detail-product-name">{order.productTitle}</h4>
              <div className="order-detail-product-meta-row">
                <span className="order-detail-product-price">¥{order.amount.toFixed(2)}</span>
                <span className="order-detail-product-qty">×{order.quantity}</span>
              </div>
            </div>
          </div>
        </div>

        <div className="order-detail-section">
          <div className="order-detail-section-header">
            <MapPin size={16} className="order-detail-section-icon" />
            <h3 className="order-detail-section-title">收货信息</h3>
          </div>
          <div className="order-detail-info-grid">
            <div className="order-detail-info-item">
              <MapPin size={14} className="order-detail-info-item-icon" />
              <span className="order-detail-info-item-label">地址</span>
              <span className="order-detail-info-item-value">{order.address || '未填写'}</span>
            </div>
            <div className="order-detail-info-item">
              <Phone size={14} className="order-detail-info-item-icon" />
              <span className="order-detail-info-item-label">电话</span>
              <span className="order-detail-info-item-value">{order.phone || '未填写'}</span>
            </div>
          </div>
        </div>

        <div className="order-detail-section">
          <div className="order-detail-section-header">
            <User size={16} className="order-detail-section-icon" />
            <h3 className="order-detail-section-title">交易信息</h3>
          </div>
          <div className="order-detail-info-grid">
            <div className="order-detail-info-item">
              <User size={14} className="order-detail-info-item-icon" />
              <span className="order-detail-info-item-label">买家</span>
              <span className="order-detail-info-item-value">{order.buyerUsername}</span>
            </div>
            <div className="order-detail-info-item">
              <User size={14} className="order-detail-info-item-icon" />
              <span className="order-detail-info-item-label">卖家</span>
              <span className="order-detail-info-item-value">{order.sellerUsername}</span>
            </div>
            {order.remark && (
              <div className="order-detail-info-item">
                <FileText size={14} className="order-detail-info-item-icon" />
                <span className="order-detail-info-item-label">备注</span>
                <span className="order-detail-info-item-value">{order.remark}</span>
              </div>
            )}
          </div>
        </div>

        <div className="order-detail-section">
          <div className="order-detail-section-header">
            <Clock size={16} className="order-detail-section-icon" />
            <h3 className="order-detail-section-title">时间信息</h3>
          </div>
          <div className="order-detail-info-grid">
            <div className="order-detail-info-item">
              <span className="order-detail-info-item-label">创建时间</span>
              <span className="order-detail-info-item-value">{order.createTime}</span>
            </div>
            <div className="order-detail-info-item">
              <span className="order-detail-info-item-label">更新时间</span>
              <span className="order-detail-info-item-value">{order.updateTime}</span>
            </div>
            <div className="order-detail-info-item">
              <span className="order-detail-info-item-label">订单号</span>
              <span className="order-detail-info-item-value order-detail-order-no">{order.orderNo}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="order-detail-actions-premium">
        {statusKey === 'PENDING_PAYMENT' && (
          <>
            <button
              onClick={handleCancel}
              disabled={isActionLoading}
              className="order-detail-btn-secondary"
            >
              取消订单
            </button>
            <button
              onClick={handlePay}
              disabled={isActionLoading}
              className="order-detail-btn-primary"
            >
              立即支付
            </button>
          </>
        )}
        {statusKey === 'PAID' && (
          <button
            onClick={handleRefund}
            disabled={isActionLoading}
            className="order-detail-btn-secondary"
          >
            申请退款
          </button>
        )}
        {statusKey === 'SHIPPED' && (
          <button
            onClick={handleReceive}
            disabled={isActionLoading}
            className="order-detail-btn-primary"
          >
            确认收货
          </button>
        )}
      </div>
    </div>
  );
}
