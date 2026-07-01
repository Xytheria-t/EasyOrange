import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui';
import { cn } from '@/lib/utils';

export interface AdminSelectOption {
    value: string | number;
    label: string;
}

export interface AdminSelectProps {
    options: AdminSelectOption[];
    value: string | number | undefined;
    onChange: (value: string) => void;
    placeholder?: string;
    minWidth?: number | string;
    style?: React.CSSProperties;
}

export function AdminSelect({ options, value, onChange, placeholder = '', minWidth, style }: AdminSelectProps) {
    const selectedLabel = options.find(o => String(o.value) === String(value))?.label;

    return (
        <div style={{ minWidth: minWidth ?? 110, ...style }}>
            <Select value={value !== undefined ? String(value) : ''} onValueChange={onChange}>
                <SelectTrigger
                    className={cn(
                        'h-9 w-full rounded-xl border-border bg-background px-3 text-sm font-medium text-foreground',
                        'hover:border-primary-400 focus:ring-primary-400/20 focus:border-primary-400',
                        !selectedLabel && 'text-muted-foreground'
                    )}
                >
                    <span className="truncate">{selectedLabel || placeholder}</span>
                </SelectTrigger>
                <SelectContent className="rounded-2xl">
                    {options.map(opt => (
                        <SelectItem key={opt.value} value={String(opt.value)} className="rounded-xl text-sm">
                            {opt.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
}
