import { useCallback } from 'react'
import { messageApi } from '@/api/messageApi'
import { useChatStore } from '@/store/chatStore'

const RECALL_TIMEOUT_MS = 2 * 60 * 1000

export function useMessageRecall(conversationId: string) {
  const updateMessage = useChatStore((s) => s.updateMessage)

  const canRecall = useCallback(
    (message: { createTime: string; status: string; senderId: string }, currentUserId: string) => {
      if (message.senderId !== currentUserId) {return false}
      if (message.status === 'RECALLED') {return false}
      const sentTime = new Date(message.createTime).getTime()
      return Date.now() - sentTime < RECALL_TIMEOUT_MS
    },
    []
  )

  const recallMessage = useCallback(
    async (messageId: string) => {
      try {
        await messageApi.recallMessage(messageId)
        updateMessage(conversationId, messageId, {
          status: 'RECALLED',
          content: '[消息已撤回]',
          type: 'RECALLED',
          recalledAt: new Date().toISOString(),
        })
        return true
      } catch {
        return false
      }
    },
    [conversationId, updateMessage]
  )

  const copyMessage = useCallback((content: string) => {
    navigator.clipboard.writeText(content).catch(console.error)
  }, [])

  return { canRecall, recallMessage, copyMessage }
}
