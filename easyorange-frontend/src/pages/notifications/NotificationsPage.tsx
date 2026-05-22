import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Bell,
  CheckCircle2,
  XCircle,
  Flag,
  Info,
  CheckCheck,
  ArrowLeft,
  ArrowRight,
  Loader2,
  Megaphone
} from 'lucide-react';
import { notificationApi } from '@/api/notificationApi';
import type { NotificationItem } from '@/types';
import './notifications.css';

const PAGE_SIZE = 20;

function getNotificationIcon(title: string) {
  if (title.includes('审核通过')) return { icon: CheckCircle2, color: '#22C55E' };
  if (title.includes('审核未通过')) return { icon: XCircle, color: '#EF4444' };
  if (title.includes('举报')) return { icon: Flag, color: '#F59E0B' };
  if (title.includes('系统') || title.includes('通知')) return { icon: Megaphone, color: '#8B5CF6' };
  return { icon: Info, color: '#6B7280' };
}

function formatTime(timeString: string): string {
  const date = new Date(timeString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return '刚刚';
  if (diffMins < 60) return `${diffMins}分钟前`;
  if (diffHours < 24) return `${diffHours}小时前`;
  if (diffDays < 7) return `${diffDays}天前`;

  return date.toLocaleDateString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
  });
}

export default function NotificationsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);

  const { data, isLoading, error } = useQuery({
    queryKey: ['notifications', page],
    queryFn: async () => {
      const response = await notificationApi.getNotifications(page, PAGE_SIZE);
      return response.data;
    },
  });

  const markAsReadMutation = useMutation({
    mutationFn: (id: string) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
  });

  const markAllReadMutation = useMutation({
    mutationFn: () => notificationApi.markAllSystemAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
  });

  const handleNotificationClick = (item: NotificationItem) => {
    if (item.isRead === 0) {
      markAsReadMutation.mutate(item.id);
    }
    if (item.businessId) {
      navigate(`/products/${item.businessId}`);
    }
  };

  const notifications = data?.records ?? [];
  const totalPages = data?.pages ?? 1;
  const unreadCount = notifications.filter((n) => n.isRead === 0).length;

  return (
    <div className="notifications-page">
      <div className="notifications-ambient">
        <div className="notifications-orb notifications-orb-1" />
        <div className="notifications-orb notifications-orb-2" />
        <div className="notifications-orb notifications-orb-3" />
      </div>

      <div className="notifications-container">
        {/* Header */}
        <div className="notifications-header">
          <div className="notifications-header-left">
            <button
              className="notifications-back-btn"
              onClick={() => navigate(-1)}
              aria-label="返回"
            >
              <ArrowLeft size={20} />
            </button>
            <div className="notifications-header-info">
              <h1 className="notifications-title">系统通知</h1>
              <div className="notifications-header-meta">
                <span className="notifications-kicker">
                  <span className="kicker-dot" />
                  通知中心
                </span>
                <span className="notifications-subtitle">
                  审核结果、举报处理等系统消息
                </span>
              </div>
            </div>
          </div>
          {unreadCount > 0 && (
            <button
              className="notifications-mark-all-btn"
              onClick={() => markAllReadMutation.mutate()}
              disabled={markAllReadMutation.isPending}
            >
              {markAllReadMutation.isPending ? (
                <Loader2 size={16} className="animate-spin" />
              ) : (
                <CheckCheck size={16} />
              )}
              全部已读
            </button>
          )}
        </div>

        <div className="notifications-divider" />

        {/* Content */}
        {isLoading ? (
          <div className="notifications-loading">
            <div className="loading-spinner" />
            <span>加载中...</span>
          </div>
        ) : error ? (
          <div className="notifications-empty">
            <Bell size={48} className="notifications-empty-icon" />
            <h3>加载失败</h3>
            <p>请稍后重试</p>
            <button
              className="notifications-retry-btn"
              onClick={() => queryClient.invalidateQueries({ queryKey: ['notifications'] })}
            >
              重新加载
            </button>
          </div>
        ) : notifications.length === 0 ? (
          <div className="notifications-empty">
            <div className="notifications-empty-visual">
              <div className="empty-orbit" />
              <div className="empty-icon-wrap">
                <Bell size={36} />
              </div>
            </div>
            <h3>暂无系统通知</h3>
            <p>当您的商品审核结果或举报处理完成时，会在这里显示</p>
          </div>
        ) : (
          <>
            <div className="notifications-list">
              {notifications.map((item) => {
                const { icon: Icon, color } = getNotificationIcon(item.title);
                const isUnread = item.isRead === 0;
                return (
                  <button
                    key={item.id}
                    className={`notification-card ${isUnread ? 'unread' : ''}`}
                    onClick={() => handleNotificationClick(item)}
                  >
                    {isUnread && <div className="notification-card-accent" />}
                    <div
                      className="notification-card-icon"
                      style={{
                        background: `linear-gradient(135deg, ${color}18, ${color}08)`,
                        borderColor: `${color}15`,
                        color
                      }}
                    >
                      <Icon size={18} />
                    </div>
                    <div className="notification-card-body">
                      <div className="notification-card-header">
                        <h3 className="notification-card-title">
                          {item.title}
                          {isUnread && <span className="notification-unread-dot" />}
                        </h3>
                        <span className="notification-card-time">
                          {formatTime(item.createTime)}
                        </span>
                      </div>
                      <p className="notification-card-content">{item.content}</p>
                      {item.businessId && (
                        <span className="notification-card-link">
                          查看详情
                          <ArrowRight size={11} />
                        </span>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="notifications-pagination">
                <button
                  className="pagination-btn"
                  disabled={page <= 1}
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                >
                  上一页
                </button>
                <span className="pagination-info">
                  {page} / {totalPages}
                </span>
                <button
                  className="pagination-btn"
                  disabled={page >= totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  下一页
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
