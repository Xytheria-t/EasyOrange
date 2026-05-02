import { useEffect, useState, useCallback } from 'react';

function ScrollProgressBar() {
  const [progress, setProgress] = useState(0);
  const [isVisible, setIsVisible] = useState(false);
  const [isHovering, setIsHovering] = useState(false);

  const updateProgress = useCallback(() => {
    const scrollTop = window.scrollY;
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    if (docHeight <= 0) {
      setProgress(0);
      setIsVisible(false);
      return;
    }
    const scrollPercent = Math.min((scrollTop / docHeight) * 100, 100);
    setProgress(scrollPercent);
    setIsVisible(scrollTop > 20);
  }, []);

  useEffect(() => {
    updateProgress();
    window.addEventListener('scroll', updateProgress, { passive: true });
    window.addEventListener('resize', updateProgress, { passive: true });
    return () => {
      window.removeEventListener('scroll', updateProgress);
      window.removeEventListener('resize', updateProgress);
    };
  }, [updateProgress]);

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const percent = clickX / rect.width;
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    window.scrollTo({
      top: percent * docHeight,
      behavior: 'smooth',
    });
  };

  return (
    <div
      className="scroll-progress-container"
      data-visible={isVisible}
      data-hovering={isHovering}
      onClick={handleClick}
      onMouseEnter={() => setIsHovering(true)}
      onMouseLeave={() => setIsHovering(false)}
    >
      <div className="scroll-progress-track">
        <div
          className="scroll-progress-fill"
          style={{ width: `${progress}%` }}
        />
        <div className="scroll-progress-shine" style={{ width: `${progress}%` }} />
        <div
          className="scroll-progress-thumb"
          style={{ left: `${progress}%` }}
        />
      </div>
      <div className="scroll-progress-glow" style={{ width: `${progress}%` }} />
    </div>
  );
}

export default ScrollProgressBar;
