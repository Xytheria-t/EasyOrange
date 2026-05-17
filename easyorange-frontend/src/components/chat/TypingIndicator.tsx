interface TypingIndicatorProps {
  userName: string
  visible: boolean
}

function TypingIndicator({ userName, visible }: TypingIndicatorProps) {
  if (!visible) {return null}

  return (
    <div className="flex justify-start mb-3">
      <div className="flex items-center gap-1.5 text-xs text-gray-400">
        <span>{userName} 正在输入</span>
        <span className="flex items-center gap-0.5">
          <span className="w-1 h-1 bg-gray-400 rounded-full animate-bounce [animation-delay:0ms]" />
          <span className="w-1 h-1 bg-gray-400 rounded-full animate-bounce [animation-delay:150ms]" />
          <span className="w-1 h-1 bg-gray-400 rounded-full animate-bounce [animation-delay:300ms]" />
        </span>
      </div>
    </div>
  )
}

export default TypingIndicator
