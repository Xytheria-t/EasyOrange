interface TypingIndicatorProps {
  userName: string
  isVisible: boolean
}

function TypingIndicator({ userName, isVisible }: TypingIndicatorProps) {
  if (!isVisible) {return null}

  return (
    <div className="flex justify-start mb-4">
      <div className="typing-indicator">
        <div className="typing-dots">
          <span className="typing-dot" />
          <span className="typing-dot" />
          <span className="typing-dot" />
        </div>
        <span className="typing-text">{userName} 正在输入</span>
      </div>
    </div>
  )
}

export default TypingIndicator
