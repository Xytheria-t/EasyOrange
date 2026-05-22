import { useRef, useState, useCallback, useEffect } from 'react'
import { Send, Paperclip, Smile } from 'lucide-react'

interface ChatInputBarProps {
  onSend: (content: string) => void
  onTyping: () => void
  isDisabled?: boolean
}

function ChatInputBar({ onSend, onTyping, isDisabled = false }: ChatInputBarProps) {
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
    if (!trimmed || isDisabled) {return}
    onSend(trimmed)
    setValue('')
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
    }
  }

  return (
    <div className="chat-input-bar">
      <div className="chat-input-bar-inner">
        <button
          className="chat-input-action-btn"
          aria-label="附件"
          disabled={isDisabled}
        >
          <Paperclip size={20} />
        </button>

        <div className="chat-input-wrapper">
          <textarea
            ref={textareaRef}
            value={value}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            disabled={isDisabled}
            rows={1}
            placeholder={isDisabled ? '' : '输入消息...'}
            className="chat-textarea"
            style={{ maxHeight: 120 }}
          />
        </div>

        <button
          className="chat-input-action-btn"
          aria-label="表情"
          disabled={isDisabled}
        >
          <Smile size={20} />
        </button>

        <button
          onClick={handleSubmit}
          disabled={!value.trim() || isDisabled}
          className="chat-send-btn"
          aria-label="发送"
        >
          <Send size={18} className="-rotate-[15deg] translate-x-[1px]" />
        </button>
      </div>
    </div>
  )
}

export default ChatInputBar
