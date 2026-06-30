import { useEffect, useRef, useCallback, forwardRef } from 'react'
import { useVirtualizer } from '@tanstack/react-virtual'
import { Button } from '@/components/ui/button'
import type { ChatMessage } from '@/types/message'
import MessageBubble from './MessageBubble'
import TypingIndicator from './TypingIndicator'

interface MessageListProps {
  messages: ChatMessage[]
  currentUserId: string
  targetUserName: string
  isTyping: boolean
  onLoadMore?: () => void
  hasMore?: boolean
  onRecall?: (messageId: string) => Promise<boolean>
  canRecallFn?: (message: ChatMessage) => boolean
}

function formatMessageDate(timeString: string): string {
  const date = new Date(timeString)
  const now = new Date()
  const isToday =
    date.getDate() === now.getDate() &&
    date.getMonth() === now.getMonth() &&
    date.getFullYear() === now.getFullYear()

  if (isToday) {return '今天'}

  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  const isYesterday =
    date.getDate() === yesterday.getDate() &&
    date.getMonth() === yesterday.getMonth() &&
    date.getFullYear() === now.getFullYear()

  if (isYesterday) {return '昨天'}

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}年${month}月${day}日`
}

function shouldShowDateSeparator(index: number, messages: ChatMessage[]): boolean {
  if (index === 0) {return true}
  const current = new Date(messages[index].createTime).toDateString()
  const prev = new Date(messages[index - 1].createTime).toDateString()
  return current !== prev
}

const ESTIMATED_ITEM_HEIGHT = 72

const MessageList = forwardRef<HTMLDivElement, MessageListProps>(
  ({ messages, currentUserId, targetUserName, isTyping, onLoadMore, hasMore, onRecall, canRecallFn }, ref) => {
    const scrollContainerRef = useRef<HTMLDivElement>(null)
    const prevMessagesLengthRef = useRef(messages.length)
    const isLoadingMoreRef = useRef(false)

    const virtualizer = useVirtualizer({
      count: messages.length + (hasMore ? 1 : 0) + 1,
      getScrollElement: () => scrollContainerRef.current,
      estimateSize: () => ESTIMATED_ITEM_HEIGHT,
      overscan: 5,
    })

    const scrollToBottom = useCallback((smooth = false) => {
      virtualizer.scrollToIndex(messages.length - 1, { align: 'end', behavior: smooth ? 'smooth' : 'auto' })
    }, [virtualizer, messages.length])

    useEffect(() => {
      if (messages.length > prevMessagesLengthRef.current) {
        scrollToBottom(true)
      }
      prevMessagesLengthRef.current = messages.length
    }, [messages.length, scrollToBottom])

    const handleScroll = useCallback(() => {
      if (!hasMore || !onLoadMore || isLoadingMoreRef.current) {return}
      if (virtualizer.scrollOffset != null && virtualizer.scrollOffset < 100) {
        isLoadingMoreRef.current = true
        onLoadMore()
        requestAnimationFrame(() => {
          isLoadingMoreRef.current = false
        })
      }
    }, [hasMore, onLoadMore, virtualizer])

    useEffect(() => {
      const el = scrollContainerRef.current
      if (!el) {return}
      el.addEventListener('scroll', handleScroll, { passive: true })
      return () => el.removeEventListener('scroll', handleScroll)
    }, [handleScroll])

    const setRefs = useCallback(
      (node: HTMLDivElement | null) => {
        (scrollContainerRef as React.MutableRefObject<HTMLDivElement | null>).current = node
        if (typeof ref === 'function') {
          ref(node)
        } else if (ref) {
          (ref as React.MutableRefObject<HTMLDivElement | null>).current = node
        }
      },
      [ref],
    )

    const virtualItems = virtualizer.getVirtualItems()

    return (
      <div ref={setRefs} style={{ overflow: 'auto', height: '100%', minHeight: 0 }}>
        <div style={{ height: `${virtualizer.getTotalSize()}px`, width: '100%', position: 'relative' }}>
          {virtualItems.map((virtualItem) => {
            const isLoadMoreRow = hasMore && virtualItem.index === 0
            const isTypingRow = virtualItem.index === messages.length + (hasMore ? 1 : 0)

            if (isLoadMoreRow) {
              return (
                <Button
                  key="load-more"
                  type="button"
                  variant="outline"
                  onClick={onLoadMore}
                  className="chat-load-more"
                  style={{ transform: `translateY(${virtualItem.start}px)` }}
                >
                  加载更多消息
                </Button>
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
              )
            }

            const message = messages[virtualItem.index - (hasMore ? 1 : 0)]
            const showDateSeparator = shouldShowDateSeparator(virtualItem.index - (hasMore ? 1 : 0), messages)

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
            )
          })}
        </div>
      </div>
    )
  },
)

MessageList.displayName = 'MessageList'
export default MessageList
