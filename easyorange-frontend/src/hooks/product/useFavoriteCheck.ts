import { useState, useCallback, useMemo } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { favoriteApi } from '@/api/favoriteApi'
import { useAuthStore } from '@/store/authStore'
import { useUIStore } from '@/store/uiStore'

export function useFavoriteCheck() {
  const { token } = useAuthStore()
  const queryClient = useQueryClient()
  const addToast = useUIStore((s) => s.addToast)
  const [favoriteMap, setFavoriteMap] = useState<Record<number, boolean>>(() => ({}))

  const checkFavorites = useCallback(async (productIds: number[]) => {
    if (!token || productIds.length === 0) {
      setFavoriteMap({})
      return
    }
    try {
      const res = await favoriteApi.batchCheck(productIds)
      setFavoriteMap(res.data ?? {})
    } catch {
      setFavoriteMap({})
    }
  }, [token])

  const toggleFavorite = useCallback(async (productId: number, shouldFavorite: boolean) => {
    if (!token) {
      return false
    }
    try {
      if (shouldFavorite) {
        await favoriteApi.add(productId)
        addToast({ type: 'success', message: '已收藏' })
      } else {
        await favoriteApi.remove(productId)
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

  const isFavorited = useCallback((productId: number): boolean => {
    return effectiveFavoriteMap[productId] ?? false
  }, [effectiveFavoriteMap])

  return { favoriteMap: effectiveFavoriteMap, checkFavorites, isFavorited, toggleFavorite }
}
