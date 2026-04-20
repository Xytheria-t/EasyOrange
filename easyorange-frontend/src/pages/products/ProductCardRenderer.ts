/**
 * @fileoverview 商品卡片渲染器
 */

import { escapeHtml, calculateDiscount, isHotProduct, formatRelativeTime } from '../../utils/index.js';
import { favoriteManager } from '../../managers/index.js';
import type { ProductListItem } from './types.js';

export class ProductCardRenderer {
    private onCardClick?: (product: ProductListItem) => void;
    private onCompareToggle?: (product: ProductListItem) => void;
    private onFavoriteToggle?: (productId: number, btn: HTMLElement) => void;
    private isCompareSelected?: (productId: number) => boolean;

    constructor(options: {
        onCardClick?: (product: ProductListItem) => void;
        onCompareToggle?: (product: ProductListItem) => void;
        onFavoriteToggle?: (productId: number, btn: HTMLElement) => void;
        isCompareSelected?: (productId: number) => boolean;
    } = {}) {
        this.onCardClick = options.onCardClick;
        this.onCompareToggle = options.onCompareToggle;
        this.onFavoriteToggle = options.onFavoriteToggle;
        this.isCompareSelected = options.isCompareSelected;
    }

    create(product: ProductListItem, index: number): HTMLElement {
        const card = document.createElement('div');
        card.className = 'product-card';
        card.dataset.productId = String(product.id);
        card.style.animationDelay = `${index * 30}ms`;
        card.setAttribute('role', 'article');
        card.setAttribute('aria-label', `商品: ${product.name}`);

        const discount = calculateDiscount(product.price, product.originalPrice);
        const isFavorited = favoriteManager.has(product.id);
        const isHot = isHotProduct(product.viewCount);
        const compareSelected = this.isCompareSelected?.(product.id);
        const relativeTime = product.createTime ? formatRelativeTime(product.createTime) : '刚刚';
        const quickLocation = product.location?.trim() || '校内面交';
        const sellerName = product.sellerName || '匿名卖家';
        const descriptionText = product.description?.trim() ||
            `${product.conditionName}成色，适合校内快速交易。`;
        const priceNote = ProductCardRenderer.getPriceNote(product.price, product.originalPrice, quickLocation);
        if (compareSelected) {card.classList.add('compare-selected');}

        // Image section
        const imageDiv = document.createElement('div');
        imageDiv.className = 'product-image';

        const img = document.createElement('img');
        img.src = product.images?.[0] || ProductCardRenderer.getDefaultImage(product.id);
        img.alt = escapeHtml(product.name);
        img.loading = 'lazy';
        img.addEventListener('error', () => {
            img.src = ProductCardRenderer.getDefaultImage(product.id);
            img.classList.add('image-error');
        }, { once: true });
        imageDiv.appendChild(img);

        // Badges
        const badgesDiv = document.createElement('div');
        badgesDiv.className = 'product-badges';

        const condBadge = document.createElement('span');
        condBadge.className = 'badge badge-condition';
        condBadge.textContent = `${product.conditionIcon} ${product.conditionName}`;
        badgesDiv.appendChild(condBadge);

        if (discount) {
            const d = document.createElement('span');
            d.className = 'badge badge-discount';
            d.textContent = `${discount}折`;
            badgesDiv.appendChild(d);
        }
        if (isHot) {
            const h = document.createElement('span');
            h.className = 'badge badge-hot';
            h.textContent = '🔥 热门';
            badgesDiv.appendChild(h);
        }
        imageDiv.appendChild(badgesDiv);

        const quickMetaDiv = document.createElement('div');
        quickMetaDiv.className = 'product-quick-meta';
        quickMetaDiv.appendChild(ProductCardRenderer.createMetaPill('location', quickLocation));
        quickMetaDiv.appendChild(ProductCardRenderer.createMetaPill('time', relativeTime));
        imageDiv.appendChild(quickMetaDiv);

        // Compare checkbox
        const compareDiv = document.createElement('div');
        compareDiv.className = 'compare-checkbox';
        compareDiv.dataset.action = 'compare';
        compareDiv.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">' +
            '<polyline points="20 6 9 17 4 12"/></svg>';
        card.appendChild(compareDiv);

        // Favorite button
        const actionsDiv = document.createElement('div');
        actionsDiv.className = 'product-actions';
        const favBtn = document.createElement('button');
        favBtn.className = `action-icon favorite-btn ${isFavorited ? 'favorited' : ''}`;
        favBtn.setAttribute('aria-label', isFavorited ? '取消收藏' : '收藏');
        favBtn.innerHTML = `<svg viewBox="0 0 24 24" fill="${isFavorited ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">` +
            '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>' +
            '</svg>';
        actionsDiv.appendChild(favBtn);
        imageDiv.appendChild(actionsDiv);
        card.appendChild(imageDiv);

        // Info section
        const infoDiv = document.createElement('div');
        infoDiv.className = 'product-info';

        const eyebrowDiv = document.createElement('div');
        eyebrowDiv.className = 'product-eyebrow';

        const catSpan = document.createElement('span');
        catSpan.className = 'product-category';
        catSpan.textContent = `${product.categoryIcon || '📦'} ${product.categoryName || '其他'}`;
        eyebrowDiv.appendChild(catSpan);

        const signalSpan = document.createElement('span');
        signalSpan.className = `product-signal ${isHot ? 'is-hot' : ''}`;
        signalSpan.textContent = product.viewCount > 0 ? `${product.viewCount}次浏览` : '新上架';
        eyebrowDiv.appendChild(signalSpan);
        infoDiv.appendChild(eyebrowDiv);

        const title = document.createElement('h3');
        title.className = 'product-title';
        title.textContent = product.name;
        infoDiv.appendChild(title);

        const desc = document.createElement('p');
        desc.className = 'product-description';
        desc.textContent = descriptionText;
        infoDiv.appendChild(desc);

        const statRow = document.createElement('div');
        statRow.className = 'product-stat-row';
        statRow.appendChild(ProductCardRenderer.createInfoChip('views', `${product.viewCount || 0} 浏览`));
        statRow.appendChild(ProductCardRenderer.createInfoChip('favorite', isFavorited ? '已收藏' : '可收藏'));
        infoDiv.appendChild(statRow);

        const footerDiv = document.createElement('div');
        footerDiv.className = 'product-footer';

        const priceDiv = document.createElement('div');
        priceDiv.className = 'product-price';

        const priceRow = document.createElement('div');
        priceRow.className = 'product-price-row';
        const currentPrice = document.createElement('span');
        currentPrice.className = 'price-current';
        currentPrice.textContent = `¥${ProductCardRenderer.formatPrice(product.price)}`;
        priceRow.appendChild(currentPrice);
        if (product.originalPrice) {
            const origPrice = document.createElement('span');
            origPrice.className = 'price-original';
            origPrice.textContent = `¥${ProductCardRenderer.formatPrice(product.originalPrice)}`;
            priceRow.appendChild(origPrice);
        }
        priceDiv.appendChild(priceRow);

        const priceNoteSpan = document.createElement('span');
        priceNoteSpan.className = 'price-note';
        priceNoteSpan.textContent = priceNote;
        priceDiv.appendChild(priceNoteSpan);
        footerDiv.appendChild(priceDiv);

        const sellerDiv = document.createElement('div');
        sellerDiv.className = 'product-seller';
        sellerDiv.appendChild(ProductCardRenderer.createSellerAvatar(sellerName, product.sellerAvatar));

        const sellerBody = document.createElement('div');
        sellerBody.className = 'seller-body';
        const sellerSpan = document.createElement('span');
        sellerSpan.className = 'seller-name';
        sellerSpan.textContent = sellerName;
        sellerBody.appendChild(sellerSpan);

        const sellerNoteSpan = document.createElement('span');
        sellerNoteSpan.className = 'seller-note';
        sellerNoteSpan.textContent = `${quickLocation} · ${relativeTime}`;
        sellerBody.appendChild(sellerNoteSpan);

        sellerDiv.appendChild(sellerBody);
        footerDiv.appendChild(sellerDiv);

        infoDiv.appendChild(footerDiv);
        card.appendChild(infoDiv);

        // Events
        card.addEventListener('click', (e: MouseEvent) => {
            const target = e.target as HTMLElement;
            if (target.closest('.compare-checkbox')) {
                e.stopPropagation();
                this.onCompareToggle?.(product);
                return;
            }
            if (!target.closest('.action-icon')) {
                this.onCardClick?.(product);
            }
        });

        favBtn.addEventListener('click', (e: MouseEvent) => {
            e.stopPropagation();
            this.onFavoriteToggle?.(product.id, favBtn);
        });

        return card;
    }

