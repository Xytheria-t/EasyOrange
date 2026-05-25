import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { CreditCard, Smartphone, Wallet, ArrowLeft, Shield, Loader2 } from 'lucide-react';
import { useCreatePayment, usePaymentStatus, useOrderDetail } from '@/hooks';
import { useUIStore } from '@/store/uiStore';
import type { PaymentMethod } from '@/types';
import '@/styles/main.css';

const PAYMENT_METHODS: { value: PaymentMethod; label: string; icon: typeof CreditCard; desc: string }[] = [
  { value: 'WECHAT', label: '微信支付', icon: Smartphone, desc: '推荐使用' },
  { value: 'ALIPAY', label: '支付宝', icon: CreditCard, desc: '安全便捷' },
  { value: 'CAMPUS_CARD', label: '校园卡', icon: Wallet, desc: '校园专属' },
];

function PaymentPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const addToast = useUIStore((s) => s.addToast);

  const orderId = searchParams.get('orderId') ?? '';
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('WECHAT');
  const [paymentId, setPaymentId] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { data: order, isLoading: orderLoading } = useOrderDetail(orderId);
  const { data: paymentStatus } = usePaymentStatus(
    paymentId ?? '',
    paymentId !== null
  );
  const createPayment = useCreatePayment();

  if (paymentStatus?.status === 'SUCCESS') {
    navigate(`/payment/result?status=success&orderId=${orderId}`, { replace: true });
    return null;
  }

  if (paymentStatus?.status === 'FAILED') {
    navigate(`/payment/result?status=failed&orderId=${orderId}`, { replace: true });
    return null;
  }

  if (!orderId) {
    return (
      <div className="payment-page">
        <div className="payment-error-state">
          <h2>无效的订单</h2>
          <p>请从订单页面重新发起支付</p>
          <button className="btn btn-primary" onClick={() => navigate('/orders')}>
            返回订单
          </button>
        </div>
      </div>
    );
  }

  const handleSubmit = async () => {
    if (!selectedMethod) {
      return;
    }

    setIsSubmitting(true);
    try {
      const result = await createPayment.mutateAsync({
        orderId,
        paymentMethod: selectedMethod,
      });
      setPaymentId(result.paymentId);
      addToast({ type: 'info', message: '支付处理中，请等待...' });
    } catch {
      addToast({ type: 'error', message: '创建支付失败，请重试' });
    } finally {
      setIsSubmitting(false);
    }
  };

  if (orderLoading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner-lg"></div>
        <span className="loading-text">加载订单信息...</span>
      </div>
    );
  }

  const isPaying = paymentId !== null && paymentStatus?.status !== 'FAILED';

  return (
    <div className="payment-page">
      <div className="payment-header">
        <button className="payment-back-btn" onClick={() => navigate(-1)}>
          <ArrowLeft size={20} />
        </button>
        <h1 className="payment-title">收银台</h1>
        <div className="payment-header-spacer" />
      </div>

      <div className="payment-amount-section">
        <p className="payment-amount-label">支付金额</p>
        <p className="payment-amount-value">
          ¥{order?.totalAmount?.toFixed(2) ?? '0.00'}
        </p>
        {order && order.items?.[0] && (
          <div className="payment-order-summary">
            <span className="payment-order-product">{order.items[0].productName}</span>
            <span className="payment-order-quantity">×{order.items[0].quantity}</span>
          </div>
        )}
      </div>

      <div className="payment-methods-section">
        <h2 className="payment-section-title">选择支付方式</h2>
        <div className="payment-methods-list">
          {PAYMENT_METHODS.map((method) => {
            const Icon = method.icon;
            const isSelected = selectedMethod === method.value;
            return (
              <button
                key={method.value}
                className={`payment-method-item ${isSelected ? 'payment-method-selected' : ''}`}
                onClick={() => setSelectedMethod(method.value)}
                disabled={isPaying}
              >
                <div className="payment-method-icon">
                  <Icon size={24} />
                </div>
                <div className="payment-method-info">
                  <span className="payment-method-name">{method.label}</span>
                  <span className="payment-method-desc">{method.desc}</span>
                </div>
                <div className={`payment-method-radio ${isSelected ? 'radio-checked' : ''}`} />
              </button>
            );
          })}
        </div>
      </div>

      <div className="payment-security-note">
        <Shield size={14} />
        <span>支付环境安全，请放心支付</span>
      </div>

      <div className="payment-footer">
        <div className="payment-footer-amount">
          <span>需支付：</span>
          <span className="payment-footer-price">
            ¥{order?.totalAmount?.toFixed(2) ?? '0.00'}
          </span>
        </div>
        <button
          className="payment-submit-btn"
          onClick={handleSubmit}
          disabled={isSubmitting || isPaying}
        >
          {isPaying ? (
            <>
              <Loader2 size={18} className="spin-animation" />
              支付处理中...
            </>
          ) : isSubmitting ? (
            <>
              <Loader2 size={18} className="spin-animation" />
              提交中...
            </>
          ) : (
            <>
              <CreditCard size={18} />
              立即支付
            </>
          )}
        </button>
      </div>
    </div>
  );
}

export default PaymentPage;
