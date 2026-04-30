import { useUIStore } from '@/store';
import '@/styles/main.css';

export function GlobalLoading() {
  const { isLoading, loadingMessage } = useUIStore();

  if (!isLoading) return null;

  return (
    <div className="global-loading-overlay">
      <div className="global-loading-modal">
        <div className="global-loading-spinner"></div>
        <p className="global-loading-message">{loadingMessage}</p>
      </div>
    </div>
  );
}
