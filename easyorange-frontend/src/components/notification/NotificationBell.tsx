import { useQuery } from '@tanstack/react-query';
import { Bell } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
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
    const label = count > 0 ? `通知，${count} 条未读` : '通知';

    // 只认「数变多」这一种变化：轮询把同一个数字再送回来不该惊动用户
    const seenCountRef = useRef(count);
    const [hasNew, setHasNew] = useState(false);
    useEffect(() => {
        if (count > seenCountRef.current) {
            setHasNew(true);
        }
        seenCountRef.current = count;
    }, [count]);

    // 动画播完就摘掉 class：留着会靠 hover 的 :has 规则反复触发
    useEffect(() => {
        if (!hasNew) {
            return;
        }
        const timer = setTimeout(() => setHasNew(false), 800);
        return () => clearTimeout(timer);
    }, [hasNew]);

    return (
        // 徽标挂在按钮外侧：icon-btn 为了流光动效带了 overflow:hidden,放按钮内会被裁掉
        <div className={`floating-nav__bell ${hasNew ? 'has-new' : ''}`}>
            <Button
                variant="ghost"
                size="icon"
                onClick={() => navigate('/notifications')}
                aria-label={label}
                className="floating-nav__icon-btn"
            >
                <Bell size={19} />
            </Button>
            {count > 0 && (
                <Badge
                    variant="destructive"
                    aria-hidden="true"
                    className="floating-nav__bell-badge absolute -right-1 -top-1 h-5 min-w-5 px-1.5 text-[0.65rem]"
                >
                    {count > 99 ? '99+' : count}
                </Badge>
            )}
        </div>
    );
}
