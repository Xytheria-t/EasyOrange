/**
 * @fileoverview 商品详情弹窗管理器
 */

import api from '../../api/index.js';
import { toast, formatRelativeTime, escapeHtml } from '../../utils/index.js';
import { favoriteManager } from '../../managers/index.js';
import type { ProductListItem } from './types.js';

interface ModalElements {
    productDetailModal: HTMLElement | null;
    modalOverlay: HTMLElement | null;
    modalClose: HTMLElement | null;
    modalMainImage: HTMLImageElement | null;
    galleryPrev: HTMLElement | null;
    galleryNext: HTMLElement | null;
    galleryCounter: HTMLElement | null;
    galleryThumbnails: HTMLElement | null;
    modalCondition: HTMLElement | null;
    modalDiscount: HTMLElement | null;
    modalTitle: HTMLElement | null;
    modalPrice: HTMLElement | null;
    modalOriginalPrice: HTMLElement | null;
    modalLocation: HTMLElement | null;
    modalDate: HTMLElement | null;
    modalViews: HTMLElement | null;
    modalDescription: HTMLElement | null;
    modalSellerAvatar: HTMLImageElement | null;
    modalSellerName: HTMLElement | null;
    modalSellerStats: HTMLElement | null;
    modalContact: HTMLElement | null;
    modalFavorite: HTMLElement | null;
    modalShare: HTMLElement | null;
    modalPriceTrend: HTMLElement | null;
    priceTrendChart: HTMLElement | null;
    trendHighPrice: HTMLElement | null;
    trendLowPrice: HTMLElement | null;
    trendAvgPrice: HTMLElement | null;
    modalSimilar: HTMLElement | null;
    similarProducts: HTMLElement | null;
    shareModal: HTMLElement | null;
    shareModalOverlay: HTMLElement | null;
    shareModalClose: HTMLElement | null;
    shareLinkInput: HTMLInputElement | null;
    copyLinkBtn: HTMLElement | null;
    shareOptions: NodeListOf<HTMLElement>;
    contactModal: HTMLElement | null;
    contactModalOverlay: HTMLElement | null;
    contactModalClose: HTMLElement | null;
    contactSellerAvatar: HTMLImageElement | null;
    contactSellerName: HTMLElement | null;
    contactProductName: HTMLElement | null;
    contactMethods: HTMLElement | null;
    productsGrid: HTMLElement | null;
}

export class ProductDetailModal {
    private currentProduct: ProductListItem | null = null;
    private currentImageIndex = 0;
    private el: ModalElements;
    private allProducts: ProductListItem[] = [];

    constructor(elements: ModalElements) {
        this.el = elements;
    }

    setProductSource(products: ProductListItem[]): void {
        this.allProducts = products;
    }

    open(product: ProductListItem): void {
        this.currentProduct = product;
        this.currentImageIndex = 0;
        const el = this.el;

        if (!el.modalMainImage) {return;}
        el.modalMainImage.src = product.images?.[0] || `https://picsum.photos/seed/${product.id}/400/400`;
        if (!el.modalCondition) {return;}
        el.modalCondition.textContent = `${product.conditionIcon} ${product.conditionName}`;

        if (product.originalPrice) {
            if (!el.modalDiscount) {return;}
            el.modalDiscount.textContent = `${Math.round((product.price / product.originalPrice) * 10)}折`;
            el.modalDiscount.style.display = 'inline-flex';
        } else {
            if (el.modalDiscount) {
                el.modalDiscount.style.display = 'none';
            }
        }

        if (!el.modalTitle) {return;}
        el.modalTitle.textContent = product.name;
        if (!el.modalPrice) {return;}
        el.modalPrice.textContent = `¥${product.price}`;
        if (!el.modalOriginalPrice) {return;}
        el.modalOriginalPrice.textContent = product.originalPrice ? `¥${product.originalPrice}` : '';
        el.modalOriginalPrice.style.display = product.originalPrice ? 'inline' : 'none';
        if (!el.modalLocation) {return;}
        el.modalLocation.textContent = product.location || '未设置';
        if (!el.modalDate) {return;}
        el.modalDate.textContent = formatRelativeTime(product.createTime);
        if (!el.modalViews) {return;}
        el.modalViews.textContent = `${product.viewCount || 0} 次浏览`;
        if (!el.modalDescription) {return;}
        el.modalDescription.textContent = product.description || '暂无描述';
        if (!el.modalSellerAvatar) {return;}
        el.modalSellerAvatar.src = product.sellerAvatar || '';
        if (!el.modalSellerName) {return;}
        el.modalSellerName.textContent = product.sellerName;
        if (!el.modalSellerStats) {return;}
        el.modalSellerStats.textContent = `已发布 ${Math.floor(Math.random() * 10) + 1} 件商品`;

        this.renderGalleryThumbnails(product.images || []);
        this.updateGalleryCounter();

        const isFavorited = favoriteManager.has(product.id);
        if (el.modalFavorite) {
            el.modalFavorite.classList.toggle('favorited', isFavorited);
        }
        this.setFavoriteBtnContent(isFavorited);

        Promise.all([this.loadPriceTrend(product.id), this.loadSimilarProducts(product)]);
        if (!el.productDetailModal) {return;}
        el.productDetailModal.classList.add('open');
        document.body.style.overflow = 'hidden';
    }

