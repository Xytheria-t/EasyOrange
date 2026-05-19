import { useState, useCallback } from 'react'
import { aiApi } from '@/api/aiApi'
import type { AutoListingResult } from '@/api/aiApi'
import { useUIStore } from '@/store/uiStore'

function checkImageAccessibility(urls: string[]): boolean {
  const hasLocalhost = urls.some(u =>
    u.startsWith('http://localhost') || u.startsWith('https://localhost') || u.startsWith('http://127.0.0.1')
  )
  const hasRelative = urls.some(u => u.startsWith('/') && !u.startsWith('//'))
  return !hasLocalhost && !hasRelative
}

export function useAutoListing() {
  const [result, setResult] = useState<AutoListingResult | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const addToast = useUIStore((s) => s.addToast)

  const analyzeImages = useCallback(async (imageUrls: string[]) => {
    if (!checkImageAccessibility(imageUrls)) {
      addToast({ type: 'warning', message: '部分图片使用本地地址，AI 可能无法访问；建议部署后使用' })
    }
    setIsLoading(true)
    try {
      const res = await aiApi.autoListing(imageUrls)
      if (res.data) {
        setResult(res.data)
        addToast({ type: 'success', message: 'AI 智能识别完成，已自动填充信息' })
      }
    } catch {
      addToast({ type: 'error', message: 'AI 识别失败，请稍后重试' })
    } finally {
      setIsLoading(false)
    }
  }, [addToast])

  const clearResult = useCallback(() => {
    setResult(null)
  }, [])

  return { result, isLoading, analyzeImages, clearResult }
}