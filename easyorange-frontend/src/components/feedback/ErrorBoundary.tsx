import { Component, type ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home, Bug, Sparkles } from 'lucide-react';
import './error-boundary.css';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    console.error('[ErrorBoundary]', error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="error-boundary-page">
          <div className="error-boundary-bg"></div>

          <div className="error-boundary-content">
            <div className="error-boundary-illustration">
              <div className="error-boundary-icon-wrapper">
                <AlertTriangle />
              </div>
              <div className="error-boundary-glow"></div>
            </div>

            <h1 className="error-boundary-title">页面出错了</h1>
            <p className="error-boundary-subtitle">
              抱歉，页面渲染时发生了错误。请尝试刷新页面或返回首页。
            </p>

            {import.meta.env.DEV && this.state.error && (
              <div className="error-boundary-debug">
                <div className="error-boundary-debug-header">
                  <Bug size={14} />
                  <span>开发者信息</span>
                </div>
                <pre className="error-boundary-debug-content">
                  {this.state.error.message}
                </pre>
              </div>
            )}

            <div className="error-boundary-actions">
              <button className="error-boundary-btn error-boundary-btn-secondary" onClick={this.handleReset}>
                <RefreshCw size={18} />
                重试
              </button>
              <button className="error-boundary-btn error-boundary-btn-primary" onClick={this.handleReload}>
                <Home size={18} />
                刷新页面
              </button>
            </div>

            <div className="error-boundary-tips">
              <div className="error-boundary-tips-header">
                <Sparkles size={14} />
                <span>小贴士</span>
              </div>
              <p>如果问题持续存在，请尝试清除浏览器缓存或联系技术支持</p>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
