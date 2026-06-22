import { useQuery } from '@tanstack/react-query'
import { Bell } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
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
    <button
      className="floating-nav__icon-btn"
      onClick={() => navigate('/notifications')}
      aria-label="通知"
    >
      <Bell size={19} />
      {count > 0 && (
        <span className="floating-nav__notification-badge">
          {count > 99 ? '99+' : count}
        </span>
      )}
    </button>
  );
}
