import { Check, CheckCheck, Copy, RotateCcw } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import type { ChatMessage } from '@/types/message';

interface MessageBubbleProps {
    message: ChatMessage;
    isOwn: boolean;
    onRecall?: (messageId: string) => Promise<boolean>;
    canRecallFn?: (message: ChatMessage) => boolean;
}

function formatTime(timeString: string): string {
    const date = new Date(timeString);
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
}

/** 菜单与视口边缘的最小间距 */
const MENU_VIEWPORT_GAP = 8;

function MessageBubble({ message, isOwn, onRecall, canRecallFn }: MessageBubbleProps) {
    // 撤回只由 status 表达：后端 MessageStatus 才有 RECALLED，type 没有这个取值
    const isRecalled = message.status === 'RECALLED';
    const [menuVisible, setMenuVisible] = useState(false);
    const [menuPos, setMenuPos] = useState({ x: 0, y: 0 });
    const menuRef = useRef<HTMLDivElement>(null);
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const showMenu = useCallback(
        (e: React.TouchEvent | React.MouseEvent) => {
            e.preventDefault();
            if (isRecalled) {
                return;
            }

            let x: number, y: number;
            if ('touches' in e) {
                x = e.touches[0].clientX;
                y = e.touches[0].clientY;
            } else {
                x = e.clientX;
                y = e.clientY;
            }

            //贴着指针开会在屏幕右/下缘被裁掉一半；按菜单实测尺寸往内收
            const { width, height } = menuRef.current?.getBoundingClientRect() ?? { width: 160, height: 96 };
            setMenuPos({
                x: Math.min(x, window.innerWidth - width - MENU_VIEWPORT_GAP),
                y: Math.min(y, window.innerHeight - height - MENU_VIEWPORT_GAP),
            });
            setMenuVisible(true);
        },
        [isRecalled]
    );

    const hideMenu = useCallback(() => {
        setMenuVisible(false);
    }, []);

    useEffect(() => {
        if (!menuVisible) {
            return;
        }
        const handleClickOutside = (ev: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(ev.target as Node)) {
                hideMenu();
            }
        };
        // Esc 要能关：右键弹出的浮层是纯鼠标可达，键盘用户没有别的退路
        const handleKeyDown = (ev: KeyboardEvent) => {
            if (ev.key === 'Escape') {
                hideMenu();
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        document.addEventListener('keydown', handleKeyDown);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            document.removeEventListener('keydown', handleKeyDown);
        };
    }, [menuVisible, hideMenu]);

    const handleTouchStart = useCallback(
        (e: React.TouchEvent) => {
            timerRef.current = setTimeout(() => {
                showMenu(e);
            }, 500);
        },
        [showMenu]
    );

    const handleTouchEnd = useCallback(() => {
        if (timerRef.current) {
            clearTimeout(timerRef.current);
            timerRef.current = null;
        }
    }, []);

    const handleCopy = () => {
        // 撤回后剪贴板里不该再出现原文
        if (isRecalled) {
            hideMenu();
            return;
        }
        navigator.clipboard.writeText(message.content).catch(() => {});
        hideMenu();
    };

    const handleRecall = async () => {
        if (onRecall) {
            await onRecall(message.id);
        }
        hideMenu();
    };

    const canRecallThis = isOwn && !isRecalled && (canRecallFn ? canRecallFn(message) : false);

    return (
        <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'} mb-4`}>
            <div className={`max-w-[75%] ${isOwn ? 'items-end' : 'items-start'} flex flex-col gap-1.5`}>
                <Button
                    type="button"
                    variant="ghost"
                    tabIndex={-1}
                    onTouchStart={handleTouchStart}
                    onTouchEnd={handleTouchEnd}
                    onContextMenu={showMenu}
                    onKeyDown={() => {}}
                    aria-label={
                        isRecalled
                            ? '已撤回的消息'
                            : `${isOwn ? '我' : '对方'}的消息：${message.content?.slice(0, 30) || ''}`
                    }
                    className={`chat-bubble ${isOwn ? 'chat-bubble-own' : 'chat-bubble-other'} ${
                        isRecalled ? 'chat-bubble-recalled' : ''
                    } font-normal tracking-normal text-left hover:bg-transparent`}
                >
                    {isRecalled ? (
                        <span className="italic opacity-60">[消息已撤回]</span>
                    ) : (
                        <span className="chat-bubble-text">{message.content}</span>
                    )}

                    <div className={`chat-bubble-meta ${isOwn ? 'justify-end' : 'justify-start'}`}>
                        <span className="chat-bubble-time">{formatTime(message.createTime)}</span>

                        {isOwn && !isRecalled && (
                            <>
                                {message.status === 'SENDING' && (
                                    <div className="flex items-center gap-0.5">
                                        <span className="w-1 h-1 bg-current rounded-full animate-bounce [animation-delay:0ms] opacity-40" />
                                        <span className="w-1 h-1 bg-current rounded-full animate-bounce [animation-delay:150ms] opacity-40" />
                                        <span className="w-1 h-1 bg-current rounded-full animate-bounce [animation-delay:300ms] opacity-40" />
                                    </div>
                                )}

                                {message.status === 'FAILED' && (
                                    <svg
                                        className="w-3.5 h-3.5 text-red-400"
                                        viewBox="0 0 16 16"
                                        fill="currentColor"
                                        aria-hidden="true"
                                    >
                                        <circle cx="8" cy="8" r="7" fill="currentColor" opacity="0.15" />
                                        <path
                                            d="M8 4v5"
                                            stroke="currentColor"
                                            strokeWidth="1.5"
                                            strokeLinecap="round"
                                        />
                                        <circle cx="8" cy="11" r="0.75" fill="currentColor" />
                                    </svg>
                                )}

                                {message.status === 'SENT' && <Check size={14} className="opacity-60" />}

                                {message.status === 'READ' && <CheckCheck size={14} className="opacity-80" />}
                            </>
                        )}
                    </div>
                </Button>

                {menuVisible && (
                    <div
                        ref={menuRef}
                        className="fixed z-50 chat-context-menu"
                        style={{ left: menuPos.x, top: menuPos.y }}
                        role="menu"
                        aria-label="消息操作"
                    >
                        <Button type="button" variant="ghost" onClick={handleCopy} className="chat-context-item">
                            <Copy size={14} />
                            复制
                        </Button>
                        {canRecallThis && (
                            <Button
                                type="button"
                                variant="ghost"
                                onClick={handleRecall}
                                className="chat-context-item chat-context-item-danger"
                            >
                                <RotateCcw size={14} />
                                撤回
                            </Button>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

export default MessageBubble;
