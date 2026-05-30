import { useState } from 'react';
import { useCategories } from '@/hooks';

interface FilterSidebarProps {
  isOpen: boolean;
  onClose: () => void;
  onApplyFilters?: (filters: FilterState) => void;
  onResetFilters?: () => void;
  initialFilters?: Partial<FilterState>;
}

export interface FilterState {
  categories: string[];
  priceMin?: number;
  priceMax?: number;
  conditions: number[];
}

export function FilterSidebar({ isOpen, onClose, onApplyFilters, onResetFilters, initialFilters }: FilterSidebarProps) {
  const { data: categories } = useCategories();
  const [selectedCategories, setSelectedCategories] = useState<string[]>(initialFilters?.categories ?? []);
  const [priceRange, setPriceRange] = useState({
    min: initialFilters?.priceMin?.toString() ?? '',
    max: initialFilters?.priceMax?.toString() ?? '',
  });
  const [selectedConditions, setSelectedConditions] = useState<number[]>(initialFilters?.conditions ?? []);
  const handleCategoryToggle = (categoryId: string) => {
    setSelectedCategories(prev =>
      prev.includes(categoryId)
        ? prev.filter(id => id !== categoryId)
        : [...prev, categoryId]
    );
  };

  const handleConditionToggle = (condition: number) => {
    setSelectedConditions(prev =>
      prev.includes(condition)
        ? prev.filter(id => id !== condition)
        : [...prev, condition]
    );
  };

  const handleApplyFilters = () => {
    onApplyFilters?.({
      categories: selectedCategories,
      priceMin: priceRange.min ? Number(priceRange.min) : undefined,
      priceMax: priceRange.max ? Number(priceRange.max) : undefined,
      conditions: selectedConditions,
    });
    onClose();
  };

  const handleResetFilters = () => {
    setSelectedCategories([]);
    setPriceRange({ min: '', max: '' });
    setSelectedConditions([]);
    onResetFilters?.();
  };

  return (
    <>
      {isOpen && (
        <div
          className="filter-overlay active"
          role="button"
          tabIndex={0}
          onClick={onClose}
          onKeyDown={(e) => e.key === 'Enter' && onClose()}
          aria-label="关闭筛选面板"
        />
      )}
      
      <aside className={`filter-sidebar ${isOpen ? 'open' : ''}`}>
        <div className="filter-header">
          <h3>筛选条件</h3>
          <button className="filter-close" onClick={onClose} aria-label="关闭筛选面板">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div className="filter-content">
          <div className="filter-section">
            <h4 className="filter-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              </svg>
              商品分类
            </h4>
            <div className="filter-options">
              {categories?.map(category => (
                <label
                  key={category.id}
                  className={`filter-checkbox ${selectedCategories.includes(category.id) ? 'is-checked' : ''}`}
                >
                  <input
                    type="checkbox"
                    checked={selectedCategories.includes(category.id)}
                    onChange={() => handleCategoryToggle(category.id)}
                  />
                  <span className="checkbox-custom" />
                  <span className="checkbox-label">{category.name}</span>
                  {(category.productCount ?? 0) > 0 && (
                    <span className="condition-icon condition-count">
                      ({category.productCount})
                    </span>
                  )}
                </label>
              ))}
            </div>
          </div>

          <div className="filter-section">
            <h4 className="filter-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="12" y1="1" x2="12" y2="23" />
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
              </svg>
              价格区间
            </h4>
            <div className="price-range">
              <div className="price-inputs">
                <input
                  type="number"
                  name="priceMin"
                  className="price-input"
                  placeholder="最低价"
                  min="0"
                  value={priceRange.min}
                  onChange={e => setPriceRange(prev => ({ ...prev, min: e.target.value }))}
                />
                <span className="price-separator">-</span>
                <input
                  type="number"
                  name="priceMax"
                  className="price-input"
                  placeholder="最高价"
                  min="0"
                  value={priceRange.max}
                  onChange={e => setPriceRange(prev => ({ ...prev, max: e.target.value }))}
                />
              </div>
              <div className="price-presets">
                <button
                  className="preset-btn"
                  onClick={() => setPriceRange({ min: '0', max: '50' })}
                >
                  ¥50 以下
                </button>
                <button
                  className="preset-btn"
                  onClick={() => setPriceRange({ min: '50', max: '200' })}
                >
                  ¥50-200
                </button>
                <button
                  className="preset-btn"
                  onClick={() => setPriceRange({ min: '200', max: '500' })}
                >
                  ¥200-500
                </button>
                <button
                  className="preset-btn"
                  onClick={() => setPriceRange({ min: '500', max: '' })}
                >
                  ¥500 以上
                </button>
              </div>
            </div>
          </div>

          <div className="filter-section">
            <h4 className="filter-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <path d="M12 6v6l4 2" />
              </svg>
              新旧程度
            </h4>
            <div className="filter-options condition-options">
              <label className={`filter-checkbox ${selectedConditions.includes(1) ? 'is-checked' : ''}`}>
                <input type="checkbox" checked={selectedConditions.includes(1)} onChange={() => handleConditionToggle(1)} />
                <span className="checkbox-custom" />
                <span className="checkbox-label">全新</span>
                <span className="condition-icon">✨</span>
              </label>
              <label className={`filter-checkbox ${selectedConditions.includes(2) ? 'is-checked' : ''}`}>
                <input type="checkbox" checked={selectedConditions.includes(2)} onChange={() => handleConditionToggle(2)} />
                <span className="checkbox-custom" />
                <span className="checkbox-label">几乎全新</span>
                <span className="condition-icon">🌟</span>
              </label>
              <label className={`filter-checkbox ${selectedConditions.includes(3) ? 'is-checked' : ''}`}>
                <input type="checkbox" checked={selectedConditions.includes(3)} onChange={() => handleConditionToggle(3)} />
                <span className="checkbox-custom" />
                <span className="checkbox-label">轻微使用</span>
                <span className="condition-icon">💫</span>
              </label>
              <label className={`filter-checkbox ${selectedConditions.includes(4) ? 'is-checked' : ''}`}>
                <input type="checkbox" checked={selectedConditions.includes(4)} onChange={() => handleConditionToggle(4)} />
                <span className="checkbox-custom" />
                <span className="checkbox-label">明显使用</span>
                <span className="condition-icon">⭐</span>
              </label>
            </div>
          </div>


        </div>

        <div className="filter-footer">
          <button className="btn btn-ghost" onClick={handleResetFilters}>
            重置
          </button>
          <button className="btn btn-secondary" onClick={handleApplyFilters}>
            应用筛选
          </button>
        </div>
      </aside>
    </>
  );
}
