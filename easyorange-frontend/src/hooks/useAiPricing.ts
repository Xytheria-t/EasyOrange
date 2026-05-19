import { useState, useCallback } from 'react'
import { aiApi, type PricingSuggestion, type PriceSuggestionParams } from '@/api/aiApi'
import { useUIStore } from '@/store/uiStore'

export function useAiPricing() {
  const [suggestion, setSuggestion] = useState<PricingSuggestion | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const addToast = useUIStore((s) => s.addToast)

  const getPricing = useCallback(async (params: PriceSuggestionParams) => {
    setIsLoading(true)
    try {
      const res = await aiApi.suggestPrice(params)
      if (res.data) {
        setSuggestion(res.data)
      }
    } catch {
      addToast({ type: 'error', message: 'AI 定价建议获取失败，请稍后重试' })
    } finally {
      setIsLoading(false)
    }
  }, [addToast])

  const clearSuggestion = useCallback(() => {
    setSuggestion(null)
  }, [])

  return { suggestion, isLoading, getPricing, clearSuggestion }
}