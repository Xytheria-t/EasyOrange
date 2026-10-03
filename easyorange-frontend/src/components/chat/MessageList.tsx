import { useVirtualizer } from '@tanstack/react-virtual';
import { forwardRef, useCallback, useEffect, useRef } from 'react';
import { Button } from '@/components/ui/button';
import type { ChatMessage } from '@/types/message';
import MessageBubble from './MessageBubble';
import TypingIndicator from './TypingIndicator';

interface MessageListProps {
    messages: ChatMessage[];
    currentUserId: string;
    targetUserName: string;
    isTyping: boolean;
    onLoadMore?: () => void | Promise<void>;
    hasMore?: boolean;
    onRecall?: (messageId: string) => Promise<boolean>;
    canRecallFn?: (message: ChatMessage) => boolean;
}

function formatMessageDate(timeString: string): string {
    const date = new Date(timeString);
    const now = new Date();
    const isToday =
        date.getDate() === now.getDate() &&
        date.getMonth() === now.getMonth() &&
        date.getFullYear() === now.getFullYear();

    if (isToday) {
        return '今天';
    }

    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    const isYesterday =
        date.getDate() === yesterday.getDate() &&
        date.getMonth() === yesterday.getMonth() &&
        date.getFullYear() === now.getFullYear();

    if (isYesterday) {
        return '昨天';
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}年${month}月${day}日`;
}

function shouldShowDateSeparator(index: number, messages: ChatMessage[]): boolean {
    if (index === 0) {
        return true;
    }
    const current = new Date(messages[index].createTime).toDateString();
    const prev = new Date(messages[index - 1].createTime).toDateString();
    return current !== prev;
}

const ESTIMATED_ITEM_HEIGHT = 72;

const MessageList = forwardRef<HTMLDivElement, MessageListProps>(
    ({ messages, currentUserId, targetUserName, isTyping, onLoadMore, hasMore, onRecall, canRecallFn }, ref) => {
        const scrollContainerRef = useRef<HTMLDivElement>(null);
        const prevMessagesLengthRef = useRef(messages.length);
        const isLoadingMoreRef = useRef(false);

        const virtualizer = useVirtualizer({
            count: messages.length + (hasMore ? 1 : 0) + 1,
            getScrollElement: () => scrollContainerRef.current,
            estimateSize: () => ESTIMATED_ITEM_HEIGHT,
            overscan: 5,
        });

        const scrollToBottom = useCallback(
            (smooth = false) => {
                virtualizer.scrollToIndex(messages.length - 1, { align: 'end', behavior: smooth ? 'smooth' : 'auto' });
            },
            [virtualizer, messages.length]
        );

        useEffect(() => {
            if (messages.length > prevMessagesLengthRef.current) {
                scrollToBottom(true);
            }
            prevMessagesLengthRef.current = messages.length;
        }, [messages.length, scrollToBottom]);

        const handleScroll = useCallback(() => {
            if (!hasMore || !onLoadMore || isLoadingMoreRef.current) {
                return;
            }
            if (virtualizer.scrollOffset != null && virtualizer.scrollOffset < 100) {
                isLoadingMoreRef.current = true;
                // 闸门跟请求生命周期走：此前用 requestAnimationFrame 释放，下一帧就解锁而请求还在飞，
                // 顶部连续滚动会并发触发 onLoadMore
                Promise.resolve(onLoadMore()).finally(() => {
                    isLoadingMoreRef.current = false;
                });
            }
        }, [hasMore, onLoadMore, virtualizer]);

        useEffect(() => {
            const el = scrollContainerRef.current;
            if (!el) {
                return;
            }
            el.addEventListener('scroll', handleScroll, { passive: true });
            return () => el.removeEventListener('scroll', handleScroll);
        }, [handleScroll]);

        const setRefs = useCallback(
            (node: HTMLDivElement | null) => {
                (scrollContainerRef as React.MutableRefObject<HTMLDivElement | null>).current = node;
                if (typeof ref === 'function') {
                    ref(node);
                } else if (ref) {
                    (ref as React.MutableRefObject<HTMLDivElement | null>).current = node;
                }
            },
            [ref]
        );

        const virtualItems = virtualizer.getVirtualItems();

        return (
            // role="region" 而非 role="log"：虚拟滚动随滚动不断增删 DOM 行，live region
            // 会把滚过的每条消息都念一遍。tabIndex 是为了让键盘也能滚长会话——
            // 可滚动但不可聚焦的容器，键盘用户永远够不到上方的历史消息。
            // biome-ignore lint/a11y/noNoninteractiveTabindex: scrollable region must be focusable
            // biome-ignore lint/a11y/useSemanticElements: 语义等价于 <section>，此处沿用 div 以复用虚拟滚动的 ref
            <div ref={setRefs} className="chat-scroll" role="region" aria-label="消息列表" tabIndex={0}>
                <div style={{ height: `${virtualizer.getTotalSize()}px`, width: '100%', position: 'relative' }}>
                    {virtualItems.map(virtualItem => {
                        const isLoadMoreRow = hasMore && virtualItem.index === 0;
                        const isTypingRow = virtualItem.index === messages.length + (hasMore ? 1 : 0);

                        if (isLoadMoreRow) {
                            // 定位交给外层绝对定位容器：直接给按钮 translateY 会让它叠在第一条消息上
                            return (
                                <div
                                    key="load-more"
                                    className="absolute left-0 w-full"
                                    style={{ transform: `translateY(${virtualItem.start}px)` }}
                                >
                                    <Button
                                        type="button"
                                        variant="outline"
                                        onClick={onLoadMore}
                                        className="chat-load-more"
                                    >
                                        加载更多消息
                                    </Button>
                                </div>
                            );
                        }

                        if (isTypingRow) {
                            return (
                                <div
                                    key="typing-indicator"
                                    className="absolute left-0 w-full"
                                    style={{ transform: `translateY(${virtualItem.start}px)` }}
                                >
                                    <TypingIndicator userName={targetUserName} isVisible={isTyping} />
                                </div>
                            );
                        }

                        const message = messages[virtualItem.index - (hasMore ? 1 : 0)];
                        const showDateSeparator = shouldShowDateSeparator(
                            virtualItem.index - (hasMore ? 1 : 0),
                            messages
                        );

                        return (
                            <div
                                key={message.id}
                                className="absolute left-0 w-full"
                                style={{
                                    transform: `translateY(${virtualItem.start}px)`,
                                }}
                            >
                                {showDateSeparator && (
                                    <div className="chat-date-separator">
                                        <span>{formatMessageDate(message.createTime)}</span>
                                    </div>
                                )}
                                <MessageBubble
                                    message={message}
                                    isOwn={message.senderId === currentUserId}
                                    onRecall={onRecall}
                                    canRecallFn={canRecallFn}
                                />
                            </div>
                        );
                    })}
                </div>
            </div>
        );
    }
);

MessageList.displayName = 'MessageList';
export default MessageList;
