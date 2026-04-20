/**
 * @fileoverview Shared motion controller
 * @description Coordinates header scroll state and reveal animations.
 */

import { throttle } from '../utils/index.js';

type RevealVariant = 'up' | 'soft' | 'card';

const REVEAL_SELECTORS = [
    '.hero-editorial-grid',
    '.market-hero',
    '.section-header',
    '.section-note-card',
    '.signature-chip',
    '.profile-note-card',
    '.category-card',
    '.products-filter',
    '.products-toolbar',
    '.market-panel-signal',
    '.quick-filter-btn',
    '.curator-note-card',
    '.curator-profile-card',
    '.curator-stats-card',
    '.active-filters',
    '.product-card',
    '.products-more',
    '.recent-history',
    '.history-item',
    '.footer-main',
    '.footer-bottom'
].join(', ');

export class MotionController {
    private header: HTMLElement | null = null;
    private initialized = false;
    private revealObserver: IntersectionObserver | null = null;
    private mutationObserver: MutationObserver | null = null;
    private scrollHandler: (() => void) | null = null;
    private reduceMotionMedia: MediaQueryList | null = null;

    init(): void {
        if (this.initialized) {return;}

        this.header = document.querySelector('.unified-header');
        this.reduceMotionMedia = window.matchMedia('(prefers-reduced-motion: reduce)');
        document.documentElement.classList.add('motion-enabled');

        this.initHeaderMotion();
        this.initRevealMotion();
        this.initialized = true;
    }

    refresh(root: ParentNode = document): void {
        const targets = this.collectTargets(root);
        targets.forEach((element, index) => this.prepareTarget(element, index));
    }

    destroy(): void {
        if (this.scrollHandler) {
            window.removeEventListener('scroll', this.scrollHandler);
            this.scrollHandler = null;
        }

        this.revealObserver?.disconnect();
        this.revealObserver = null;

        this.mutationObserver?.disconnect();
        this.mutationObserver = null;

        this.header = null;
        this.initialized = false;
    }

    private initHeaderMotion(): void {
        if (!this.header) {return;}

        const applyHeaderState = () => {
            this.header?.classList.toggle('scrolled', window.scrollY > 18);
        };

        this.scrollHandler = throttle(applyHeaderState, 60);
        window.addEventListener('scroll', this.scrollHandler, { passive: true });
        applyHeaderState();
    }

    private initRevealMotion(): void {
        const prefersReducedMotion = this.reduceMotionMedia?.matches ?? false;

        if (prefersReducedMotion || typeof IntersectionObserver === 'undefined') {
            this.refresh(document);
            document.querySelectorAll<HTMLElement>('.motion-reveal').forEach(element => {
                this.reveal(element);
            });
            return;
        }

        this.revealObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (!entry.isIntersecting) {return;}
                const element = entry.target as HTMLElement;
                this.reveal(element);
                this.revealObserver?.unobserve(element);
            });
        }, {
            rootMargin: '0px 0px -8% 0px',
            threshold: 0.12
        });

        this.refresh(document);
        this.initMutationObserver();
    }

    private initMutationObserver(): void {
        this.mutationObserver = new MutationObserver((mutations) => {
            mutations.forEach(mutation => {
                mutation.addedNodes.forEach(node => {
                    if (!(node instanceof HTMLElement)) {return;}
                    this.refresh(node);
                });
            });
        });

        this.mutationObserver.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    private collectTargets(root: ParentNode): HTMLElement[] {
        const elements = new Set<HTMLElement>();

        if (root instanceof HTMLElement && root.matches(REVEAL_SELECTORS)) {
            elements.add(root);
        }

        root.querySelectorAll<HTMLElement>(REVEAL_SELECTORS).forEach(element => {
            elements.add(element);
        });

        return Array.from(elements);
    }

    private prepareTarget(element: HTMLElement, index: number): void {
        if (element.dataset.motionReady === 'true') {return;}

        const prefersReducedMotion = this.reduceMotionMedia?.matches ?? false;

        element.dataset.motionReady = 'true';
        element.dataset.motion = this.getVariant(element);
        element.style.setProperty('--motion-delay', `${Math.min(index % 6, 5) * 60}ms`);
        element.classList.add('motion-reveal');

        requestAnimationFrame(() => {
            element.classList.add('motion-ready');
            if (prefersReducedMotion) {
                this.reveal(element);
                return;
            }
            this.revealObserver?.observe(element);
        });
    }

    private reveal(element: HTMLElement): void {
        element.classList.add('motion-revealed');
    }

    private getVariant(element: HTMLElement): RevealVariant {
        if (element.matches('.product-card, .category-card, .profile-note-card, .signature-chip, .market-panel-signal, .curator-note-card, .curator-profile-card, .curator-stats-card, .recent-history, .history-item')) {
            return 'card';
        }

        if (element.matches('.footer-main, .footer-bottom, .products-filter, .products-toolbar, .products-more, .active-filters, .section-header, .market-hero')) {
            return 'soft';
        }

        return 'up';
    }
}

export const motionController = new MotionController();
export default motionController;