    close(): void {
        this.el.productDetailModal?.classList.remove('open');
        document.body.style.overflow = '';
        this.currentProduct = null;
    }

    isOpen(): boolean {
        return this.el.productDetailModal?.classList.contains('open') ?? false;
    }

    getCurrentProduct(): ProductListItem | null { return this.currentProduct; }

    navigateGallery(direction: number): void {
        if (!this.currentProduct?.images?.length) {return;}
        const total = this.currentProduct.images.length;
        this.currentImageIndex = (this.currentImageIndex + direction + total) % total;
        this.updateGallery();
    }

    toggleFavorite(): void {
        if (!this.currentProduct) {return;}
        const isFavorited = favoriteManager.toggle(this.currentProduct.id);
        toast.show(isFavorited ? '已添加到收藏' : '已取消收藏', isFavorited ? 'success' : 'info');
        this.el.modalFavorite?.classList.toggle('favorited', isFavorited);
        this.setFavoriteBtnContent(isFavorited);

        const cardBtn = this.el.productsGrid?.querySelector<HTMLElement>(`[data-product-id="${this.currentProduct.id}"] .favorite-btn`);
        if (cardBtn) {
            cardBtn.classList.toggle('favorited', isFavorited);
            cardBtn.innerHTML = `<svg viewBox="0 0 24 24" fill="${isFavorited ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">` +
                '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>';
        }
    }

    private setFavoriteBtnContent(isFavorited: boolean): void {
        const modalFavorite = this.el.modalFavorite;
        if (!modalFavorite) {return;}
        modalFavorite.innerHTML =
            `<svg viewBox="0 0 24 24" fill="${isFavorited ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">` +
            `<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>` +
            `</svg>${isFavorited ? '已收藏' : '收藏'}`;
    }

    private updateGallery(): void {
        if (!this.currentProduct?.images?.length) {return;}
        const modalMainImage = this.el.modalMainImage;
        if (!modalMainImage) {return;}
        modalMainImage.src = this.currentProduct.images[this.currentImageIndex];
        this.el.galleryThumbnails?.querySelectorAll<HTMLElement>('.gallery-thumb').forEach((thumb, i) => {
            thumb.classList.toggle('active', i === this.currentImageIndex);
        });
        this.updateGalleryCounter();
    }

    private updateGalleryCounter(): void {
        if (!this.currentProduct?.images?.length) {return;}
        const galleryCounter = this.el.galleryCounter;
        if (!galleryCounter) {return;}
        galleryCounter.textContent = `${this.currentImageIndex + 1} / ${this.currentProduct.images.length}`;
    }

    private renderGalleryThumbnails(images: string[]): void {
        if (!this.el.galleryThumbnails) {return;}
        if (images.length <= 1) { this.el.galleryThumbnails.innerHTML = ''; return; }

        let html = '';
        images.forEach((img, i) => {
            html += `<div class="gallery-thumb ${i === 0 ? 'active' : ''}" data-index="${i}">` +
                `<img src="${escapeHtml(img)}" alt="商品图片 ${i + 1}"></div>`;
        });
        this.el.galleryThumbnails.innerHTML = html;
        this.el.galleryThumbnails.querySelectorAll<HTMLElement>('.gallery-thumb').forEach(thumb => {
            thumb.addEventListener('click', () => {
                this.currentImageIndex = parseInt(thumb.dataset.index || '0');
                this.updateGallery();
            });
        });
    }

    private async loadPriceTrend(productId: number): Promise<void> {
        try {
            const response = await api.product.getPriceHistory(productId);
            if (response) {
                const data = response.data ?? [];
                this.renderPriceTrendFromHistory(data);
            } else {
                this.el.modalPriceTrend?.style.setProperty('display', 'none');
            }
        } catch {
            this.renderMockPriceTrend();
        }
    }

