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

function MessageBubble({ message, isOwn, onRecall, canRecallFn }: MessageBubbleProps) {
    const isRecalled = message.type === 'RECALLED' || message.status === 'RECALLED';
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

            setMenuPos({ x, y });
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
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
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
                <button
                    type="button"
                    className={`chat-bubble ${isOwn ? 'chat-bubble-own' : 'chat-bubble-other'} ${isRecalled ? 'chat-bubble-recalled' : ''}`}
                    onTouchStart={handleTouchStart}
                    onTouchEnd={handleTouchEnd}
                    onContextMenu={showMenu}
                    tabIndex={-1}
                    onKeyDown={() => {}}
                    aria-label={isRecalled ? '已撤回的消息' : `${isOwn ? '我' : '对方'}的消息：${message.content?.slice(0, 30) || ''}`}
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

                                {(message.status === 'SENT' || message.status === 'DELIVERED') && (
                                    <Check size={14} className="opacity-60" />
                                )}

                                {message.status === 'READ' && <CheckCheck size={14} className="opacity-80" />}
                            </>
                        )}
                    </div>
                </button>

                {menuVisible && (
                    <div
                        ref={menuRef}
                        className="fixed z-50 chat-context-menu"
                        style={{ left: menuPos.x, top: menuPos.y }}
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
