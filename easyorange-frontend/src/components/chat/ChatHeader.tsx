import { ArrowLeft, MoreVertical } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface TargetUser {
    id: string;
    name: string;
    avatar: string | null;
}

interface ChatHeaderProps {
    targetUser?: TargetUser | null;
    onBack: () => void;
}

function ChatHeader({ targetUser, onBack }: ChatHeaderProps) {
    return (
        <header className="chat-header">
            <div className="chat-header-inner">
                <div className="flex items-center gap-3">
                    <Button variant="ghost" size="icon" onClick={onBack} className="chat-back-btn" aria-label="返回">
                        <ArrowLeft size={20} />
                    </Button>

                    {targetUser && (
                        <div className="flex items-center gap-3">
                            <div className="chat-avatar">
                                {targetUser.avatar ? (
                                    <img
                                        src={targetUser.avatar}
                                        alt={targetUser.name}
                                        className="w-full h-full object-cover"
                                    />
                                ) : (
                                    <span className="chat-avatar-text">{targetUser.name.charAt(0)}</span>
                                )}
                                <span className="chat-avatar-status" />
                            </div>
                            <div className="flex flex-col">
                                <span className="chat-header-name">{targetUser.name}</span>
                                <span className="chat-header-status">在线</span>
                            </div>
                        </div>
                    )}
                </div>

                <div className="flex items-center gap-1">
                    <Button variant="ghost" size="icon" className="chat-action-btn" aria-label="更多选项">
                        <MoreVertical size={18} />
                    </Button>
                </div>
            </div>
        </header>
    );
}

export default ChatHeader;
