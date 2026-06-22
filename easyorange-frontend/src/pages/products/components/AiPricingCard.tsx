import { Sparkles, TrendingUp, Zap, Star } from 'lucide-react';

interface AiPricingCardProps {
  price: number;
}

export function AiPricingCard({ price }: AiPricingCardProps) {
  const marketPrice = (price || 100) * 1.15;
  const lowPrice = (price || 100) * 0.85;

  return (
    <div className="pdp-ai-pricing-card">
      <div className="pdp-ai-pricing-header">
        <div className="pdp-ai-badge">
          <Sparkles size={14} />
          <span>AI智能估价</span>
        </div>
        <span className="pdp-ai-confidence">置信度 95%</span>
      </div>
      <div className="pdp-ai-pricing-body">
        <div className="pdp-ai-price-range">
          <div className="pdp-ai-price-item">
            <span className="pdp-ai-price-label">市场均价</span>
            <span className="pdp-ai-price-value">¥{marketPrice.toFixed(0)}</span>
          </div>
          <div className="pdp-ai-price-divider" />
          <div className="pdp-ai-price-item">
            <span className="pdp-ai-price-label">低价区间</span>
            <span className="pdp-ai-price-value low">¥{lowPrice.toFixed(0)}</span>
          </div>
          <div className="pdp-ai-price-divider" />
          <div className="pdp-ai-price-item highlight">
            <span className="pdp-ai-price-label">当前定价</span>
            <span className="pdp-ai-price-value">¥{price.toFixed(0)}</span>
          </div>
        </div>
        <div className="pdp-ai-pricing-analysis">
          <div className="pdp-ai-analysis-icon">
            <TrendingUp size={14} />
          </div>
          <p className="pdp-ai-analysis-text">
            该商品定价<span className="highlight">合理偏低</span>，相比同类商品具有价格优势，性价比突出
          </p>
        </div>
        <div className="pdp-ai-pricing-tags">
          <span className="pdp-ai-tag"><Zap size={10} />价格优势</span>
          <span className="pdp-ai-tag"><Star size={10} />值得购买</span>
          <span className="pdp-ai-tag"><TrendingUp size={10} />热门品类</span>
        </div>
      </div>
    </div>
  );
}
