import { useState } from 'react';

interface ToolsPlazaProps {
  onFilterChange?: (filter: string) => void;
  total?: number;
}

export function ToolsPlaza({ onFilterChange, total = 0 }: ToolsPlazaProps) {
  const [activeFilter, setActiveFilter] = useState('all');

  const handleFilterClick = (filter: string) => {
    setActiveFilter(filter);
    onFilterChange?.(filter);
  };

  return (
    <div className="tools-plaza" id="quickFilters">
      <div className="plaza-header">
        <div className="plaza-brand">
          <div className="brand-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="3" />
              <path d="M12 1v6m0 6v6m11-7h-6m-6 0H1m15.5-6.5l-4.5 4.5m-4 4l-4.5 4.5m9-13l4.5 4.5m-4 4l4.5 4.5" />
            </svg>
          </div>
          <div className="brand-text">
            <span className="brand-title">筛选工具</span>
            <span className="brand-subtitle">TOOLS PLAZA</span>
          </div>
        </div>
        <div className="plaza-status">
          <span className="status-dot" />
          <span className="status-text">{total} 件商品</span>
        </div>
      </div>

      <div className="plaza-tools">
        <div className="tool-group">
          <button
            className={`plaza-tool ${activeFilter === 'all' ? 'active' : ''}`}
            onClick={() => handleFilterClick('all')}
          >
            <div className="tool-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="7" height="7" />
                <rect x="14" y="3" width="7" height="7" />
                <rect x="14" y="14" width="7" height="7" />
                <rect x="3" y="14" width="7" height="7" />
              </svg>
            </div>
            <span className="tool-label">全部</span>
            <div className="tool-badge">ALL</div>
          </button>
        </div>

        <div className="tool-divider" />

        <div className="tool-group">
          <button
            className={`plaza-tool ${activeFilter === 'new' ? 'active' : ''}`}
            onClick={() => handleFilterClick('new')}
          >
            <div className="tool-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" />
              </svg>
            </div>
            <span className="tool-label">最新发布</span>
            <div className="tool-badge">NEW</div>
          </button>

          <button
            className={`plaza-tool ${activeFilter === 'hot' ? 'active' : ''}`}
            onClick={() => handleFilterClick('hot')}
          >
            <div className="tool-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z" />
              </svg>
            </div>
            <span className="tool-label">热门商品</span>
            <div className="tool-badge">HOT</div>
          </button>

          <button
            className={`plaza-tool ${activeFilter === 'discount' ? 'active' : ''}`}
            onClick={() => handleFilterClick('discount')}
          >
            <div className="tool-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
            </div>
            <span className="tool-label">特价优惠</span>
            <div className="tool-badge">SALE</div>
          </button>

          <button
            className={`plaza-tool ${activeFilter === 'newArrival' ? 'active' : ''}`}
            onClick={() => handleFilterClick('newArrival')}
          >
            <div className="tool-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                <line x1="12" y1="22.08" x2="12" y2="12" />
              </svg>
            </div>
            <span className="tool-label">全新物品</span>
            <div className="tool-badge">FRESH</div>
          </button>
        </div>
      </div>

      <div className="plaza-accent">
        <div className="accent-glow" />
        <div className="accent-line" />
      </div>
    </div>
  );
}
