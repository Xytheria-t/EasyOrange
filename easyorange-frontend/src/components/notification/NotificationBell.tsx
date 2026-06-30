import { useQuery } from '@tanstack/react-query'
import { Bell } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui'
import { notificationApi } from '@/api/notificationApi'
import { useNotificationSocket } from '@/hooks/useNotificationSocket'

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
    <Button
      variant="ghost"
      size="icon"
      onClick={() => navigate('/notifications')}
      aria-label="通知"
      className="relative"
    >
      <Bell size={19} />
      {count > 0 && (
        <Badge variant="destructive" className="absolute -right-1 -top-1 h-5 min-w-5 px-1.5 text-[0.65rem]">
          {count > 99 ? '99+' : count}
        </Badge>
      )}
    </Button>
  );
}
