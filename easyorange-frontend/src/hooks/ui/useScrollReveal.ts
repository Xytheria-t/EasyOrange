import { type RefObject, useEffect, useRef } from 'react';

const REVEAL_SELECTOR = '.reveal, .reveal-scale, .reveal-left, .reveal-right, .reveal-stagger';

/**
 * 滚动浮现 — 将返回值 ref 挂到组件根元素，观察其内部 `.reveal*` 元素，
 * 进入视口时加 `.revealed` 类触发 CSS 动画。
 *
 * 相比旧版全局 {@code document.querySelectorAll}，改为 scope 到组件自身根，
 * 避免 hook 与整个文档的 class 命名耦合（markup/CSS 漂移时不会误伤其他区域）。
 *
 * threshold 默认 0：命中任一点即显现。用比例阈值时，若目标比视口高得多，
 * 页面停在最底部（只剩一条边可见）时比例永远达不到，元素会一直停在 opacity:0。
 */
export function useScrollReveal(threshold = 0): RefObject<HTMLElement | null> {
    const rootRef = useRef<HTMLElement | null>(null);

    useEffect(() => {
        const root = rootRef.current ?? document;
        const revealElements = root.querySelectorAll(REVEAL_SELECTOR);

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

        revealElements.forEach(el => {
            observer.observe(el);
        });

        return () => observer.disconnect();
    }, [threshold]);

    return rootRef;
}
