import { X } from 'lucide-react';
import { useMemo } from 'react';
import { Button } from '@/components/ui/button';
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

function formatConditionLabel(value: string): string {
    const labels: Record<string, string> = {
        '1': '全新',
        '2': '几乎全新',
        '3': '轻微使用痕迹',
        '4': '明显使用痕迹',
        '5': '有缺陷',
    };
    return labels[value] ?? value;
}

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

function getItemLabel(group: string, value: string): string {
    switch (group) {
        case 'condition':
            return formatConditionLabel(value);
        case 'price':
            return formatPriceLabel(value);
        default:
            return value;
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
                label: getItemLabel(group, value),
                count: facet.count,
            });
        }

        return Array.from(map.values());
    }, [facets]);

    if (!facets || facets.length === 0) {
        return null;
    }

    return (
        <div className="w-full" role="group" aria-label="搜索结果过滤">
            {/* Mobile: stacked layout; sm+: horizontal wrapping */}
            <div className="flex flex-col sm:flex-row sm:flex-wrap gap-4 sm:gap-6">
                {groups.map(group => (
                    <div key={group.key} className="flex-1 sm:flex-initial min-w-[140px]">
                        {/* Group header */}
                        <div className="flex items-center justify-between mb-2">
                            <h4 className="text-sm font-semibold text-gray-700">{group.label}</h4>
                            {filters[group.key] && (
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => onFilterChange(group.key, null)}
                                    className="text-[11px] text-orange-500 hover:text-orange-600 font-medium transition-colors ml-2 shrink-0 h-auto min-h-0"
                                >
                                    清除
                                </Button>
                            )}
                        </div>

                        {/* Filter pills */}
                        <div className="flex flex-wrap gap-1.5">
                            {group.items.map(item => {
                                const isActive = filters[group.key] === item.value;
                                return (
                                    <Button
                                        key={item.value}
                                        type="button"
                                        variant="outline"
                                        size="sm"
                                        onClick={() => onFilterChange(group.key, isActive ? null : item.value)}
                                        className={`
                      inline-flex items-center gap-1 px-2.5 py-1.5 rounded-full text-xs font-medium
                      transition-all duration-200 border h-auto min-h-0
                      ${
                          isActive
                              ? 'bg-orange-50 text-orange-600 border-orange-200 shadow-sm'
                              : 'bg-white text-gray-600 border-gray-200 hover:border-orange-200 hover:text-orange-500 hover:bg-orange-50/50'
}
                    `}
                                        aria-pressed={isActive}
                                    >
                                        <span>{item.label}</span>
                                        <span
                                            className={`
                        text-[10px] leading-none px-1 py-0.5 rounded font-medium
                        ${isActive ? 'bg-orange-100 text-orange-500' : 'bg-gray-100 text-gray-400'}
                      `}
                                        >
                                            {item.count}
                                        </span>
                                        {isActive && <X size={10} className="text-orange-400 shrink-0" />}
                                    </Button>
                                );
                            })}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