    createSkeleton(): HTMLElement {
        const skeleton = document.createElement('div');
        skeleton.className = 'skeleton-card';
        skeleton.appendChild(document.createElement('div')).className = 'skeleton-image';
        const content = document.createElement('div');
        content.className = 'skeleton-content';
        ['short', 'medium', 'full'].forEach(cls => {
            const line = document.createElement('div');
            line.className = `skeleton-line ${cls}`;
            content.appendChild(line);
        });
        skeleton.appendChild(content);
        return skeleton;
    }

    renderSkeletonGrid(container: HTMLElement, count = 8): void {
        container.innerHTML = '';
        for (let i = 0; i < count; i++) {container.appendChild(this.createSkeleton());}
    }

    private static createMetaPill(type: 'location' | 'time', text: string): HTMLElement {
        const pill = document.createElement('span');
        pill.className = 'product-quick-pill';
        pill.innerHTML = ProductCardRenderer.getIconSvg(type);
        const textSpan = document.createElement('span');
        textSpan.textContent = text;
        pill.appendChild(textSpan);
        return pill;
    }

    private static createInfoChip(type: 'views' | 'favorite', text: string): HTMLElement {
        const chip = document.createElement('span');
        chip.className = 'product-info-chip';
        chip.innerHTML = ProductCardRenderer.getIconSvg(type);
        const textSpan = document.createElement('span');
        textSpan.textContent = text;
        chip.appendChild(textSpan);
        return chip;
    }

