/**
 * @fileoverview ProductCard 辅助函数模块
 * @description 提供商品卡片渲染所需的纯函数辅助方法
 */

import { escapeHtml, formatRelativeTime, calculateDiscount, isHotProduct } from '../utils/index.js';

export function getDefaultImage(categoryId?: number): string {
    const seed = categoryId || Math.floor(Math.random() * 1000);
    return `https://picsum.photos/seed/${seed}/400/400`;
}

export function getPlaceholder(): string {
    return `data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 400'%3E%3Crect fill='%23f5f5f5' width='400' height='400'/%3E%3Crect fill='%23e0e0e0' x='150' y='150' width='100' height='100' rx='8'/%3E%3C/svg%3E`;
}

const CATEGORY_ICONS: Record<number, string> = {
    1: '📚', 2: '💻', 3: '👕', 4: '🎮', 5: '📱',
    6: '🎵', 7: '🚲', 8: '✏️', 9: '🏠', 10: '其他'
};

const CATEGORY_NAMES: Record<number, string> = {
    1: '书籍教材', 2: '数码电子', 3: '服饰鞋包', 4: '游戏装备', 5: '手机平板',
    6: '影音娱乐', 7: '运动户外', 8: '文具办公', 9: '生活用品', 10: '其他'
};

export function getCategoryIcon(categoryId?: number): string {
    if (!categoryId) {return '📦';}
    return CATEGORY_ICONS[categoryId] || '📦';
}

export function getCategoryName(categoryId?: number): string {
    if (!categoryId) {return '其他';}
    return CATEGORY_NAMES[categoryId] || '其他';
}

export function getConditionIcon(condition: string): string {
    const iconMap: Record<string, string> = {
        'NEW': '🆕', 'LIKE_NEW': '✨', 'GOOD': '👍', 'FAIR': '👌', 'POOR': '📉'
    };
    return iconMap[condition] || '👍';
}

export function getConditionName(condition: string): string {
    return condition || '良好';
}

export function getSellerRating(rating?: number): string {
    if (!rating || rating <= 0) {return '';}
    return `⭐ ${rating.toFixed(1)}`;
}

export function getHotnessLevel(views?: number): 'normal' | 'warm' | 'hot' | 'trending' {
    const count = views || 0;
    if (count >= 500) {return 'trending';}
    if (count >= 100) {return 'hot';}
    if (count >= 20) {return 'warm';}
    return 'normal';
}

export function formatPrice(price: number): string {
    return price.toFixed(2);
}

export function getRelativeTime(date?: string): string {
    return date ? formatRelativeTime(date) : '刚刚';
}

export function getSellerInitial(name?: string): string {
    if (!name) {return 'U';}
    const trimmed = name.trim();
    return trimmed ? trimmed.charAt(0).toUpperCase() : 'U';
}

export function getSellerNote(location?: string, relativeTime?: string, rating?: number): string {
    if (rating && rating > 0) {
        return `${location || '校内'} · 信誉 ${rating.toFixed(1)}`;
    }
    return `${location || '校内面交'} · ${relativeTime || '刚刚'}`;
}

export function getPriceNote(price?: number, originalPrice?: number | null, location?: string): string {
    if (originalPrice && originalPrice > (price || 0)) {
        return `较原价省 ¥${formatPrice(originalPrice - (price || 0))}`;
    }
    return `${location || '校内'} 可当面验货`;
}

export function createMetaPill(type: 'location' | 'time', text: string): HTMLElement {
    const pill = document.createElement('span');
    pill.className = 'product-quick-pill';
    pill.innerHTML = getIconSvg(type);
    const textSpan = document.createElement('span');
    textSpan.textContent = text;
    pill.appendChild(textSpan);
    return pill;
}

export function createInfoChip(type: 'views' | 'favorite', text: string): HTMLElement {
    const chip = document.createElement('span');
    chip.className = 'product-info-chip';
    chip.innerHTML = getIconSvg(type);
    const textSpan = document.createElement('span');
    textSpan.textContent = text;
    chip.appendChild(textSpan);
    return chip;
}

export function createSellerAvatar(name: string, avatarUrl: string | null | undefined): HTMLElement {
    const avatar = document.createElement('div');
    avatar.className = 'seller-avatar';

    if (avatarUrl) {
        const img = document.createElement('img');
        img.src = avatarUrl;
        img.alt = `${name}头像`;
        img.loading = 'lazy';
        img.addEventListener('error', () => {
            avatar.innerHTML = '';
            avatar.appendChild(createSellerAvatarFallback(name));
        }, { once: true });
        avatar.appendChild(img);
        return avatar;
    }

    avatar.appendChild(createSellerAvatarFallback(name));
    return avatar;
}

export function createSellerAvatarFallback(name: string): HTMLElement {
    const fallback = document.createElement('span');
    fallback.className = 'seller-avatar-fallback';
    fallback.textContent = getSellerInitial(name);
    return fallback;
}

export function getIconSvg(type: 'location' | 'time' | 'views' | 'favorite'): string {
    switch (type) {
        case 'location':
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">' +
                '<path d="M12 21s7-4.35 7-11a7 7 0 1 0-14 0c0 6.65 7 11 7 11Z"/>' +
                '<circle cx="12" cy="10" r="2.5"/></svg>';
        case 'time':
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">' +
                '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>';
        case 'views':
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">' +
                '<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z"/>' +
                '<circle cx="12" cy="12" r="3"/></svg>';
        case 'favorite':
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">' +
                '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>';
    }
}

export function getFavoriteSvg(filled: boolean): string {
    if (filled) {
        return `<svg viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
        </svg>`;
    }
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
    </svg>`;
}

export { calculateDiscount, isHotProduct, escapeHtml, formatRelativeTime };
