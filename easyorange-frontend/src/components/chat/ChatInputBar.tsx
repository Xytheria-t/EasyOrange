import { useRef, useState, useCallback, useEffect } from 'react'
import { Send } from 'lucide-react'

interface ChatInputBarProps {
  onSend: (content: string) => void
  onTyping: () => void
  disabled?: boolean
}

function ChatInputBar({ onSend, onTyping, disabled = false }: ChatInputBarProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const [value, setValue] = useState('')
  const typingTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const adjustHeight = useCallback(() => {
    const el = textareaRef.current
    if (!el) {return}
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`
  }, [])

  const handleTypingDebounced = useCallback(() => {
    if (typingTimerRef.current) {
      clearTimeout(typingTimerRef.current)
    }
    onTyping()
    typingTimerRef.current = setTimeout(() => {
      typingTimerRef.current = null
    }, 2000)
  }, [onTyping])

  useEffect(() => {
    return () => {
      if (typingTimerRef.current) {
        clearTimeout(typingTimerRef.current)
      }
    }
  }, [])

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit()
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setValue(e.target.value)
    adjustHeight()
    if (e.target.value.trim()) {
      handleTypingDebounced()
    }
  }

  const handleSubmit = () => {
    const trimmed = value.trim()
    if (!trimmed || disabled) {return}
    onSend(trimmed)
    setValue('')
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
    }
  }

  return (
    <div className="flex items-end gap-2 px-4 py-3 border-t border-gray-200 bg-white">
      <textarea
        ref={textareaRef}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        rows={1}
        placeholder={disabled ? '' : '输入消息...'}
        className="flex-1 resize-none rounded-xl border border-gray-300 bg-gray-50 px-3 py-2.5 text-sm leading-relaxed text-gray-900 placeholder:text-gray-400 focus:border-orange-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        style={{ maxHeight: 120 }}
      />
      <button
        onClick={handleSubmit}
        disabled={!value.trim() || disabled}
        className="flex shrink-0 items-center justify-center w-10 h-10 rounded-full bg-orange-500 text-white transition-colors hover:bg-orange-600 active:bg-orange-700 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-orange-500"
        aria-label="发送"
      >
        <Send size={18} className="-rotate-[15deg] translate-x-[1px]" />
      </button>
    </div>
  )
}

export default ChatInputBar
