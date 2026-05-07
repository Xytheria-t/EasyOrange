import { useEffect, useRef, useCallback, useState } from 'react';

function ScrollProgressBar() {
  const [isHovering, setIsHovering] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const rafRef = useRef<number | null>(null);
  const progressRef = useRef(0);
  const isVisibleRef = useRef(false);

  const updateProgress = useCallback(() => {
    const scrollTop = window.scrollY;
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;

    const newVisible = scrollTop > 20;
    if (newVisible !== isVisibleRef.current) {
      isVisibleRef.current = newVisible;
      containerRef.current?.setAttribute('data-visible', String(newVisible));
    }

    if (docHeight <= 0) {
      if (progressRef.current !== 0) {
        progressRef.current = 0;
        containerRef.current?.style.setProperty('--progress', '0%');
      }
      return;
    }

    const scrollPercent = Math.min((scrollTop / docHeight) * 100, 100);
    if (Math.abs(scrollPercent - progressRef.current) > 0.5) {
      progressRef.current = scrollPercent;
      containerRef.current?.style.setProperty('--progress', `${scrollPercent}%`);
    }
  }, []);

  const handleScroll = useCallback(() => {
    if (rafRef.current) {
      cancelAnimationFrame(rafRef.current);
    }
    rafRef.current = requestAnimationFrame(updateProgress);
  }, [updateProgress]);

  useEffect(() => {
    updateProgress();
    window.addEventListener('scroll', handleScroll, { passive: true });
    window.addEventListener('resize', handleScroll, { passive: true });
    return () => {
      window.removeEventListener('scroll', handleScroll);
      window.removeEventListener('resize', handleScroll);
      if (rafRef.current) {
        cancelAnimationFrame(rafRef.current);
      }
    };
  }, [handleScroll, updateProgress]);

  const scrollToPercent = useCallback((percent: number) => {
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    window.scrollTo({
      top: Math.max(0, Math.min(1, percent)) * docHeight,
      behavior: 'smooth',
    });
  }, []);

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    scrollToPercent(clickX / rect.width);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    const step = 10;
    let newPercent = progressRef.current;

    switch (e.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        e.preventDefault();
        newPercent = Math.min(100, progressRef.current + step);
        break;
      case 'ArrowLeft':
      case 'ArrowUp':
        e.preventDefault();
        newPercent = Math.max(0, progressRef.current - step);
        break;
      case 'Home':
        e.preventDefault();
        newPercent = 0;
        break;
      case 'End':
        e.preventDefault();
        newPercent = 100;
        break;
      default:
        return;
    }

    scrollToPercent(newPercent / 100);
  };

  return (
    <div
      ref={containerRef}
      className="scroll-progress-container"
      data-visible={false}
      data-hovering={isHovering}
      role="progressbar"
      aria-label="页面滚动进度"
      aria-valuemin={0}
      aria-valuemax={100}
      tabIndex={0}
      onClick={handleClick}
      onMouseEnter={() => setIsHovering(true)}
      onMouseLeave={() => setIsHovering(false)}
      onKeyDown={handleKeyDown}
    >
      <div className="scroll-progress-track">
        <div className="scroll-progress-fill" />
        <div className="scroll-progress-shine" />
        <div className="scroll-progress-thumb" />
      </div>
      <div className="scroll-progress-glow" />
    </div>
  );
}

export default ScrollProgressBar;
