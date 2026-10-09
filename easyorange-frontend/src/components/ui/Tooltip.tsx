import * as React from 'react';
import { cn } from '@/lib/utils';
import './tooltip.css';

/*
 * 键盘模态跟踪：Tab / 方向键聚焦的按钮才值得弹 tooltip，鼠标点击时闪一下是噪声。
 * 用 :focus-visible 的 matches() 判据不行——jsdom 与旧浏览器对伪类的脚本求值口径不稳，
 * 一个可见性组件不该赌在环境差异上，所以自己记录最近一次输入方式。
 * 修饰键不翻模态：按住 Shift 再点鼠标仍是鼠标。
 */
const MODIFIER_KEYS = new Set(['Shift', 'Control', 'Alt', 'Meta', 'CapsLock']);
let keyboardMode = false;
if (typeof window !== 'undefined') {
    window.addEventListener(
        'keydown',
        (e: KeyboardEvent) => {
            if (!MODIFIER_KEYS.has(e.key)) {
                keyboardMode = true;
            }
        },
        { capture: true }
    );
    window.addEventListener('pointerdown', () => {
        keyboardMode = false;
    });
}

interface TooltipProps {
    /** 只给鼠标用户的视觉提示；图标按钮的无障碍名仍由 aria-label 承担 */
    content: string;
    placement?: 'top' | 'bottom';
    /** 包住页面原来的那颗按钮，不改变它的语义与事件 */
    children: React.ReactElement;
}

function Tooltip({ content, placement = 'top', children }: TooltipProps) {
    const [visible, setVisible] = React.useState(false);
    const id = React.useId();

    const anchor = React.cloneElement(children as React.ReactElement<Record<string, unknown>>, {
        'aria-describedby': visible ? id : undefined,
        onFocus: (e: React.FocusEvent) => {
            if (keyboardMode) {
                setVisible(true);
            }
            (children.props as { onFocus?: (e: React.FocusEvent) => void }).onFocus?.(e);
        },
        onBlur: (e: React.FocusEvent) => {
            setVisible(false);
            (children.props as { onBlur?: (e: React.FocusEvent) => void }).onBlur?.(e);
        },
    });

    return (
        // biome-ignore lint/a11y/noStaticElementInteractions: 悬停容器只是气泡的定位锚，交互仍由包住的按钮承接
        <span
            className="tt-anchor"
            onMouseOver={() => setVisible(true)}
            onMouseOut={() => setVisible(false)}
            onKeyDown={e => {
                if (e.key === 'Escape') {
                    setVisible(false);
                }
            }}
        >
            {anchor}
            <span id={id} role="tooltip" className={cn('tt-bubble', `tt-${placement}`)} hidden={!visible}>
                {content}
            </span>
        </span>
    );
}

export { Tooltip };
