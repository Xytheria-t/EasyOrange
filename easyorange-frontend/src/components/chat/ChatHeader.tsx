import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface TargetUser {
    id: string;
    name: string;
    avatar: string | null;
}

interface ChatHeaderProps {
    targetUser?: TargetUser | null;
    /** 不传即不渲染返回键（桌面双栏下列表常驻，没有可返回的对象） */
    onBack?: () => void;
    /** 对方正在输入——没有 presence 服务，这里只报真实观测到的状态 */
    isTyping?: boolean;
}

function ChatHeader({ targetUser, onBack, isTyping = false }: ChatHeaderProps) {
    return (
        <header className="chat-header">
            <div className="chat-header-inner">
                <div className="flex items-center gap-3">
                    {onBack && (
                        <Button
                            variant="ghost"
                            size="icon"
                            onClick={onBack}
                            className="chat-back-btn"
                            aria-label="返回会话列表"
                        >
                            <ArrowLeft size={20} />
                        </Button>
                    )}

                    {targetUser && (
                        <div className="flex items-center gap-3">
                            <div className="chat-avatar">
                                {targetUser.avatar ? (
                                    <img
                                        src={targetUser.avatar}
                                        alt={targetUser.name}
                                        className="w-full h-full object-cover"
                                        loading="lazy"
                                        decoding="async"
                                    />
                                ) : (
                                    <span className="chat-avatar-text">{targetUser.name.charAt(0)}</span>
                                )}
                            </div>
                            <div className="flex flex-col">
                                <span className="chat-header-name">{targetUser.name}</span>
                                {isTyping && (
                                    <span className="chat-header-status" role="status">
                                        正在输入
                                    </span>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
}

export default ChatHeader;