    private renderMockPriceTrend(): void {
        if (!this.currentProduct) {return;}
        const { modalPriceTrend, priceTrendChart, trendHighPrice, trendLowPrice, trendAvgPrice } = this.el;
        if (!modalPriceTrend || !priceTrendChart || !trendHighPrice || !trendLowPrice || !trendAvgPrice) {return;}
        modalPriceTrend.style.display = 'block';
        const base = this.currentProduct.price;
        const prices = Array.from({ length: 8 }, (_, i) => i === 7 ? base : Math.max(base + (Math.random() - 0.3) * base * 0.2, base * 0.7));
        const max = Math.max(...prices), min = Math.min(...prices), avg = prices.reduce((a, b) => a + b, 0) / prices.length;

        let html = '';
        prices.forEach(p => { html += `<div class="trend-bar" style="height: ${Math.max(8, (p / max) * 100)}%;" data-price="¥${p.toFixed(0)}"></div>`; });
        priceTrendChart.innerHTML = html;
        trendHighPrice.textContent = `¥${max.toFixed(0)}`;
        trendLowPrice.textContent = `¥${min.toFixed(0)}`;
        trendAvgPrice.textContent = `¥${avg.toFixed(0)}`;
    }

    private renderPriceTrendFromHistory(history: Array<{ date: string; price: number }>): void {
        if (!history?.length) { this.renderMockPriceTrend(); return; }
        const { modalPriceTrend, priceTrendChart, trendHighPrice, trendLowPrice, trendAvgPrice } = this.el;
        if (!modalPriceTrend || !priceTrendChart || !trendHighPrice || !trendLowPrice || !trendAvgPrice) {return;}
        const prices = history.map(h => h.price);
        const max = Math.max(...prices), min = Math.min(...prices), avg = prices.reduce((a, b) => a + b, 0) / prices.length;

        let html = '';
        prices.forEach(p => { html += `<div class="trend-bar" style="height: ${Math.max(8, (p / max) * 100)}%;" data-price="¥${p}"></div>`; });
        modalPriceTrend.style.display = 'block';
        priceTrendChart.innerHTML = html;
        trendHighPrice.textContent = `¥${max}`;
        trendLowPrice.textContent = `¥${min}`;
        trendAvgPrice.textContent = `¥${avg}`;
    }

    private async loadSimilarProducts(product: ProductListItem): Promise<void> {
        try {
            const response: unknown = await api.product.getSimilarProducts(product.id);
            if (response) {
                const raw = (response as { data?: unknown }).data ?? response;
                const similar = (raw as unknown as ProductListItem[]) || [];
                if (Array.isArray(similar) && similar.length > 0) {
                    this.renderSimilarList(similar);
                } else {
                    this.renderSimilarFallback(product);
                }
            } else {
                this.renderSimilarFallback(product);
            }
        } catch {
            this.renderSimilarFallback(product);
        }
    }

    private renderSimilarFallback(product: ProductListItem): void {
        const similar = this.allProducts.filter(p => p.id !== product.id && p.categoryId === product.categoryId).slice(0, 3);
        if (!this.el.modalSimilar) {return;}
        if (!similar.length) { this.el.modalSimilar.style.display = 'none'; return; }
        this.el.modalSimilar.style.display = 'block';
        this.renderSimilarList(similar);
    }

    private renderSimilarList(products: ProductListItem[]): void {
        if (!this.el.modalSimilar || !this.el.similarProducts) {return;}
        this.el.modalSimilar.style.display = 'block';
        let html = '';
        products.forEach(p => {
            html += `<div class="similar-product" data-id="${escapeHtml(String(p.id))}">` +
                `<img src="${escapeHtml(p.images?.[0] || `https://picsum.photos/seed/${p.id}/200/200`)}" alt="${escapeHtml(p.name)}">` +
                '<div class="similar-product-info">' +
                    `<div class="similar-product-name">${escapeHtml(p.name)}</div>` +
                    `<div class="similar-product-price">¥${escapeHtml(String(p.price))}</div>` +
                '</div></div>';
        });
        this.el.similarProducts.innerHTML = html;
        this.el.similarProducts.querySelectorAll<HTMLElement>('.similar-product').forEach(item => {
            item.addEventListener('click', () => {
                const productId = parseInt(item.dataset.id || '0');
                this.close();
                this.loadProductDetail(productId);
            });
        });
    }

