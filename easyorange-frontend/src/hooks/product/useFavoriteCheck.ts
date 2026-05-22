import { useState, useCallback, useMemo } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { favoriteApi } from '@/api/favoriteApi'
import { useAuthStore } from '@/store/authStore'
import { useUIStore } from '@/store/uiStore'

export function useFavoriteCheck() {
  const { token } = useAuthStore()
  const queryClient = useQueryClient()
  const addToast = useUIStore((s) => s.addToast)
  const [favoriteMap, setFavoriteMap] = useState<Record<string, boolean>>(() => ({}))

  const checkFavorites = useCallback(async (productIds: string[]) => {
    if (!token || productIds.length === 0) {
      setFavoriteMap({})
      return
    }
    try {
      const response = await favoriteApi.batchCheck(productIds)
      setFavoriteMap(response.data ?? {})
    } catch {
      setFavoriteMap({})
    }
  }, [token])

  const toggleFavorite = useCallback(async (productId: string, shouldFavorite: boolean) => {
    if (!token) {
      return false
    }
    try {
      if (shouldFavorite) {
        await favoriteApi.addFavorite(productId)
        addToast({ type: 'success', message: '已收藏' })
      } else {
        await favoriteApi.removeFavorite(productId)
        addToast({ type: 'success', message: '已取消收藏' })
      }
      setFavoriteMap(prev => ({ ...prev, [productId]: shouldFavorite }))
      queryClient.invalidateQueries({ queryKey: ['favorites'] })
      return true
    } catch {
      addToast({ type: 'error', message: shouldFavorite ? '收藏失败' : '取消收藏失败' })
      return false
    }
  }, [token, addToast, queryClient])

  const effectiveFavoriteMap = useMemo(() => {
    return token ? favoriteMap : {}
  }, [token, favoriteMap])

  const isFavorited = useCallback((productId: string): boolean => {
    return effectiveFavoriteMap[productId] ?? false
  }, [effectiveFavoriteMap])

  return { favoriteMap: effectiveFavoriteMap, checkFavorites, isFavorited, toggleFavorite }
}
