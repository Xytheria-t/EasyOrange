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

/** 同一个人连着发的并成一组：超过这个间隔就另起一组，避免连发时糊成一条长墙 */
const GROUP_GAP_MS = 5 * 60 * 1000;

function startsGroup(index: number, messages: ChatMessage[]): boolean {
    if (index === 0) {
        return true;
    }
    const prev = messages[index - 1];
    const current = messages[index];
    if (prev.senderId !== current.senderId) {
        return true;
    }
    return new Date(current.createTime).getTime() - new Date(prev.createTime).getTime() > GROUP_GAP_MS;
}

const ESTIMATED_ITEM_HEIGHT = 72;

/** 距底部小于该值即视为「贴底」，新消息到达时继续跟随 */
const PIN_THRESHOLD_PX = 48;

const MessageList = forwardRef<HTMLDivElement, MessageListProps>(
    ({ messages, currentUserId, targetUserName, isTyping, onLoadMore, hasMore, onRecall, canRecallFn }, ref) => {
        const scrollContainerRef = useRef<HTMLDivElement>(null);
        const isLoadingMoreRef = useRef(false);
        /** 用户是否还贴在底部：翻历史时必须为 false，否则会被一次次拽回最新 */
        const isPinnedRef = useRef(true);

        const virtualizer = useVirtualizer({
            count: messages.length + (hasMore ? 1 : 0) + 1,
            getScrollElement: () => scrollContainerRef.current,
            estimateSize: () => ESTIMATED_ITEM_HEIGHT,
            overscan: 5,
        });

        const totalSize = virtualizer.getTotalSize();

        // 逐条测高会持续改写总高度，只在消息数变化时按 index 对齐会停在半路，
        // 最后一条消息的时间戳被顶出可视区像被截断。贴底时直接压到底，
        // 且用瞬时定位——平滑动画会被随后的布局变动打断。
        // biome-ignore lint/correctness/useExhaustiveDependencies: totalSize / messages.length 是触发条件而非读取值——测高每改一次就该重新贴底
        useEffect(() => {
            const el = scrollContainerRef.current;
            if (el && isPinnedRef.current) {
                el.scrollTop = el.scrollHeight;
            }
        }, [totalSize, messages.length]);

        const handleScroll = useCallback(() => {
            const el = scrollContainerRef.current;
            if (el) {
                isPinnedRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < PIN_THRESHOLD_PX;
            }
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
                                    data-index={virtualItem.index}
                                    ref={virtualizer.measureElement}
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
                                    data-index={virtualItem.index}
                                    ref={virtualizer.measureElement}
                                    className="absolute left-0 w-full"
                                    style={{ transform: `translateY(${virtualItem.start}px)` }}
                                >
                                    <TypingIndicator userName={targetUserName} isVisible={isTyping} />
                                </div>
                            );
                        }

                        const messageIndex = virtualItem.index - (hasMore ? 1 : 0);
                        const message = messages[messageIndex];
                        const groupStart = startsGroup(messageIndex, messages);
                        const groupEnd =
                            messageIndex === messages.length - 1 || startsGroup(messageIndex + 1, messages);

                        return (
                            // measureElement 不可省：只给 estimateSize 的话每行都按 72px 排，
                            // 长消息与多行时间戳会直接压在下一条上（虚拟列表量不到真实高度）
                            <div
                                key={message.id}
                                data-index={virtualItem.index}
                                ref={virtualizer.measureElement}
                                className="absolute left-0 w-full"
                                style={{
                                    transform: `translateY(${virtualItem.start}px)`,
                                }}
                            >
                                {shouldShowDateSeparator(messageIndex, messages) && (
                                    <div className="chat-date-separator">
                                        <span>{formatMessageDate(message.createTime)}</span>
                                    </div>
                                )}
                                <MessageBubble
                                    message={message}
                                    isOwn={message.senderId === currentUserId}
                                    isGroupStart={groupStart}
                                    isGroupEnd={groupEnd}
                                    showAvatar={groupStart}
                                    senderName={targetUserName}
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