    private async loadProductDetail(productId: number): Promise<void> {
        try {
            const response: unknown = await api.product.getProductDetail(productId);
            if (response) {
                const raw = (response as { data?: unknown }).data ?? response;
                setTimeout(() => this.open(raw as ProductListItem), 300);
            }
        } catch (error) {
            toast.error('加载商品详情失败');
        }
    }

    // ===== Contact Modal =====

    openContactModal(): void {
        if (!this.currentProduct) {return;}
        const { sellerName, sellerPhone, sellerWechat, contactMethod, name } = this.currentProduct;
        const phone = sellerPhone || contactMethod || '';
        const wechat = sellerWechat || '';
        const { contactSellerName, contactProductName, contactMethods, contactModal } = this.el;
        if (!contactSellerName || !contactProductName || !contactMethods || !contactModal) {return;}

        contactSellerName.textContent = sellerName || '卖家';
        contactProductName.textContent = name;

        let html = '';
        if (phone && phone !== '未公开') {html += this.contactMethodHTML('phone', '手机号', phone);}
        if (wechat && wechat !== '未公开') {html += this.contactMethodHTML('wechat', '微信号', wechat);}
        if (!html) {
            html = '<div class="no-contact-methods">' +
                '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">' +
                    '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>' +
                '</svg><p>卖家暂未公开联系方式</p><span>请在商品描述中查看联系方式</span></div>';
        }
        contactMethods.innerHTML = html;
        contactMethods.querySelectorAll<HTMLElement>('.copy-btn').forEach(btn => {
            btn.addEventListener('click', () => this.copyToClipboard(btn.dataset.value || '', btn));
        });
        contactModal.classList.add('open');
    }

    closeContactModal(): void { this.el.contactModal?.classList.remove('open'); }

    private contactMethodHTML(type: string, label: string, value: string): string {
        const phonePath = '<path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>';
        const wechatPath = '<path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.01-.269-.03-.407-.03zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/>';
        const copyIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>';
        return `<div class="contact-method-item">` +
            `<div class="method-icon ${type === 'wechat' ? 'wechat' : ''}"><svg viewBox="0 0 24 24" fill="${type === 'wechat' ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">${type === 'wechat' ? wechatPath : phonePath}</svg></div>` +
            `<div class="method-info"><span class="method-label">${label}</span><span class="method-value">${value}</span></div>` +
            `<button class="copy-btn" data-value="${value}" aria-label="复制${label}">${copyIcon}复制</button></div>`;
    }

    private copyToClipboard(text: string, btn: HTMLElement): void {
        navigator.clipboard?.writeText(text).then(() => {
            const orig = btn.innerHTML;
            btn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>已复制';
            btn.classList.add('copied');
            toast.success('已复制到剪贴板');
            setTimeout(() => { btn.innerHTML = orig; btn.classList.remove('copied'); }, 2000);
        }).catch(() => toast.error('复制失败'));
    }

    // ===== Share Modal =====

    openShareModal(): void {
        if (!this.currentProduct) {return;}
        const { shareLinkInput, shareModal } = this.el;
        if (!shareLinkInput || !shareModal) {return;}
        shareLinkInput.value = `${window.location.origin}/products.html?id=${this.currentProduct.id}`;
        shareModal.classList.add('open');
    }

    closeShareModal(): void { this.el.shareModal?.classList.remove('open'); }

    copyShareLink(): void {
        const { shareLinkInput } = this.el;
        if (!shareLinkInput) {return;}
        shareLinkInput.select();
        navigator.clipboard?.writeText(shareLinkInput.value)
            .then(() => toast.success('链接已复制到剪贴板'))
            .catch(() => { try { document.execCommand('copy'); toast.success('链接已复制到剪贴板'); } catch { toast.error('复制失败，请手动复制'); } });
    }

    handleShare(platform: string): void {
        if (!this.currentProduct) {return;}
        const url = this.el.shareLinkInput?.value ?? '';
        const title = `${this.currentProduct.name} - EasyOrange`;
        const desc = `发现一个不错的二手商品：${this.currentProduct.name}，仅售¥${this.currentProduct.price}`;
        if (platform === 'wechat') { toast.info('请截图分享到微信'); }
        else if (platform === 'weibo') { window.open(`https://service.weibo.com/share/share.php?url=${encodeURIComponent(url)}&title=${encodeURIComponent(desc)}`, '_blank', 'width=600,height=400'); }
        else if (platform === 'qq') { window.open(`https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(url)}&title=${encodeURIComponent(title)}&desc=${encodeURIComponent(desc)}`, '_blank', 'width=600,height=400'); }
    }
}
