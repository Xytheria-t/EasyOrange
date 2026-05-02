import { useRef, useCallback, useEffect } from 'react'

interface TiltConfig {
  maxRotateX?: number
  maxRotateY?: number
  perspective?: number
  scale?: number
  transitionDuration?: number
}

interface TiltState {
  rotateX: number
  rotateY: number
  scale: number
}

export function use3DTilt(config: TiltConfig = {}) {
  const {
    maxRotateX = 10,
    maxRotateY = 10,
    perspective = 1000,
    scale = 1.02,
    transitionDuration = 400,
  } = config

  const elementRef = useRef<HTMLDivElement>(null)
  const stateRef = useRef<TiltState>({ rotateX: 0, rotateY: 0, scale: 1 })
  const rafRef = useRef<number>(0)
  const isHoveringRef = useRef(false)

  const applyTransform = useCallback(() => {
    const el = elementRef.current
    if (!el) return
    const { rotateX, rotateY, scale } = stateRef.current
    el.style.transform = `perspective(${perspective}px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(${scale}, ${scale}, ${scale})`
  }, [perspective])

  const animateTo = useCallback(
    (targetRotateX: number, targetRotateY: number, targetScale: number) => {
      const startRotateX = stateRef.current.rotateX
      const startRotateY = stateRef.current.rotateY
      const startScale = stateRef.current.scale
      const startTime = performance.now()

      const animate = (currentTime: number) => {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / transitionDuration, 1)
        // easeOutCubic
        const eased = 1 - Math.pow(1 - progress, 3)

        stateRef.current = {
          rotateX: startRotateX + (targetRotateX - startRotateX) * eased,
          rotateY: startRotateY + (targetRotateY - startRotateY) * eased,
          scale: startScale + (targetScale - startScale) * eased,
        }

        applyTransform()

        if (progress < 1) {
          rafRef.current = requestAnimationFrame(animate)
        }
      }

      if (rafRef.current) cancelAnimationFrame(rafRef.current)
      rafRef.current = requestAnimationFrame(animate)
    },
    [transitionDuration, applyTransform]
  )

  const handleMouseMove = useCallback(
    (e: MouseEvent) => {
      const el = elementRef.current
      if (!el || !isHoveringRef.current) return

      const rect = el.getBoundingClientRect()
      const centerX = rect.left + rect.width / 2
      const centerY = rect.top + rect.height / 2

      const mouseX = e.clientX - centerX
      const mouseY = e.clientY - centerY

      const rotateY = (mouseX / (rect.width / 2)) * maxRotateY
      const rotateX = -(mouseY / (rect.height / 2)) * maxRotateX

      // Direct update for responsiveness during mousemove
      stateRef.current = { rotateX, rotateY, scale }
      applyTransform()
    },
    [maxRotateX, maxRotateY, scale, applyTransform]
  )

  const handleMouseEnter = useCallback(() => {
    isHoveringRef.current = true
    const el = elementRef.current
    if (el) {
      el.style.transition = 'none'
    }
  }, [])

  const handleMouseLeave = useCallback(() => {
    isHoveringRef.current = false
    const el = elementRef.current
    if (el) {
      el.style.transition = `transform ${transitionDuration}ms cubic-bezier(0.22, 1, 0.36, 1)`
    }
    animateTo(0, 0, 1)
  }, [animateTo, transitionDuration])

  useEffect(() => {
    const el = elementRef.current
    if (!el) return

    el.addEventListener('mouseenter', handleMouseEnter)
    el.addEventListener('mousemove', handleMouseMove)
    el.addEventListener('mouseleave', handleMouseLeave)

    return () => {
      el.removeEventListener('mouseenter', handleMouseEnter)
      el.removeEventListener('mousemove', handleMouseMove)
      el.removeEventListener('mouseleave', handleMouseLeave)
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
    }
  }, [handleMouseEnter, handleMouseMove, handleMouseLeave])

  return elementRef
}
