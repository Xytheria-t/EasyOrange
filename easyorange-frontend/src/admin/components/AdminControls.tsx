import { Search } from 'lucide-react';
import { type ReactNode, useId } from 'react';
import { Button } from '@/components/ui/button';
import { AdminSelect } from './AdminSelect';

/**
 * 工具栏控件 —— 收敛此前 4 个页面逐字复制的搜索框与筛选下拉。
 * 关键修正：筛选项不再是「悬空 span + 无名控件」，统一由 `<label htmlFor>` 关联。
 */

export function AdminToolbar({ children }: { children: ReactNode }) {
    return <div className="admin-toolbar">{children}</div>;
}

interface AdminSearchInputProps {
    value: string;
    onChange: (value: string) => void;
    /** 实时筛选（submitOnEnterOnly）可不传：此时 Enter 与提交按钮都没有语义 */
    onSubmit?: () => void;
    placeholder?: string;
    /** 请求进行中：禁用输入与提交，避免连点打出并发请求 */
    loading?: boolean;
    /** 隐藏提交按钮时改为失焦即搜（如「分类管理」树内搜索） */
    submitOnEnterOnly?: boolean;
}

/** 搜索框 + 提交按钮。输入框自带可访问名，不再只靠 placeholder。 */
export function AdminSearchInput({
    value,
    onChange,
    onSubmit,
    placeholder = '搜索…',
    loading = false,
    submitOnEnterOnly = false,
}: AdminSearchInputProps) {
    const inputId = useId();

    return (
        <div className="admin-search">
            <div style={{ flex: 1, minWidth: 0 }}>
                <label htmlFor={inputId} className="admin-label" style={{ display: 'block' }}>
                    关键词
                </label>
                {/* 图标单独包一层：放在 label 同级会让 50% 居中落到 label+输入框的中点 */}
                <div style={{ position: 'relative' }}>
                    <Search
                        size={15}
                        aria-hidden="true"
                        style={{
                            position: 'absolute',
                            left: '0.85rem',
                            top: '50%',
                            transform: 'translateY(-50%)',
                            color: 'var(--admin-faint)',
                            pointerEvents: 'none',
                        }}
                    />
                    <input
                        id={inputId}
                        type="search"
                        value={value}
                        disabled={loading}
                        placeholder={placeholder}
                        onChange={e => onChange(e.target.value)}
                        onKeyDown={e => {
                            if (e.key === 'Enter' && onSubmit) {
                                e.preventDefault();
                                onSubmit();
                            }
                        }}
                        className="admin-input"
                        style={{ paddingLeft: '2.4rem' }}
                    />
                </div>
            </div>
            {submitOnEnterOnly || !onSubmit ? null : (
                <Button
                    type="button"
                    onClick={() => onSubmit()}
                    disabled={loading}
                    isLoading={loading}
                    loadingText="搜索中"
                    className="self-end"
                >
                    搜索
                </Button>
            )}
        </div>
    );
}

interface AdminFilterFieldProps {
    label: string;
    options: { value: string | number; label: string }[];
    value: string | number | undefined;
    onChange: (value: string) => void;
    placeholder?: string;
    minWidth?: number | string;
    disabled?: boolean;
}

/** 带可见 label 的筛选下拉。label 与 `AdminSelect` 触发器通过 id 关联。 */
export function AdminFilterField({
    label,
    options,
    value,
    onChange,
    placeholder = '全部',
    minWidth = 130,
    disabled = false,
}: AdminFilterFieldProps) {
    const selectId = useId();

    return (
        <div className="admin-filter">
            <label htmlFor={selectId} className="admin-label">
                {label}
            </label>
            <AdminSelect
                id={selectId}
                options={options}
                value={value}
                onChange={onChange}
                placeholder={placeholder}
                minWidth={minWidth}
                disabled={disabled}
            />
        </div>
    );
}

interface AdminFieldProps {
    label: string;
    required?: boolean;
    error?: string;
    hint?: string;
    children: (props: { id: string; 'aria-invalid': boolean; 'aria-describedby'?: string }) => ReactNode;
}

/** 表单字段：label + 控件 + 就地错误。错误不只冒在顶部。 */
export function AdminField({ label, required = false, error, hint, children }: AdminFieldProps) {
    const id = useId();
    const messageId = `${id}-msg`;

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            <label
                htmlFor={id}
                className="admin-label"
                style={{ display: 'flex', gap: '0.3rem', alignItems: 'center' }}
            >
                {label}
                {required ? (
                    <span aria-hidden="true" style={{ color: 'var(--admin-danger)' }}>
                        *
                    </span>
                ) : null}
            </label>
            {children({
                id,
                'aria-invalid': Boolean(error),
                ...(error || hint ? { 'aria-describedby': messageId } : {}),
            })}
            {error || hint ? (
                <p
                    id={messageId}
                    style={{
                        margin: 0,
                        fontSize: '0.78rem',
                        color: error ? 'var(--admin-danger)' : 'var(--admin-muted)',
                    }}
                >
                    {error || hint}
                </p>
            ) : null}
        </div>
    );
}
