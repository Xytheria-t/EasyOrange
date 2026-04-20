/**
 * @fileoverview 首页动画模块
 * @description 处理页面动画效果，包括计数器动画、滚动动画、悬浮光效、收藏心跳等
 */

import { dom, throttle } from '../../utils/index.js';

/** 动画模块 DOM 元素 */
interface AnimationElements {
    statValues: HTMLElement[];
    navbar: HTMLElement | null;
    backToTop: HTMLElement | null;
    productCards: HTMLElement[];
    header: HTMLElement | null;
    cursorGlow: HTMLElement | null;
    favoriteBtns: HTMLElement[];
}

/** 计数器定时器引用 */
interface AnimationTimers {
    counterTimers: ReturnType<typeof setInterval>[];
    scrollHandler: (() => void) | null;
    backToTopHandler: (() => void) | null;
    clickHandler: (() => void) | null;
    mouseMoveHandler: ((e: MouseEvent) => void) | null;
    heartbeatHandler: ((e: MouseEvent) => void) | null;
}

/**
 * 动画管理器
 */
export class AnimationManager {
    private elements: AnimationElements;
    private initialized: boolean;
    private timers: AnimationTimers;

    constructor() {
        this.elements = {
            statValues: [],
            navbar: null,
            backToTop: null,
            productCards: [],
            header: null,
            cursorGlow: null,
            favoriteBtns: []
        };
        this.timers = {
            counterTimers: [],
            scrollHandler: null,
            backToTopHandler: null,
            clickHandler: null,
            mouseMoveHandler: null,
            heartbeatHandler: null
        };
        this.initialized = false;
    }

    /**
     * 初始化动画模块
     */
    init(): void {
        if (this.initialized) {return;}

        this.cacheElements();
        this.animateCounters();
        this.initScrollAnimations();
        this.initBackToTop();
        this.initStaggerAnimation();
        this.initMouseGlow();
        this.initHeartBeat();
        this.initHeaderScroll();
        this.initialized = true;
    }

    /**
     * 缓存 DOM 元素
     */
    private cacheElements(): void {
        this.elements = {
            statValues: dom.getAll('.stat-value[data-count]'),
            navbar: dom.get('.navbar-nav'),
            backToTop: dom.get('#backToTop'),
            productCards: dom.getAll('.product-card'),
            header: dom.get('.unified-header'),
            cursorGlow: dom.get('.cursor-glow'),
            favoriteBtns: dom.getAll('.action-icon.favorited')
        };
    }

    /**
     * 动画计数器
     */
    animateCounters(): void {
        const { statValues } = this.elements;

        if (!statValues || statValues.length === 0) {return;}

        const animateValue = (element: HTMLElement, start: number, end: number, duration: number): void => {
            const range = end - start;
            const increment = range / (duration / 16);
            let current = start;
            const timer = setInterval(() => {
                current += increment;
                if (current >= end) {
                    current = end;
                    clearInterval(timer);
                }
                element.textContent = Math.floor(current).toLocaleString();
            }, 16);
            this.timers.counterTimers.push(timer);
        };

        statValues.forEach(element => {
            const targetValue = parseInt(element.dataset.count || '0', 10);
            if (targetValue > 0) {
                animateValue(element, 0, targetValue, 2000);
            }
        });
    }

    /**
     * 初始化滚动动画
     */
    private initScrollAnimations(): void {
        const { navbar } = this.elements;

        if (!navbar) {return;}

        const handler = throttle(() => {
            if (window.scrollY > 50) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
        }, 50);

        this.timers.scrollHandler = handler;
        window.addEventListener('scroll', handler, { passive: true });
    }

    /**
     * 初始化 Header 滚动变化
     */
    private initHeaderScroll(): void {
        const { header } = this.elements;
        if (!header) {return;}

        const handler = throttle(() => {
            if (window.scrollY > 100) {
                header.classList.add('header--scrolled');
            } else {
                header.classList.remove('header--scrolled');
            }
        }, 50);
        window.addEventListener('scroll', handler, { passive: true });
    }

