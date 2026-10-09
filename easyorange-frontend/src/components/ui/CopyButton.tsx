import { Check, Copy } from 'lucide-react';
import * as React from 'react';
import { cn } from '@/lib/utils';
import './copy-button.css';

/** 复制成功反馈的保持时长：够读一眼「已复制」又不挡后续操作 */
const RESET_DELAY = 2000;

/** clipboard API 不可用时（http 源 / 旧浏览器）的兜底：隐藏 textarea + execCommand */
function legacyCopy(text: string): boolean {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    let ok = false;
    try {
        ok = document.execCommand('copy');
    } catch {
        ok = false;
    }
    document.body.removeChild(textarea);
    return ok;
}

async function copyText(text: string): Promise<boolean> {
    if (navigator.clipboard?.writeText) {
        try {
            await navigator.clipboard.writeText(text);
            return true;
        } catch {
            // 用户拒绝授权 / 非安全上下文下少数浏览器仍抛错，走兜底而非直接失败
        }
    }
    return legacyCopy(text);
}

interface CopyButtonProps extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'value' | 'onCopy'> {
    /** 支持传函数以延迟求值（如复制当前 URL 而非渲染时的 URL） */
    value: string | (() => string);
    /** 空 = 图标按钮，调用方需自带 aria-label；传了就是带文案的按钮 */
    label?: string;
    /** 覆盖默认的复制/对勾图标对；传了就不做成功换图，反馈只走 aria-label 与文案 */
    icon?: React.ReactNode;
    variant?: 'ghost' | 'outline';
    size?: 'sm' | 'md';
    /** ok = 剪贴板是否写入成功；调用方据此给 toast，失败文案这里不替它编 */
    onCopy?: (text: string, ok: boolean) => void;
}

function CopyButton({
    value,
    label,
    icon,
    variant = 'ghost',
    size = 'md',
    className,
    onCopy,
    ...props
}: CopyButtonProps) {
    const [copied, setCopied] = React.useState(false);
    const timerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);

    React.useEffect(
        () => () => {
            if (timerRef.current) {
                clearTimeout(timerRef.current);
            }
        },
        []
    );

    const handleCopy = async () => {
        const text = typeof value === 'function' ? value() : value;
        const ok = await copyText(text);
        if (ok) {
            setCopied(true);
            if (timerRef.current) {
                clearTimeout(timerRef.current);
            }
            timerRef.current = setTimeout(() => setCopied(false), RESET_DELAY);
        }
        onCopy?.(text, ok);
    };

    return (
        <button
            type="button"
            className={cn('cb-root', `cb-${variant}`, `cb-size-${size}`, className)}
            data-copied={copied}
            onClick={handleCopy}
            aria-label={label ? undefined : copied ? '已复制' : '复制'}
            {...props}
        >
            {icon ??
                (copied ? (
                    <Check className="cb-icon" aria-hidden="true" />
                ) : (
                    <Copy className="cb-icon" aria-hidden="true" />
                ))}
            {label && <span className="cb-label">{copied ? '已复制' : label}</span>}
            {/* 反馈不只靠图标换色：屏幕阅读器需要一条独立播报 */}
            <span role="status" aria-live="polite" className="sr-only">
                {copied ? '已复制' : ''}
            </span>
        </button>
    );
}

export { CopyButton };
