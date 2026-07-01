import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui';

export type SortOption = 'newest' | 'price_asc' | 'price_desc' | 'popular';

interface SortDropdownProps {
    value: SortOption;
    onChange: (value: SortOption) => void;
}

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
    { value: 'newest', label: '最新发布' },
    { value: 'price_asc', label: '价格从低到高' },
    { value: 'price_desc', label: '价格从高到低' },
    { value: 'popular', label: '最受欢迎' },
];

export default function SortDropdown({ value, onChange }: SortDropdownProps) {
    return (
        <Select value={value} onValueChange={v => onChange(v as SortOption)}>
            <SelectTrigger className="h-auto w-auto gap-2 rounded-xl border-primary-500/15 bg-white/85 px-5 py-2.5 text-sm font-bold text-primary-600 backdrop-blur-xl transition-all hover:-translate-y-0.5 hover:border-primary-500/30 hover:bg-primary-500/8 hover:shadow-[0_4px_16px_rgba(249,115,22,0.15)]">
                <SelectValue placeholder="排序方式" />
            </SelectTrigger>
            <SelectContent className="rounded-2xl border-primary-500/10 bg-white/97 backdrop-blur-xl shadow-xl">
                {SORT_OPTIONS.map(option => (
                    <SelectItem
                        key={option.value}
                        value={option.value}
                        className="rounded-lg px-3 py-2.5 text-[0.8125rem] font-semibold text-[var(--text-secondary)] focus:bg-primary-500/6 focus:text-primary-600 data-[state=checked]:bg-gradient-to-br data-[state=checked]:from-primary-500/10 data-[state=checked]:to-rose-400/6 data-[state=checked]:text-primary-600"
                    >
                        {option.label}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}
