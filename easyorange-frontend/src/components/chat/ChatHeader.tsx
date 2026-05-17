import { ArrowLeft } from 'lucide-react'

interface TargetUser {
  id: string
  name: string
  avatar: string | null
}

interface ChatHeaderProps {
  targetUser?: TargetUser | null
  onBack: () => void
}

function ChatHeader({ targetUser, onBack }: ChatHeaderProps) {
  return (
    <header className="flex items-center h-14 px-4 border-b border-gray-200 bg-white shadow-sm">
      <button
        onClick={onBack}
        className="flex items-center justify-center w-9 h-9 rounded-full hover:bg-gray-100 active:bg-gray-200 transition-colors"
        aria-label="返回"
      >
        <ArrowLeft size={20} className="text-gray-700" />
      </button>

      {targetUser && (
        <div className="flex items-center gap-3 ml-2">
          <div className="w-8 h-8 rounded-full bg-orange-500 flex items-center justify-center text-white text-sm font-medium overflow-hidden shrink-0">
            {targetUser.avatar ? (
              <img
                src={targetUser.avatar}
                alt={targetUser.name}
                className="w-full h-full object-cover"
              />
            ) : (
              targetUser.name.charAt(0)
            )}
          </div>
          <span className="text-sm font-medium text-gray-900 truncate max-w-[160px]">
            {targetUser.name}
          </span>
        </div>
      )}
    </header>
  )
}

export default ChatHeader
