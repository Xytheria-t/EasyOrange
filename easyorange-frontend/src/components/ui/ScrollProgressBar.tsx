import { useEffect, useState } from 'react';

function ScrollProgressBar() {
    const [progress, setProgress] = useState(0);
    const [visible, setVisible] = useState(false);
    const [isHovering, setIsHovering] = useState(false);

    useEffect(() => {
        let raf = 0;
        const update = () => {
            const scrollTop = window.scrollY;
            const docHeight = document.documentElement.scrollHeight - window.innerHeight;
            setVisible(scrollTop > 20);
            setProgress(docHeight <= 0 ? 0 : Math.min((scrollTop / docHeight) * 100, 100));
        };
        const onScroll = () => {
            cancelAnimationFrame(raf);
            raf = requestAnimationFrame(update);
        };
        update();
        window.addEventListener('scroll', onScroll, { passive: true });
        window.addEventListener('resize', onScroll, { passive: true });
        return () => {
            cancelAnimationFrame(raf);
            window.removeEventListener('scroll', onScroll);
            window.removeEventListener('resize', onScroll);
        };
    }, []);

    const scrollToPercent = (percent: number) => {
        const docHeight = document.documentElement.scrollHeight - window.innerHeight;
        window.scrollTo({
            top: Math.max(0, Math.min(1, percent)) * docHeight,
            behavior: 'smooth',
        });
    };

    const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
        const rect = e.currentTarget.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        scrollToPercent(clickX / rect.width);
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
        const step = 10;
        let newPercent = progress;

        switch (e.key) {
            case 'ArrowRight':
            case 'ArrowDown':
                e.preventDefault();
                newPercent = Math.min(100, progress + step);
                break;
            case 'ArrowLeft':
            case 'ArrowUp':
                e.preventDefault();
                newPercent = Math.max(0, progress - step);
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
            className="scroll-progress-container"
            style={{ '--progress': `${progress}%` } as React.CSSProperties}
            data-visible={visible}
            data-hovering={isHovering}
            role="progressbar"
            aria-label="页面滚动进度"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(progress)}
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
