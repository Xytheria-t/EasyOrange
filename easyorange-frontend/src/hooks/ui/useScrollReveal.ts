import { useEffect } from 'react';

export function useScrollReveal(threshold = 0.15) {
    useEffect(() => {
        const revealElements = document.querySelectorAll(
            '.reveal, .reveal-scale, .reveal-left, .reveal-right, .reveal-stagger'
        );

        const observer = new IntersectionObserver(
            entries => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('revealed');
                        observer.unobserve(entry.target);
                    }
                });
            },
            {
                threshold,
                rootMargin: '0px 0px -50px 0px',
            }
        );

        revealElements.forEach(el => observer.observe(el));

        return () => observer.disconnect();
    }, [threshold]);
}
