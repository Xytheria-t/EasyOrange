import { SlidersHorizontal, X } from 'lucide-react';
import { useMemo } from 'react';
import { CONDITION_LABEL_MAP } from '@/constants';
import type { FacetBucket } from '@/types/product';

interface FacetFilterProps {
    facets: FacetBucket[];
    filters: Record<string, string>;
    onFilterChange: (key: string, value: string | null) => void;
}

interface FacetGroup {
    key: string;
    label: string;
    items: Array<{ value: string; label: string; count: number }>;
}

/**
 * Parse a facet code into its group key and value.
 *
 * Convention:
 *   - First underscore separates the group name from the value.
 *   - For price, format is `price_{min}_{max}`.
 *
 * Examples:
 *   "category_1"       → { group: "category", value: "1" }
 *   "condition_2"      → { group: "condition", value: "2" }
 *   "price_0_100"      → { group: "price",    value: "0_100" }
 */
function parseFacetCode(code: string): { group: string; value: string } {
    const idx = code.indexOf('_');
    if (idx === -1) {
        return { group: code, value: code };
    }
    return {
        group: code.substring(0, idx),
        value: code.substring(idx + 1),
    };
}

const GROUP_LABELS: Record<string, string> = {
    category: '分类',
    condition: '成色',
    price: '价格区间',
};

function formatPriceLabel(value: string): string {
    const parts = value.split('-');
    if (parts.length === 2) {
        const [min, max] = parts;
        if (min === '*' && max) {
            return `¥0 - ¥${max}`;
        }
        if (min && max === '*') {
            return `¥${min}+`;
        }
        if (min && max) {
            return `¥${min} - ¥${max}`;
        }
    }
    return value;
}

function getItemLabel(group: string, value: string, backendLabel?: string): string {
    switch (group) {
        case 'condition':
            return CONDITION_LABEL_MAP[Number(value)] ?? backendLabel ?? value;
        case 'price':
            return formatPriceLabel(value);
        default:
            // 分类等动态维度：后端聚合出展示名（类目名）时优先用它，原始 id 只作兜底
            return backendLabel && backendLabel !== value ? backendLabel : value;
    }
}

export default function FacetFilter({ facets, filters, onFilterChange }: FacetFilterProps) {
    const groups = useMemo(() => {
        const map = new Map<string, FacetGroup>();

        for (const facet of facets) {
            const { group, value } = parseFacetCode(facet.code);
            if (!map.has(group)) {
                map.set(group, {
                    key: group,
                    label: GROUP_LABELS[group] || group,
                    items: [],
                });
            }
            map.get(group)?.items.push({
                value,
                label: getItemLabel(group, value, facet.label),
                count: facet.count,
            });
        }

        return Array.from(map.values());
    }, [facets]);

    if (!facets || facets.length === 0) {
        return null;
    }

    const activeKeys = groups.filter(group => filters[group.key]).map(group => group.key);

    const handleClearAll = () => {
        activeKeys.forEach(key => {
            onFilterChange(key, null);
        });
    };

    return (
        <section className="facet-panel" aria-label="搜索结果过滤">
            <div className="facet-panel-header">
                <div className="facet-panel-icon">
                    <SlidersHorizontal size={14} />
                </div>
                <h3 className="facet-panel-title">结果筛选</h3>
                {activeKeys.length > 0 && (
                    <button type="button" className="facet-clear-all" onClick={handleClearAll}>
                        <X size={12} />
                        <span>清除全部</span>
                    </button>
                )}
            </div>

            <div className="facet-groups">
                {groups.map(group => (
                    <div key={group.key} className="facet-group">
                        <h4 className="facet-group-label">{group.label}</h4>
                        <div className="facet-chips">
                            {group.items.map(item => {
                                const isActive = filters[group.key] === item.value;
                                return (
                                    <button
                                        key={item.value}
                                        type="button"
                                        className={`facet-chip${isActive ? ' active' : ''}`}
                                        onClick={() => onFilterChange(group.key, isActive ? null : item.value)}
                                        aria-pressed={isActive}
                                    >
                                        <span>{item.label}</span>
                                        <span className="facet-chip-count">{item.count}</span>
                                        {isActive && <X size={12} className="facet-chip-x" aria-hidden="true" />}
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                ))}
            </div>
        </section>
    );
}
