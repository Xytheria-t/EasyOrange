import { useCallback, useEffect, useRef, useState } from 'react';

interface WaveLayer {
  amplitude: number;
  frequency: number;
  speed: number;
  offset: number;
  color: string;
}

export default function BackgroundEffects() {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animationRef = useRef<number>(0);
  const startTimeRef = useRef<number>(0);
  const isVisibleState = useState(true);
  const setIsVisible = isVisibleState[1];
  const shouldAnimateState = useState(true);
  const setShouldAnimate = shouldAnimateState[1];
  const lastFrameTimeRef = useRef<number>(0);
  const frameCountRef = useRef<number>(0);
  const isVisibleRef = useRef(true);
  const shouldAnimateRef = useRef(true);
  const drawScheduledRef = useRef(false);

  const prefersReducedMotion = typeof window !== 'undefined' 
    ? window.matchMedia('(prefers-reduced-motion: reduce)').matches 
    : false;

  const scheduleDraw = useCallback(() => {
    if (drawScheduledRef.current) {return;}
    drawScheduledRef.current = true;
    
    const doDraw = () => {
      drawScheduledRef.current = false;
      const canvas = canvasRef.current;
      const ctx = canvas?.getContext('2d');
      if (!ctx || !canvas || !isVisibleRef.current || !shouldAnimateRef.current) {
        animationRef.current = requestAnimationFrame(scheduleDraw);
        return;
      }

      const now = performance.now();
      const deltaTime = now - lastFrameTimeRef.current;
      
      if (deltaTime < 16) {
        animationRef.current = requestAnimationFrame(scheduleDraw);
        return;
      }
      
      lastFrameTimeRef.current = now;
      frameCountRef.current++;

      const width = window.innerWidth;
      const height = window.innerHeight;

      ctx.clearRect(0, 0, width, height);

      const elapsed = (now - startTimeRef.current) / 1000;
      const isMobile = window.innerWidth < 768;
      const waveStep = isMobile ? 48 : 24;

      const waveLayers: WaveLayer[] = [
        { amplitude: 80, frequency: 0.003, speed: 0.4, offset: 0, color: 'rgba(249, 115, 22, 0.06)' },
        { amplitude: 60, frequency: 0.004, speed: 0.3, offset: 1.5, color: 'rgba(251, 113, 133, 0.05)' },
        ...(isMobile ? [] : [
          { amplitude: 100, frequency: 0.002, speed: 0.2, offset: 3, color: 'rgba(195, 155, 211, 0.04)' },
        ]),
      ];

      for (const wave of waveLayers) {
        ctx.beginPath();
        const time = elapsed * wave.speed + wave.offset;

        for (let x = 0; x <= width; x += waveStep) {
          const y = height * 0.5 + Math.sin(x * wave.frequency + time) * wave.amplitude;

          if (x === 0) {
            ctx.moveTo(x, y);
          } else {
            ctx.lineTo(x, y);
          }
        }

        ctx.lineTo(width, height);
        ctx.lineTo(0, height);
        ctx.closePath();

        ctx.fillStyle = wave.color;
        ctx.fill();
      }

      animationRef.current = requestAnimationFrame(scheduleDraw);
    };
    
    requestAnimationFrame(doDraw);
  }, []);

  useEffect(() => {
    if (prefersReducedMotion) {
      shouldAnimateRef.current = false;
      setShouldAnimate(false);
      return;
    }

    const canvas = canvasRef.current;
    if (!canvas) {return;}

    const ctx = canvas.getContext('2d');
    if (!ctx) {return;}
    const c = ctx;

    function resize() {
      if (!canvas) {return;}
      const dpr = Math.min(window.devicePixelRatio, 1.5);
      canvas.width = window.innerWidth * dpr;
      canvas.height = window.innerHeight * dpr;
      canvas.style.width = `${window.innerWidth  }px`;
      canvas.style.height = `${window.innerHeight  }px`;
      c.scale(dpr, dpr);
    }

    const handleVisibilityChange = () => {
      const visible = !document.hidden;
      if (isVisibleRef.current !== visible) {
        isVisibleRef.current = visible;
        setIsVisible(visible);
      }
      if (visible) {
        lastFrameTimeRef.current = performance.now();
      }
    };

    const container = containerRef.current;
    let visibilityObserver: IntersectionObserver | null = null;
    
    if (container) {
      visibilityObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          const visible = entry.isIntersecting && !document.hidden;
          if (isVisibleRef.current !== visible) {
            isVisibleRef.current = visible;
            setIsVisible(visible);
          }
          if (entry.isIntersecting) {
            lastFrameTimeRef.current = performance.now();
          }
        });
      }, { threshold: 0, rootMargin: '100px' });
      visibilityObserver.observe(container);
    }

    resize();
    startTimeRef.current = performance.now();
    lastFrameTimeRef.current = performance.now();
    scheduleDraw();

    let resizeTimeout: ReturnType<typeof setTimeout> | null = null;
    const handleResize = () => {
      if (resizeTimeout) {clearTimeout(resizeTimeout);}
      resizeTimeout = setTimeout(resize, 200);
    };

    window.addEventListener('resize', handleResize, { passive: true });
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      cancelAnimationFrame(animationRef.current);
      window.removeEventListener('resize', handleResize);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      visibilityObserver?.disconnect();
      if (resizeTimeout) {clearTimeout(resizeTimeout);}
    };
  }, [prefersReducedMotion, setIsVisible, setShouldAnimate, scheduleDraw]);

  if (prefersReducedMotion) {
    return (
      <div ref={containerRef}>
        <div className="bg-gradient-mesh"></div>
        <div className="floating-orbs">
          <div className="orb orb-1"></div>
          <div className="orb orb-2"></div>
          <div className="orb orb-3"></div>
        </div>
        <div className="noise-overlay"></div>
      </div>
    );
  }

  return (
    <div ref={containerRef}>
      <div className="bg-gradient-mesh"></div>

      <div className="floating-orbs">
        <div className="orb orb-1"></div>
        <div className="orb orb-2"></div>
        <div className="orb orb-3"></div>
      </div>

      <canvas
        ref={canvasRef}
        className="wave-canvas"
        style={{
          position: 'fixed',
          inset: 0,
          zIndex: -2,
          pointerEvents: 'none',
        }}
      />

      <div className="noise-overlay"></div>
    </div>
  );
}
