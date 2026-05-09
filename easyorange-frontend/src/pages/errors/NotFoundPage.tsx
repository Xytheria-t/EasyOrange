import { Link, useNavigate } from 'react-router-dom';
import { Home, ArrowLeft, Search, TrendingUp, Sparkles, Brain } from 'lucide-react';
import './not-found.css';

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="not-found-page">
      <div className="not-found-bg"></div>

      <div className="not-found-content">
        <div className="not-found-illustration">
          <div className="not-found-404">404</div>
          <div className="not-found-icon-wrapper">
            <Brain />
          </div>
        </div>

        <h1 className="not-found-title">页面走丢了</h1>
        <p className="not-found-subtitle">
          抱歉，您访问的页面不存在或已被移除。别担心，让AI助手帮你找到正确的方向。
        </p>

        <div className="not-found-actions">
          <button className="not-found-btn not-found-btn-secondary" onClick={() => navigate(-1)}>
            <ArrowLeft size={18} />
            返回上页
          </button>
          <Link to="/" className="not-found-btn not-found-btn-primary">
            <Home size={18} />
            回到首页
          </Link>
        </div>

        <div className="not-found-ai-section">
          <div className="not-found-ai-header">
            <div className="not-found-ai-icon">
              <Sparkles />
            </div>
            <div className="not-found-ai-title">
              <h3>AI智能导航</h3>
              <span>为你推荐可能想去的页面</span>
            </div>
          </div>

          <div className="not-found-ai-suggestions">
            <Link to="/" className="not-found-ai-suggestion">
              <div className="suggestion-icon home">
                <Home />
              </div>
              <div className="suggestion-content">
                <span>浏览首页</span>
                <small>发现最新上架的好物</small>
              </div>
              <ArrowLeft size={16} className="suggestion-arrow" style={{ transform: 'rotate(180deg)' }} />
            </Link>

            <Link to="/search" className="not-found-ai-suggestion">
              <div className="suggestion-icon search">
                <Search />
              </div>
              <div className="suggestion-content">
                <span>搜索商品</span>
                <small>输入关键词找到你想要的</small>
              </div>
              <ArrowLeft size={16} className="suggestion-arrow" style={{ transform: 'rotate(180deg)' }} />
            </Link>

            <Link to="/products" className="not-found-ai-suggestion">
              <div className="suggestion-icon hot">
                <TrendingUp />
              </div>
              <div className="suggestion-content">
                <span>热门商品</span>
                <small>看看大家都在关注什么</small>
              </div>
              <ArrowLeft size={16} className="suggestion-arrow" style={{ transform: 'rotate(180deg)' }} />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
