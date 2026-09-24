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
    /** 与可见 `<label>` 关联，缺了筛选控件对读屏就是无名的 */
    id?: string;
    disabled?: boolean;
    'aria-describedby'?: string;
    'aria-invalid'?: boolean;
}

export function AdminSelect({
    options,
    value,
    onChange,
    placeholder = '',
    minWidth,
    style,
    id,
    disabled = false,
    'aria-describedby': ariaDescribedBy,
    'aria-invalid': ariaInvalid,
}: AdminSelectProps) {
    const selectedLabel = options.find(o => String(o.value) === String(value))?.label;

    return (
        <div style={{ minWidth: minWidth ?? 110, ...style }}>
            <Select value={value !== undefined ? String(value) : ''} onValueChange={onChange} disabled={disabled}>
                <SelectTrigger
                    id={id}
                    aria-describedby={ariaDescribedBy}
                    aria-invalid={ariaInvalid}
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
