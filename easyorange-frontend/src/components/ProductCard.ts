import type { Product as TypesProduct, ProductCondition } from '../types';
import { toast, calculateDiscount, isHotProduct, escapeHtml } from '../utils/index.js';
import { favoriteManager } from '../managers/index.js';
import * as helpers from './ProductCardHelpers.js';

export type Product = TypesProduct;

export interface ProductCardOptions {
    showActions?: boolean;
    showCategory?: boolean;
    showSeller?: boolean;
    lazyLoad?: boolean;
}

export interface ProductCardConfig {
    onCardClick?: ((product: Product) => void) | null;
    onFavoriteChange?: ((productId: number, isFavorited: boolean) => void) | null;
}

class ProductCard {
    static onCardClick: ((product: Product) => void) | null = null;
    static onFavoriteChange: ((productId: number, isFavorited: boolean) => void) | null = null;
    static _imageObserver: IntersectionObserver | null = null;
    
    static initLazyLoader(): void {
        if (ProductCard._imageObserver) {return;}
        
        ProductCard._imageObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target as HTMLImageElement;
                    if (img.dataset.src) {
                        img.src = img.dataset.src;
                        img.removeAttribute('data-src');
                        ProductCard._imageObserver?.unobserve(img);
                    }
                }
            });
        }, {
            rootMargin: '100px 0px',
            threshold: 0.01
        });
    }
    
    static create(product: Product, options: ProductCardOptions = {}): HTMLElement {
        const {
            showActions = true,
            showCategory = true,
            showSeller = true,
            lazyLoad = true
        } = options;

        if (lazyLoad) {
            ProductCard.initLazyLoader();
        }

        const card = document.createElement('div');
        card.className = 'product-card';
        card.dataset.productId = String(product.id);
        card.setAttribute('role', 'article');
        card.setAttribute('aria-label', `商品: ${product.title}`);

        const discount = calculateDiscount(product.price, product.originalPrice);
        const isHot = isHotProduct(product.views || 0);
        const isFavorited = favoriteManager.has(product.id);
        const imageUrl = product.images?.[0] || ProductCard.getDefaultImage(product.categoryId);
        const shouldLazyLoad = lazyLoad && typeof IntersectionObserver !== 'undefined';
        const relativeTime = ProductCard.getRelativeTime(product.createTime);
        const quickLocation = product.location?.trim() || '校内面交';
        const sellerName = product.sellerName || '匿名用户';
        const sellerNote = ProductCard.getSellerNote(quickLocation, relativeTime, product.sellerRating);
        const descriptionText = product.description?.trim() ||
            `${ProductCard.getConditionName(product.condition)}成色，适合校内快速交易。`;
        const priceNote = ProductCard.getPriceNote(product.price, product.originalPrice, quickLocation);

        // --- product-image section ---
        const imageDiv = document.createElement('div');
        imageDiv.className = 'product-image';

        const img = document.createElement('img');
        if (shouldLazyLoad) {
            img.dataset.src = imageUrl;
            img.src = ProductCard.getPlaceholder(product.categoryId);
        } else {
            img.src = imageUrl;
        }
        img.alt = escapeHtml(product.title);
        img.loading = lazyLoad ? 'lazy' : 'eager';
        const handleImageError = () => {
            img.removeEventListener('error', handleImageError);
            img.src = ProductCard.getDefaultImage(product.categoryId);
            img.classList.add('image-error');
        };
        img.addEventListener('error', handleImageError);
        imageDiv.appendChild(img);

        // badges
        const badgesDiv = document.createElement('div');
        badgesDiv.className = 'product-badges';

        const conditionBadge = document.createElement('span');
        conditionBadge.className = 'badge badge-condition';
        conditionBadge.textContent = `${ProductCard.getConditionIcon(product.condition)} ${ProductCard.getConditionName(product.condition)}`;
        badgesDiv.appendChild(conditionBadge);

        if (discount) {
            const discountBadge = document.createElement('span');
            discountBadge.className = 'badge badge-discount';
            discountBadge.textContent = `${discount}折`;
            badgesDiv.appendChild(discountBadge);
        }

        if (isHot) {
            const hotBadge = document.createElement('span');
            hotBadge.className = 'badge badge-hot';
            hotBadge.textContent = '热门';
            badgesDiv.appendChild(hotBadge);
        }
        imageDiv.appendChild(badgesDiv);

        const quickMetaDiv = document.createElement('div');
        quickMetaDiv.className = 'product-quick-meta';
        quickMetaDiv.appendChild(ProductCard.createMetaPill('location', quickLocation));
        quickMetaDiv.appendChild(ProductCard.createMetaPill('time', relativeTime));
        imageDiv.appendChild(quickMetaDiv);

        // favorite button
        if (showActions) {
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'product-actions';

            const favBtn = document.createElement('button');
            favBtn.className = `action-icon favorite-btn ${isFavorited ? 'favorited' : ''}`;
            favBtn.setAttribute('aria-label', isFavorited ? '取消收藏' : '收藏');
            favBtn.setAttribute('aria-pressed', String(isFavorited));
            favBtn.dataset.action = 'favorite';
            favBtn.innerHTML = ProductCard._favoriteSVG(isFavorited);
            actionsDiv.appendChild(favBtn);
            imageDiv.appendChild(actionsDiv);
        }

        card.appendChild(imageDiv);

        // --- product-info section ---
        const infoDiv = document.createElement('div');
        infoDiv.className = 'product-info';

        const eyebrowDiv = document.createElement('div');
        eyebrowDiv.className = 'product-eyebrow';

        if (showCategory) {
            const categorySpan = document.createElement('span');
            categorySpan.className = 'product-category';
            categorySpan.textContent = `${ProductCard.getCategoryIcon(product.categoryId)} ${product.categoryName || ProductCard.getCategoryName(product.categoryId)}`;
            eyebrowDiv.appendChild(categorySpan);
        }

        const signalSpan = document.createElement('span');
        signalSpan.className = `product-signal ${isHot ? 'is-hot' : ''}`;
        signalSpan.textContent = product.favorites > 0 ? `${product.favorites}人收藏` : (isHot ? '热度精选' : '校园在售');
        eyebrowDiv.appendChild(signalSpan);
        infoDiv.appendChild(eyebrowDiv);

        const titleH3 = document.createElement('h3');
        titleH3.className = 'product-title';
        titleH3.textContent = product.title;
        infoDiv.appendChild(titleH3);

        const descP = document.createElement('p');
        descP.className = 'product-description';
        descP.textContent = descriptionText;
        infoDiv.appendChild(descP);

        const statRow = document.createElement('div');
        statRow.className = 'product-stat-row';
        statRow.appendChild(ProductCard.createInfoChip('views', `${product.views || 0} 浏览`));
        statRow.appendChild(ProductCard.createInfoChip('favorite', `${product.favorites || 0} 收藏`));
        infoDiv.appendChild(statRow);

        const footerDiv = document.createElement('div');
        footerDiv.className = 'product-footer';

        const priceDiv = document.createElement('div');
        priceDiv.className = 'product-price';

        const priceRow = document.createElement('div');
        priceRow.className = 'product-price-row';

        const currentPrice = document.createElement('span');
        currentPrice.className = 'price-current';
        currentPrice.textContent = `¥${ProductCard.formatPrice(product.price)}`;
        priceRow.appendChild(currentPrice);

        if (product.originalPrice) {
            const originalPrice = document.createElement('span');
            originalPrice.className = 'price-original';
            originalPrice.textContent = `¥${ProductCard.formatPrice(product.originalPrice)}`;
            priceRow.appendChild(originalPrice);
        }
        priceDiv.appendChild(priceRow);

        const priceNoteSpan = document.createElement('span');
        priceNoteSpan.className = 'price-note';
        priceNoteSpan.textContent = priceNote;
        priceDiv.appendChild(priceNoteSpan);
        footerDiv.appendChild(priceDiv);

        if (showSeller) {
            const sellerDiv = document.createElement('div');
            sellerDiv.className = 'product-seller';

            sellerDiv.appendChild(ProductCard.createSellerAvatar(sellerName, product.sellerAvatar));

            const sellerBody = document.createElement('div');
            sellerBody.className = 'seller-body';

            const sellerSpan = document.createElement('span');
            sellerSpan.className = 'seller-name';
            sellerSpan.textContent = sellerName;
            sellerBody.appendChild(sellerSpan);

            const sellerNoteSpan = document.createElement('span');
            sellerNoteSpan.className = 'seller-note';
            sellerNoteSpan.textContent = sellerNote;
            sellerBody.appendChild(sellerNoteSpan);

            sellerDiv.appendChild(sellerBody);
            footerDiv.appendChild(sellerDiv);
        }

        infoDiv.appendChild(footerDiv);
        card.appendChild(infoDiv);

        // lazy load observer
        if (shouldLazyLoad && img.dataset.src) {
            ProductCard._imageObserver?.observe(img);
        }

        ProductCard.bindCardEvents(card, product);

        return card;
    }

    static bindCardEvents(card: HTMLElement, product: Product): void {
        card.addEventListener('click', (e) => {
            if (!(e.target as HTMLElement).closest('.action-icon')) {
                ProductCard.handleCardClick(product);
            }
        });

        const favoriteBtn = card.querySelector('.favorite-btn');
        favoriteBtn?.addEventListener('click', (e) => {
            e.stopPropagation();
            ProductCard.handleFavoriteClick(product.id, favoriteBtn as HTMLElement);
        });

        card.addEventListener('mouseenter', () => {
            ProductCard.handleCardHover(card, true);
        });

        card.addEventListener('mouseleave', () => {
            ProductCard.handleCardHover(card, false);
        });
    }

    static handleCardClick(product: Product): void {
        if (ProductCard.onCardClick) {
            ProductCard.onCardClick(product);
        } else {
            ProductCard.showProductDetail(product);
        }
    }

    static handleFavoriteClick(productId: number, btn: HTMLElement): void {
        const isFavorited = favoriteManager.toggle(productId);

        btn.classList.toggle('favorited', isFavorited);
        btn.innerHTML = helpers.getFavoriteSvg(isFavorited);

        ProductCard.showToast(
            isFavorited ? '已添加到收藏' : '已取消收藏',
            isFavorited ? 'success' : 'info'
        );

        if (ProductCard.onFavoriteChange) {
            ProductCard.onFavoriteChange(productId, isFavorited);
        }
    }

    static handleCardHover(card: HTMLElement, isHovering: boolean): void {
        card.classList.toggle('card-hover-elevated', isHovering);
    }

    static showProductDetail(product: Product): void {
        const event = new CustomEvent<Product>('productDetail', { 
            detail: product,
            bubbles: true
        });
        document.dispatchEvent(event);
    }

    static createSkeleton(): HTMLElement {
        const skeleton = document.createElement('div');
        skeleton.className = 'skeleton-card';

        const imageDiv = document.createElement('div');
        imageDiv.className = 'skeleton-image';
        skeleton.appendChild(imageDiv);

        const contentDiv = document.createElement('div');
        contentDiv.className = 'skeleton-content';

        const lineLengths = ['short', 'medium', 'full'];
        lineLengths.forEach(cls => {
            const line = document.createElement('div');
            line.className = `skeleton-line ${cls}`;
            contentDiv.appendChild(line);
        });

        skeleton.appendChild(contentDiv);
        return skeleton;
    }

    static createSkeletonGrid(count = 8): DocumentFragment {
        const fragment = document.createDocumentFragment();
        for (let i = 0; i < count; i++) {
            fragment.appendChild(ProductCard.createSkeleton());
        }
        return fragment;
    }

    static getConditionName(condition: ProductCondition): string {
        return helpers.getConditionName(condition);
    }

    static getConditionIcon(condition: ProductCondition): string {
        return helpers.getConditionIcon(condition);
    }

    static getCategoryIcon(categoryId: number): string {
        return helpers.getCategoryIcon(categoryId);
    }

    static getCategoryName(categoryId: number): string {
        return helpers.getCategoryName(categoryId);
    }

    static getDefaultImage(categoryId: number): string {
        return helpers.getDefaultImage(categoryId);
    }

    static getPlaceholder(_categoryId: number): string {
        return helpers.getPlaceholder();
    }

    static formatPrice(price: number | string): string {
        return helpers.formatPrice(typeof price === 'string' ? parseFloat(price) : price);
    }

    static getRelativeTime(date: string): string {
        return helpers.getRelativeTime(date);
    }

    static getSellerNote(location: string, relativeTime: string, rating?: number): string {
        return helpers.getSellerNote(location, relativeTime, rating);
    }

    static getPriceNote(price: number, originalPrice: number | null, location: string): string {
        return helpers.getPriceNote(price, originalPrice, location);
    }

    static createMetaPill(type: 'location' | 'time', text: string): HTMLElement {
        return helpers.createMetaPill(type, text);
    }

    static createInfoChip(type: 'views' | 'favorite', text: string): HTMLElement {
        return helpers.createInfoChip(type, text);
    }

    static createSellerAvatar(name: string, avatarUrl: string | null): HTMLElement {
        return helpers.createSellerAvatar(name, avatarUrl);
    }

    private static _favoriteSVG(filled: boolean): string {
        return helpers.getFavoriteSvg(filled);
    }

    static showToast(message: string, type: 'success' | 'error' | 'info' | 'warning' = 'info'): void {
        toast.show(message, type);
    }

    static configure(options: ProductCardConfig): void {
        if (options.onCardClick) {
            ProductCard.onCardClick = options.onCardClick;
        }
        if (options.onFavoriteChange) {
            ProductCard.onFavoriteChange = options.onFavoriteChange;
        }
    }

    static destroy(): void {
        if (ProductCard._imageObserver) {
            ProductCard._imageObserver.disconnect();
            ProductCard._imageObserver = null;
        }
    }
}

export default ProductCard;
