import { useQuery } from '@tanstack/react-query';
import { MessageCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { messageApi } from '@/api/messageApi';
import { Badge } from '@/components/ui';
import { Button } from '@/components/ui/button';
import type { ChatSession } from '@/types';

/**
 * 顶栏消息入口 —— 带会话未读汇总。
 *
 * <p>未读数直接由会话列表求和：铃铛管系统通知，这里管跟人的对话，两处各读自己那一份，
 * 不必为这个角标再发一次 unread-count 请求。会话列表由 WS 帧失效，数字随之刷新。
 */
export function MessagesEntry() {
    const navigate = useNavigate();

    const { data: conversations } = useQuery({
        queryKey: ['messages', 'conversations'],
        queryFn: async () => {
            const response = await messageApi.getConversations();
            return (response.data ?? []) as unknown as ChatSession[];
        },
        staleTime: 15 * 1000,
    });

    const unread = (conversations ?? []).reduce((sum, c) => sum + (c.unreadCount > 0 ? c.unreadCount : 0), 0);

    return (
        // 徽标挂在按钮外侧：icon-btn 为流光动效带了 overflow:hidden，放按钮内会被裁掉
        <div className="floating-nav__bell">
            <Button
                variant="ghost"
                size="icon"
                onClick={() => navigate('/messages')}
                aria-label={unread > 0 ? `消息，${unread} 条未读` : '消息'}
                className="floating-nav__icon-btn"
            >
                <MessageCircle size={19} />
            </Button>
            {unread > 0 && (
                <Badge
                    variant="destructive"
                    aria-hidden="true"
                    className="floating-nav__bell-badge absolute -right-1 -top-1 h-5 min-w-5 px-1.5 text-[0.65rem]"
                >
                    {unread > 99 ? '99+' : unread}
                </Badge>
            )}
        </div>
    );
}
