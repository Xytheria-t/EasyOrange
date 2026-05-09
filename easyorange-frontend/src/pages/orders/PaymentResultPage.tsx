import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Clock, Package, ArrowLeft, Sparkles, Home, ShoppingBag, Brain, RefreshCw } from 'lucide-react';
import './payment-result.css';

function PaymentResultPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const status = searchParams.get('status');
  const orderId = searchParams.get('orderId');

  const isSuccess = status === 'success';
  const isFailed = status === 'failed';
  const isPending = !isSuccess && !isFailed;

  return (
    <div className="payment-result-page-v2">
      <div className="payment-result-bg">
        <div className="result-orb result-orb-1"></div>
        <div className="result-orb result-orb-2"></div>
        <div className="result-orb result-orb-3"></div>
      </div>

      <div className="payment-result-header">
        <button className="payment-back-btn" onClick={() => navigate('/orders')}>
          <ArrowLeft size={20} />
        </button>
        <h1 className="payment-title">支付结果</h1>
        <div className="payment-header-spacer" />
      </div>

      <div className="payment-result-content">
        <div className={`payment-result-icon-wrap ${isSuccess ? 'result-success' : isFailed ? 'result-failed' : 'result-pending'}`}>
          <div className="result-icon-glow"></div>
          <div className="result-icon-ring"></div>
          <div className="result-icon-inner">
            {isSuccess ? <CheckCircle size={48} /> : isFailed ? <XCircle size={48} /> : <Clock size={48} />}
          </div>
          {isSuccess && (
            <div className="result-sparkles">
              <Sparkles size={16} className="sparkle-1" />
              <Sparkles size={12} className="sparkle-2" />
              <Sparkles size={14} className="sparkle-3" />
            </div>
          )}
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

        {isSuccess && (
          <div className="payment-result-ai-section">
            <div className="ai-section-header">
              <div className="ai-section-icon">
                <Brain size={18} />
              </div>
              <div className="ai-section-title">
                <h3>智能推荐</h3>
                <span>为你精选相似好物</span>
              </div>
            </div>
            <div className="ai-section-hint">
              <Sparkles size={14} />
              <span>AI正在为你寻找更多好物...</span>
            </div>
          </div>
        )}

        <div className="payment-result-actions">
          {isSuccess && (
            <button
              className="result-btn result-btn-primary"
              onClick={() => navigate(orderId ? `/orders/${orderId}` : '/orders')}
            >
              <Package size={18} />
              查看订单
            </button>
          )}

          {isFailed && (
            <button
              className="result-btn result-btn-primary"
              onClick={() => navigate(orderId ? `/payment?orderId=${orderId}` : '/orders')}
            >
              <RefreshCw size={18} />
              重新支付
            </button>
          )}

          {isPending && (
            <button
              className="result-btn result-btn-primary"
              onClick={() => navigate('/orders')}
            >
              <Package size={18} />
              查看订单
            </button>
          )}

          <button
            className="result-btn result-btn-secondary"
            onClick={() => navigate('/products')}
          >
            <ShoppingBag size={18} />
            继续购物
          </button>

          <button
            className="result-btn result-btn-ghost"
            onClick={() => navigate('/')}
          >
            <Home size={18} />
            返回首页
          </button>
        </div>
      </div>
    </div>
  );
}

export default PaymentResultPage;
