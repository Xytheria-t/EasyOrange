import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui';

export type SortOption = 'relevance' | 'newest' | 'price_asc' | 'price_desc' | 'popular';

const SORT_LABELS: Record<SortOption, string> = {
    relevance: '最相关',
    newest: '最新发布',
    price_asc: '价格从低到高',
    price_desc: '价格从高到低',
    popular: '最受欢迎',
};

/** 默认全量排序项；商品列表页无「相关性」概念，传入自己的子集 */
const ALL_SORT_OPTIONS = Object.keys(SORT_LABELS) as SortOption[];

interface SortDropdownProps {
    value: SortOption;
    onChange: (value: SortOption) => void;
    options?: SortOption[];
}

export default function SortDropdown({ value, onChange, options = ALL_SORT_OPTIONS }: SortDropdownProps) {
    return (
        <Select value={value} onValueChange={v => onChange(v as SortOption)}>
            <SelectTrigger className="h-auto w-auto gap-2 rounded-xl border-primary-500/15 bg-white/85 px-5 py-2.5 text-sm font-bold text-primary-600 backdrop-blur-xl transition-all hover:-translate-y-0.5 hover:border-primary-500/30 hover:bg-primary-500/8 hover:shadow-[0_4px_16px_rgba(249,115,22,0.15)]">
                <SelectValue placeholder="排序方式" />
            </SelectTrigger>
            <SelectContent className="rounded-2xl border-primary-500/10 bg-white/97 backdrop-blur-xl shadow-xl">
                {options.map(option => (
                    <SelectItem
                        key={option}
                        value={option}
                        className="rounded-lg px-3 py-2.5 text-[0.8125rem] font-semibold text-[var(--text-secondary)] focus:bg-primary-500/6 focus:text-primary-600 data-[state=checked]:bg-gradient-to-br data-[state=checked]:from-primary-500/10 data-[state=checked]:to-rose-400/6 data-[state=checked]:text-primary-600"
                    >
                        {SORT_LABELS[option]}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}