    private static createSellerAvatar(name: string, avatarUrl: string | null): HTMLElement {
        const avatar = document.createElement('div');
        avatar.className = 'seller-avatar';

        if (avatarUrl) {
            const img = document.createElement('img');
            img.src = avatarUrl;
            img.alt = `${name}头像`;
            img.loading = 'lazy';
            img.addEventListener('error', () => {
                avatar.innerHTML = '';
                avatar.appendChild(ProductCardRenderer.createSellerAvatarFallback(name));
            }, { once: true });
            avatar.appendChild(img);
            return avatar;
        }

        avatar.appendChild(ProductCardRenderer.createSellerAvatarFallback(name));
        return avatar;
    }

    private static createSellerAvatarFallback(name: string): HTMLElement {
        const fallback = document.createElement('span');
        fallback.className = 'seller-avatar-fallback';
        fallback.textContent = ProductCardRenderer.getSellerInitial(name);
        return fallback;
    }

    private static getDefaultImage(seed: number): string {
        return `https://picsum.photos/seed/${seed}/400/400`;
    }

    private static getSellerInitial(name: string): string {
        const trimmed = name.trim();
        return trimmed ? trimmed.charAt(0).toUpperCase() : 'U';
    }

    private static getPriceNote(price: number, originalPrice: number | null, location: string): string {
        if (originalPrice && originalPrice > price) {
            return `较原价省 ¥${ProductCardRenderer.formatPrice(originalPrice - price)}`;
        }
        return `${location} 可现场确认`;
    }

    private static formatPrice(price: number): string {
        return price.toFixed(2);
    }

    private static getIconSvg(type: 'location' | 'time' | 'views' | 'favorite'): string {
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
}
