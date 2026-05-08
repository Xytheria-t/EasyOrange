import { useUIStore } from '@/store';
import '@/styles/loading.css';

export function GlobalLoading() {
  const { isLoading, loadingMessage } = useUIStore();

  if (!isLoading) {return null;}

  return (
    <div className="global-loading-overlay">
      <div className="global-loading-backdrop"></div>
      <div className="global-loading-modal">
        <div className="global-loading-brand">
          <div className="global-loading-logo">
            <svg viewBox="0 0 48 48" fill="none">
              <defs>
                <linearGradient id="loadingLogoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#F97316"/>
                  <stop offset="40%" stopColor="#FB7185"/>
                  <stop offset="100%" stopColor="#C39BD3"/>
                </linearGradient>
              </defs>
              <path d="M8 12h13M8 12v24M8 36h13M8 12h8M8 24h10" stroke="url(#loadingLogoGradient)" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
              <circle cx="36" cy="24" r="10" stroke="url(#loadingLogoGradient)" strokeWidth="3"/>
            </svg>
          </div>
          <div className="global-loading-spinner-ring">
            <div className="ring-segment"></div>
            <div className="ring-segment"></div>
            <div className="ring-segment"></div>
            <div className="ring-segment"></div>
          </div>
        </div>
        <p className="global-loading-message">{loadingMessage || '加载中...'}</p>
        <div className="global-loading-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
  );
}
