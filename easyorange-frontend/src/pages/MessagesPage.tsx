import { useNavigate } from 'react-router-dom';
import { MessageCircle, Bell, Loader2 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { messageApi } from '@/api/messageApi';
import type { ChatSession } from '@/types';

export function MessagesPage() {
  const navigate = useNavigate();

  const { data: conversations, isLoading, error } = useQuery({
    queryKey: ['messages', 'conversations'],
    queryFn: async () => {
      const response = await messageApi.getConversations();
      return (response.data ?? []) as unknown as ChatSession[];
    },
    staleTime: 15 * 1000,
  });

  if (isLoading) {
    return (
      <div className="py-6">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">消息中心</h1>
          <p className="mt-1 text-sm text-gray-500">查看您的所有消息</p>
        </div>
        <div className="flex items-center justify-center py-20">
          <Loader2 size={32} className="animate-spin text-primary-600" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="py-6">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">消息中心</h1>
        </div>
        <div className="rounded-2xl bg-white p-8 shadow-sm ring-1 ring-gray-200/50 text-center">
          <p className="text-red-500">加载消息失败，请稍后重试</p>
        </div>
      </div>
    );
  }

  const hasConversations = conversations && conversations.length > 0;

  return (
    <div className="py-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">消息中心</h1>
        <p className="mt-1 text-sm text-gray-500">查看您的所有消息</p>
      </div>

      {!hasConversations ? (
        <div className="rounded-2xl bg-white p-8 shadow-sm ring-1 ring-gray-200/50">
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <div className="flex h-20 w-20 items-center justify-center rounded-full bg-gray-100">
              <MessageCircle size={36} className="text-gray-400" />
            </div>
            <h3 className="mt-4 text-lg font-semibold text-gray-900">暂无新消息</h3>
            <p className="mt-2 max-w-sm text-sm text-gray-500">
              当您收到卖家回复或系统通知时，会在这里显示
            </p>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          {conversations.map((conv) => (
            <div
              key={conv.id}
              className="flex items-center gap-4 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-gray-200/50 hover:shadow-md transition-shadow cursor-pointer"
              onClick={() => navigate(`/messages?userId=${conv.targetUserId}`)}
            >
              <div className="relative flex-shrink-0">
                {conv.targetUserAvatar ? (
                  <img
                    src={conv.targetUserAvatar}
                    alt={conv.targetUserName}
                    className="h-12 w-12 rounded-full object-cover"
                  />
                ) : (
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary-50 text-primary-600">
                    <span className="text-lg font-semibold">
                      {conv.targetUserName?.charAt(0) ?? '?'}
                    </span>
                  </div>
                )}
                {conv.unreadCount > 0 && (
                  <span className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-xs font-bold text-white">
                    {conv.unreadCount > 9 ? '9+' : conv.unreadCount}
                  </span>
                )}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-gray-900">{conv.targetUserName}</h3>
                  <span className="text-xs text-gray-400">{conv.lastMessageTime}</span>
                </div>
                <p className="mt-1 text-sm text-gray-500 truncate">{conv.lastMessage}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="mt-6 rounded-2xl bg-white p-6 shadow-sm ring-1 ring-gray-200/50">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary-50 text-primary-600">
            <Bell size={20} />
          </div>
          <div>
            <p className="font-medium text-gray-900">消息提醒</p>
            <p className="text-sm text-gray-500">开启消息推送，及时获取最新动态</p>
          </div>
        </div>
      </div>
    </div>
  );
}
