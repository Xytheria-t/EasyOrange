import { useQuery } from '@tanstack/react-query';
import { Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { notificationApi } from '@/api/notificationApi';
import { Badge } from '@/components/ui';
import { Button } from '@/components/ui/button';
import { useNotificationSocket } from '@/hooks/useNotificationSocket';

export function NotificationBell() {
    const navigate = useNavigate();
    useNotificationSocket();

    const { data: unreadCount } = useQuery({
        queryKey: ['unread-count'],
        queryFn: async () => {
            const response = await notificationApi.getUnreadCount();
            return response.data;
        },
        refetchInterval: 60_000,
        staleTime: 15_000,
    });

    const count = unreadCount?.systemCount ?? 0;

    return (
        // 徽标挂在按钮外侧：icon-btn 为了流光动效带了 overflow:hidden,放按钮内会被裁掉
        <div className="floating-nav__bell">
            <Button
                variant="ghost"
                size="icon"
                onClick={() => navigate('/notifications')}
                aria-label="通知"
                className="floating-nav__icon-btn"
            >
                <Bell size={19} />
            </Button>
            {count > 0 && (
                <Badge
                    variant="destructive"
                    className="floating-nav__bell-badge absolute -right-1 -top-1 h-5 min-w-5 px-1.5 text-[0.65rem]"
                >
                    {count > 99 ? '99+' : count}
                </Badge>
            )}
        </div>
    );
}