    /**
     * 初始化交错入场动画
     */
    private initStaggerAnimation(): void {
        const { productCards } = this.elements;
        if (!productCards || productCards.length === 0) {return;}

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    const card = entry.target as HTMLElement;
                    const index = Array.from(this.elements.productCards!).indexOf(card);
                    card.style.setProperty('--stagger-delay', `${index * 60}ms`);
                    card.classList.add('stagger-enter');
                    observer.unobserve(card);
                }
            });
        }, { threshold: 0.1 });

        productCards.forEach((card, index) => {
            (card as HTMLElement).style.setProperty('--stagger-index', String(index));
            observer.observe(card);
        });
    }

    /**
     * 初始化鼠标跟随光效
     */
    private initMouseGlow(): void {
        const { cursorGlow } = this.elements;
        if (!cursorGlow) {return;}

        let rafId: number | null = null;
        const handler = (e: MouseEvent) => {
            if (rafId) {cancelAnimationFrame(rafId);}
            rafId = requestAnimationFrame(() => {
                cursorGlow.style.transform = `translate(${e.clientX}px, ${e.clientY}px)`;
            });
        };
        this.timers.mouseMoveHandler = handler;
        document.addEventListener('mousemove', handler);
    }

    /**
     * 初始化收藏心跳动画
     */
    private initHeartBeat(): void {
        const { favoriteBtns } = this.elements;
        favoriteBtns.forEach(btn => {
            btn.classList.add('heartbeat');
            setTimeout(() => {
                btn.classList.remove('heartbeat');
            }, 600);
        });

        const handler = (e: MouseEvent) => {
            const target = e.target as HTMLElement;
            const favoriteBtn = target.closest('.action-icon[data-favorite]');
            if (favoriteBtn) {
                favoriteBtn.classList.add('heartbeat');
                setTimeout(() => {
                    favoriteBtn.classList.remove('heartbeat');
                }, 600);
            }
        };
        this.timers.heartbeatHandler = handler;
        document.addEventListener('click', handler);
    }

    /**
     * 初始化返回顶部按钮
     */
    private initBackToTop(): void {
        const { backToTop } = this.elements;

        if (!backToTop) {return;}

        // 点击返回顶部
        const clickHandler = () => {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        };
        this.timers.clickHandler = clickHandler;
        backToTop.addEventListener('click', clickHandler);

        // 滚动时显示/隐藏
        const scrollHandler = throttle(() => {
            if (window.scrollY > 500) {
                backToTop.classList.add('visible');
            } else {
                backToTop.classList.remove('visible');
            }
        }, 100);
        this.timers.backToTopHandler = scrollHandler;
        window.addEventListener('scroll', scrollHandler, { passive: true });
    }

    /**
     * 销毁模块，清理资源
     */
    destroy(): void {
        // 清理所有计数器
        this.timers.counterTimers.forEach(timer => clearInterval(timer));
        this.timers.counterTimers = [];

        // 移除滚动事件监听
        if (this.timers.scrollHandler) {
            window.removeEventListener('scroll', this.timers.scrollHandler);
            this.timers.scrollHandler = null;
        }
        if (this.timers.backToTopHandler) {
            window.removeEventListener('scroll', this.timers.backToTopHandler);
            this.timers.backToTopHandler = null;
        }
        if (this.timers.mouseMoveHandler) {
            document.removeEventListener('mousemove', this.timers.mouseMoveHandler);
            this.timers.mouseMoveHandler = null;
        }
        if (this.timers.heartbeatHandler) {
            document.removeEventListener('click', this.timers.heartbeatHandler);
            this.timers.heartbeatHandler = null;
        }

        // 移除点击事件监听
        if (this.timers.clickHandler && this.elements.backToTop) {
            this.elements.backToTop.removeEventListener('click', this.timers.clickHandler);
            this.timers.clickHandler = null;
        }

        this.initialized = false;
        this.elements = {
            statValues: [],
            navbar: null,
            backToTop: null,
            productCards: [],
            header: null,
            cursorGlow: null,
            favoriteBtns: []
        };
    }
}

export default new AnimationManager();
