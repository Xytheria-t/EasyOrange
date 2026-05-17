import { useState, useRef, useEffect, useCallback } from 'react'
import { Copy, RotateCcw } from 'lucide-react'
import { ChatMessage } from '@/types/message'

interface MessageBubbleProps {
  message: ChatMessage
  isOwn: boolean
  onRecall?: (messageId: string) => Promise<boolean>
  canRecallFn?: (message: ChatMessage) => boolean
}

function formatTime(timeStr: string): string {
  const date = new Date(timeStr)
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

function MessageBubble({
  message,
  isOwn,
  onRecall,
  canRecallFn,
}: MessageBubbleProps) {
  const isRecalled = message.type === 'RECALLED' || message.status === 'RECALLED'
  const [menuVisible, setMenuVisible] = useState(false)
  const [menuPos, setMenuPos] = useState({ x: 0, y: 0 })
  const menuRef = useRef<HTMLDivElement>(null)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const showMenu = useCallback(
    (e: React.TouchEvent | React.MouseEvent) => {
      e.preventDefault()
      if (isRecalled) {return}

      let x: number, y: number
      if ('touches' in e) {
        x = e.touches[0].clientX
        y = e.touches[0].clientY
      } else {
        x = e.clientX
        y = e.clientY
      }

      setMenuPos({ x, y })
      setMenuVisible(true)
    },
    [isRecalled]
  )

  const hideMenu = useCallback(() => {
    setMenuVisible(false)
  }, [])

  useEffect(() => {
    if (!menuVisible) {return}
    const handleClickOutside = (ev: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(ev.target as Node)) {
        hideMenu()
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [menuVisible, hideMenu])

  const handleTouchStart = useCallback(
    (e: React.TouchEvent) => {
      timerRef.current = setTimeout(() => {
        showMenu(e)
      }, 500)
    },
    [showMenu]
  )

  const handleTouchEnd = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const handleCopy = () => {
    navigator.clipboard.writeText(message.content).catch(console.error)
    hideMenu()
  }

  const handleRecall = async () => {
    if (onRecall) {
      await onRecall(message.id)
    }
    hideMenu()
  }

  const canRecallThis =
    isOwn &&
    !isRecalled &&
    (canRecallFn ? canRecallFn(message) : false)

  return (
    <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'} mb-3`}>
      <div className={`max-w-[70%] ${isOwn ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
        <div
          className={`relative px-3 py-2 rounded-2xl text-sm leading-relaxed break-words whitespace-pre-wrap cursor-default select-none ${
            isOwn
              ? 'bg-orange-500 text-white rounded-br-md'
              : 'bg-gray-100 text-gray-900 rounded-bl-md'
          }`}
          onTouchStart={handleTouchStart}
          onTouchEnd={handleTouchEnd}
          onContextMenu={showMenu}
        >
          {isRecalled ? (
            <span className="italic text-gray-400">[消息已撤回]</span>
          ) : (
            <span>{message.content}</span>
          )}

          <div className={`flex items-center gap-1.5 mt-1 ${isOwn ? 'justify-end' : 'justify-start'}`}>
            <span className={`text-[10px] ${isOwn ? 'text-orange-100' : 'text-gray-400'}`}>
              {formatTime(message.createTime)}
            </span>

            {isOwn && !isRecalled && (
              <>
                {message.status === 'SENDING' && (
                  <div className="flex items-center gap-0.5">
                    <span className="w-1 h-1 bg-orange-200 rounded-full animate-bounce [animation-delay:0ms]" />
                    <span className="w-1 h-1 bg-orange-200 rounded-full animate-bounce [animation-delay:150ms]" />
                    <span className="w-1 h-1 bg-orange-200 rounded-full animate-bounce [animation-delay:300ms]" />
                  </div>
                )}

                {message.status === 'FAILED' && (
                  <svg className="w-3.5 h-3.5 text-red-400" viewBox="0 0 16 16" fill="currentColor">
                    <circle cx="8" cy="8" r="7" fill="currentColor" opacity="0.15" />
                    <path d="M8 4v5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                    <circle cx="8" cy="11" r="0.75" fill="currentColor" />
                  </svg>
                )}

                {(message.status === 'SENT' || message.status === 'DELIVERED') && (
                  <svg className="w-3 h-3 text-orange-200" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M2 6l2.5 2.5L10 4" />
                  </svg>
                )}

                {message.status === 'READ' && (
                  <div className="flex items-center -space-x-0.5">
                    <svg className="w-3 h-3 text-orange-200" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M2 6l2.5 2.5L10 4" />
                    </svg>
                    <svg className="w-3 h-3 text-orange-200" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M2 6l2.5 2.5L10 4" />
                    </svg>
                  </div>
                )}
              </>
            )}
          </div>
        </div>

        {menuVisible && (
          <div
            ref={menuRef}
            className="fixed z-50 bg-white rounded-lg shadow-lg border border-gray-200 py-1 min-w-[120px]"
            style={{ left: menuPos.x, top: menuPos.y }}
          >
            <button
              onClick={handleCopy}
              className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
            >
              <Copy size={14} />
              复制
            </button>
            {canRecallThis && (
              <button
                onClick={handleRecall}
                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-red-500 hover:bg-red-50 transition-colors"
              >
                <RotateCcw size={14} />
                撤回
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

export default MessageBubble
