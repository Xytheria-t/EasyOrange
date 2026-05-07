import { useRef, useCallback, useEffect, useState } from 'react'

interface ZoomConfig {
  zoomLevel?: number
  lensSize?: number
}

interface ZoomState {
  isVisible: boolean
  lensX: number
  lensY: number
  bgX: number
  bgY: number
}

export function useImageZoom(config: ZoomConfig = {}) {
  const { zoomLevel = 2.5, lensSize = 120 } = config

  const containerRef = useRef<HTMLDivElement>(null)
  const imageRef = useRef<HTMLImageElement>(null)
  const [zoomState, setZoomState] = useState<ZoomState>({
    isVisible: false,
    lensX: 0,
    lensY: 0,
    bgX: 0,
    bgY: 0,
  })

  const handleMouseMove = useCallback(
    (e: MouseEvent) => {
      const container = containerRef.current
      const image = imageRef.current
      if (!container || !image) {return}

      const rect = container.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top

      // Clamp lens position
      const lensX = Math.max(lensSize / 2, Math.min(x, rect.width - lensSize / 2))
      const lensY = Math.max(lensSize / 2, Math.min(y, rect.height - lensSize / 2))

      // Calculate background position for zoomed view
      const bgX = (lensX / rect.width) * 100
      const bgY = (lensY / rect.height) * 100

      setZoomState({
        isVisible: true,
        lensX: lensX - lensSize / 2,
        lensY: lensY - lensSize / 2,
        bgX,
        bgY,
      })
    },
    [lensSize]
  )

  const handleMouseEnter = useCallback(() => {
    setZoomState((prev) => ({ ...prev, isVisible: true }))
  }, [])

  const handleMouseLeave = useCallback(() => {
    setZoomState((prev) => ({ ...prev, isVisible: false }))
  }, [])

  useEffect(() => {
    const container = containerRef.current
    if (!container) {return}

    container.addEventListener('mousemove', handleMouseMove)
    container.addEventListener('mouseenter', handleMouseEnter)
    container.addEventListener('mouseleave', handleMouseLeave)

    return () => {
      container.removeEventListener('mousemove', handleMouseMove)
      container.removeEventListener('mouseenter', handleMouseEnter)
      container.removeEventListener('mouseleave', handleMouseLeave)
    }
  }, [handleMouseMove, handleMouseEnter, handleMouseLeave])

  const lensStyle: React.CSSProperties = {
    position: 'absolute',
    width: lensSize,
    height: lensSize,
    borderRadius: '50%',
    border: '2px solid rgba(249, 115, 22, 0.4)',
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    boxShadow: '0 0 20px rgba(249, 115, 22, 0.2), inset 0 0 20px rgba(255, 255, 255, 0.2)',
    pointerEvents: 'none',
    zIndex: 10,
    left: zoomState.lensX,
    top: zoomState.lensY,
    opacity: zoomState.isVisible ? 1 : 0,
    transition: 'opacity 0.2s ease',
    backdropFilter: 'blur(1px)',
  }

  const zoomWindowStyle: React.CSSProperties = {
    position: 'absolute',
    width: 300,
    height: 300,
    borderRadius: 'var(--radius-xl)',
    border: '1px solid rgba(249, 115, 22, 0.15)',
    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.15)',
    overflow: 'hidden',
    pointerEvents: 'none',
    zIndex: 20,
    opacity: zoomState.isVisible ? 1 : 0,
    transition: 'opacity 0.2s ease',
    backgroundImage: imageRef.current ? `url(${imageRef.current.src})` : 'none',
    backgroundRepeat: 'no-repeat',
    backgroundSize: `${zoomLevel * 100}%`,
    backgroundPosition: `${zoomState.bgX}% ${zoomState.bgY}%`,
  }

  return {
    containerRef,
    imageRef,
    lensStyle,
    zoomWindowStyle,
    isZoomVisible: zoomState.isVisible,
    zoomLevel,
  }
}
