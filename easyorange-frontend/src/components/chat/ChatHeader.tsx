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
    /** 副标题：会话性质（如系统通知只读）。不传时默认报「实时会话」 */
    subtitle?: string;
}

function ChatHeader({ targetUser, onBack, isTyping = false, subtitle }: ChatHeaderProps) {
    return (
        <header className="chat-header">
            <div className="chat-header-inner">
                {onBack && (
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={onBack}
                        className="chat-back-btn"
                        aria-label="返回会话列表"
                    >
                        <ArrowLeft size={19} />
                    </Button>
                )}

                {targetUser && (
                    <div className="chat-header-identity">
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
                            {!isTyping && <span className="chat-avatar-status" aria-hidden="true" />}
                        </div>
                        <div className="chat-header-meta">
                            <span className="chat-header-name">{targetUser.name}</span>
                            <span
                                className={isTyping ? 'chat-header-status' : 'chat-header-sub'}
                                role={isTyping ? 'status' : undefined}
                            >
                                {isTyping ? '正在输入…' : (subtitle ?? '实时会话')}
                            </span>
                        </div>
                    </div>
                )}
            </div>
        </header>
    );
}

export default ChatHeader;
