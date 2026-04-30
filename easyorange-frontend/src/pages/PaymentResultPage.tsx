import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Clock, Package, ArrowLeft } from 'lucide-react';
import '@/styles/main.css';

export function PaymentResultPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const status = searchParams.get('status');
  const orderId = searchParams.get('orderId');

  const isSuccess = status === 'success';
  const isFailed = status === 'failed';
  const isPending = !isSuccess && !isFailed;

  return (
    <div className="payment-result-page">
      <div className="payment-result-header">
        <button className="payment-back-btn" onClick={() => navigate('/orders')}>
          <ArrowLeft size={20} />
        </button>
        <h1 className="payment-title">支付结果</h1>
        <div className="payment-header-spacer" />
      </div>

      <div className="payment-result-content">
        <div className={`payment-result-icon ${isSuccess ? 'result-success' : isFailed ? 'result-failed' : 'result-pending'}`}>
          {isSuccess ? <CheckCircle size={64} /> : isFailed ? <XCircle size={64} /> : <Clock size={64} />}
        </div>

        <h2 className="payment-result-title">
          {isSuccess ? '支付成功' : isFailed ? '支付失败' : '支付处理中'}
        </h2>

        <p className="payment-result-desc">
          {isSuccess
            ? '您的订单已支付成功，卖家将尽快为您发货'
            : isFailed
            ? '支付未成功，请重新尝试或选择其他支付方式'
            : '支付结果确认中，请稍后查看订单状态'}
        </p>

        {isSuccess && (
          <div className="payment-result-order-hint">
            <Package size={16} />
            <span>您可以到订单详情中查看物流信息</span>
          </div>
        )}

        <div className="payment-result-actions">
          {isSuccess && (
            <button
              className="btn btn-primary payment-result-btn"
              onClick={() => navigate(orderId ? `/orders/${orderId}` : '/orders')}
            >
              查看订单
            </button>
          )}

          {isFailed && (
            <button
              className="btn btn-primary payment-result-btn"
              onClick={() => navigate(orderId ? `/payment?orderId=${orderId}` : '/orders')}
            >
              重新支付
            </button>
          )}

          {isPending && (
            <button
              className="btn btn-primary payment-result-btn"
              onClick={() => navigate('/orders')}
            >
              查看订单
            </button>
          )}

          <button
            className="btn btn-outline payment-result-btn"
            onClick={() => navigate('/products')}
          >
            继续购物
          </button>

          <button
            className="btn btn-ghost payment-result-btn"
            onClick={() => navigate('/orders')}
          >
            返回订单列表
          </button>
        </div>
      </div>
    </div>
  );
}
