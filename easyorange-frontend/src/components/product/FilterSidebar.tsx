import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
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
            prev.includes(categoryId) ? prev.filter(id => id !== categoryId) : [...prev, categoryId]
        );
    };

    const handleConditionToggle = (condition: number) => {
        setSelectedConditions(prev =>
            prev.includes(condition) ? prev.filter(id => id !== condition) : [...prev, condition]
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
                <button
                    type="button"
                    className="filter-overlay active block border-0 bg-transparent p-0"
                    onClick={onClose}
                    aria-label="关闭筛选面板"
                />
            )}

            <aside className={`filter-sidebar ${isOpen ? 'open' : ''}`}>
                <div className="filter-header">
                    <h3>筛选条件</h3>
                    <Button
                        variant="ghost"
                        size="icon"
                        className="filter-close"
                        onClick={onClose}
                        aria-label="关闭筛选面板"
                    >
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                            <line x1="18" y1="6" x2="6" y2="18" />
                            <line x1="6" y1="6" x2="18" y2="18" />
                        </svg>
                    </Button>
                </div>

                <div className="filter-content">
                    <div className="filter-section">
                        <h4 className="filter-title">
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                aria-hidden="true"
                            >
                                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                            </svg>
                            商品分类
                        </h4>
                        <div className="filter-options">
                            {categories?.map(category => (
                                <div key={category.id} className="filter-checkbox">
                                    <Checkbox
                                        id={`cat-${category.id}`}
                                        checked={selectedCategories.includes(category.id)}
                                        onCheckedChange={() => handleCategoryToggle(category.id)}
                                    />
                                    <label htmlFor={`cat-${category.id}`} className="checkbox-label">
                                        {category.name}
                                    </label>
                                    {(category.productCount ?? 0) > 0 && (
                                        <span className="condition-icon condition-count">
                                            ({category.productCount})
                                        </span>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="filter-section">
                        <h4 className="filter-title">
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                aria-hidden="true"
                            >
                                <line x1="12" y1="1" x2="12" y2="23" />
                                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
                            </svg>
                            价格区间
                        </h4>
                        <div className="price-range">
                            <div className="price-inputs">
                                <Input
                                    type="number"
                                    name="priceMin"
                                    className="price-input"
                                    placeholder="最低价"
                                    min="0"
                                    value={priceRange.min}
                                    onChange={e => setPriceRange(prev => ({ ...prev, min: e.target.value }))}
                                />
                                <span className="price-separator">-</span>
                                <Input
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
                                <Button
                                    variant="outline"
                                    size="sm"
                                    className="preset-btn"
                                    onClick={() => setPriceRange({ min: '0', max: '50' })}
                                >
                                    ¥50 以下
                                </Button>
                                <Button
                                    variant="outline"
                                    size="sm"
                                    className="preset-btn"
                                    onClick={() => setPriceRange({ min: '50', max: '200' })}
                                >
                                    ¥50-200
                                </Button>
                                <Button
                                    variant="outline"
                                    size="sm"
                                    className="preset-btn"
                                    onClick={() => setPriceRange({ min: '200', max: '500' })}
                                >
                                    ¥200-500
                                </Button>
                                <Button
                                    variant="outline"
                                    size="sm"
                                    className="preset-btn"
                                    onClick={() => setPriceRange({ min: '500', max: '' })}
                                >
                                    ¥500 以上
                                </Button>
                            </div>
                        </div>
                    </div>

                    <div className="filter-section">
                        <h4 className="filter-title">
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                aria-hidden="true"
                            >
                                <circle cx="12" cy="12" r="10" />
                                <path d="M12 6v6l4 2" />
                            </svg>
                            新旧程度
                        </h4>
                        <div className="filter-options condition-options">
                            <div className="filter-checkbox">
                                <Checkbox
                                    id="cond-1"
                                    checked={selectedConditions.includes(1)}
                                    onCheckedChange={() => handleConditionToggle(1)}
                                />
                                <label htmlFor="cond-1" className="checkbox-label">
                                    全新
                                </label>
                                <span className="condition-icon">✨</span>
                            </div>
                            <div className="filter-checkbox">
                                <Checkbox
                                    id="cond-2"
                                    checked={selectedConditions.includes(2)}
                                    onCheckedChange={() => handleConditionToggle(2)}
                                />
                                <label htmlFor="cond-2" className="checkbox-label">
                                    几乎全新
                                </label>
                                <span className="condition-icon">🌟</span>
                            </div>
                            <div className="filter-checkbox">
                                <Checkbox
                                    id="cond-3"
                                    checked={selectedConditions.includes(3)}
                                    onCheckedChange={() => handleConditionToggle(3)}
                                />
                                <label htmlFor="cond-3" className="checkbox-label">
                                    轻微使用
                                </label>
                                <span className="condition-icon">💫</span>
                            </div>
                            <div className="filter-checkbox">
                                <Checkbox
                                    id="cond-4"
                                    checked={selectedConditions.includes(4)}
                                    onCheckedChange={() => handleConditionToggle(4)}
                                />
                                <label htmlFor="cond-4" className="checkbox-label">
                                    明显使用
                                </label>
                                <span className="condition-icon">⭐</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="filter-footer">
                    <Button variant="ghost" onClick={handleResetFilters}>
                        重置
                    </Button>
                    <Button variant="outline" onClick={handleApplyFilters}>
                        应用筛选
                    </Button>
                </div>
            </aside>
        </>
    );
}
