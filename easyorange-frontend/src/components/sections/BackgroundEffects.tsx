import { useEffect, useRef } from 'react';

interface WaveLayer {
  amplitude: number;
  frequency: number;
  speed: number;
  offset: number;
  color: string;
}

export default function BackgroundEffects() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animationRef = useRef<number>(0);
  const timeRef = useRef(0);
  const mouseRef = useRef({ x: 0.5, y: 0.5 });
  const smoothMouseRef = useRef({ x: 0.5, y: 0.5 });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const waveLayers: WaveLayer[] = [
      { amplitude: 80, frequency: 0.003, speed: 0.0004, offset: 0, color: 'rgba(249, 115, 22, 0.06)' },
      { amplitude: 60, frequency: 0.004, speed: 0.0003, offset: 1.5, color: 'rgba(251, 113, 133, 0.05)' },
      { amplitude: 100, frequency: 0.002, speed: 0.0002, offset: 3, color: 'rgba(195, 155, 211, 0.04)' },
      { amplitude: 50, frequency: 0.005, speed: 0.0005, offset: 4.5, color: 'rgba(251, 191, 36, 0.03)' },
    ];

    function resize() {
      if (!canvas) return;
      const dpr = Math.min(window.devicePixelRatio, 2);
      canvas.width = window.innerWidth * dpr;
      canvas.height = window.innerHeight * dpr;
      canvas.style.width = window.innerWidth + 'px';
      canvas.style.height = window.innerHeight + 'px';
      ctx!.scale(dpr, dpr);
    }

    function draw() {
      if (!ctx || !canvas) return;
      const width = window.innerWidth;
      const height = window.innerHeight;

      // Smooth mouse following
      smoothMouseRef.current.x += (mouseRef.current.x - smoothMouseRef.current.x) * 0.02;
      smoothMouseRef.current.y += (mouseRef.current.y - smoothMouseRef.current.y) * 0.02;

      ctx.clearRect(0, 0, width, height);

      // Draw flowing waves
      waveLayers.forEach((wave, index) => {
        ctx.beginPath();
        const time = timeRef.current * wave.speed + wave.offset;
        const mouseInfluence = (index % 2 === 0 ? 1 : -1) * 20;

        for (let x = 0; x <= width; x += 4) {
          const normalizedX = x / width;
          const distToMouse = Math.abs(normalizedX - smoothMouseRef.current.x);
          const mouseEffect = Math.max(0, 1 - distToMouse * 3) * mouseInfluence;

          const y = height * 0.5 +
            Math.sin(x * wave.frequency + time) * wave.amplitude +
            Math.sin(x * wave.frequency * 1.5 + time * 1.3) * wave.amplitude * 0.5 +
            mouseEffect;

          if (x === 0) {
            ctx.moveTo(x, y);
          } else {
            ctx.lineTo(x, y);
          }
        }

        // Close the path to fill
        ctx.lineTo(width, height);
        ctx.lineTo(0, height);
        ctx.closePath();

        ctx.fillStyle = wave.color;
        ctx.fill();
      });

      timeRef.current += 1;
      animationRef.current = requestAnimationFrame(draw);
    }

    function handleMouseMove(e: MouseEvent) {
      mouseRef.current = {
        x: e.clientX / window.innerWidth,
        y: e.clientY / window.innerHeight,
      };
    }

    resize();
    draw();

    window.addEventListener('resize', resize);
    window.addEventListener('mousemove', handleMouseMove);

    return () => {
      cancelAnimationFrame(animationRef.current);
      window.removeEventListener('resize', resize);
      window.removeEventListener('mousemove', handleMouseMove);
    };
  }, []);

  return (
    <>
      {/* Base warm gradient background */}
      <div className="bg-gradient-mesh"></div>

      {/* Animated gradient orbs - reduced count for minimalism */}
      <div className="floating-orbs">
        <div className="orb orb-1"></div>
        <div className="orb orb-2"></div>
        <div className="orb orb-3"></div>
      </div>

      {/* Flowing wave canvas */}
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

      {/* Subtle noise texture overlay */}
      <div className="noise-overlay"></div>
    </>
  );
}
