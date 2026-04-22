/**
 * @fileoverview 评价页面
 * @version 1.0.0
 */

import api, { type OrderDetail } from '../../api/index.js';
import { formatDate } from '../../utils/index.js';
import { navigation } from '../../app/navigation.js';
import { isSuccessCode, type ApiCode, type Result } from '../../types/index.js';

export interface ReviewData {
    orderId: number;
    targetUserId: number;
    productId: number;
    rating: number;
    content: string;
}

export interface CreateReviewRequest {
    orderId: number;
    targetUserId: number;
    productId: number;
    rating: number;
    content: string;
}

export interface ApiResponse<T = unknown> {
    code: ApiCode;
    message: string;
    data: T;
}

export default class ReviewPage {
    private orderId: string | null = null;
    private order: OrderDetail | null = null;

    constructor() {
        this.orderId = null;
        this.order = null;
        this.init();
    }

    async init(): Promise<void> {
        const urlParams = new URLSearchParams(window.location.search);
        this.orderId = urlParams.get('orderId');

        if (!this.orderId) {
            alert('订单ID不存在');
            window.history.back();
            return;
        }

        this.render();
        await this.loadOrder();
    }

    render(): void {
        const container = document.getElementById('review-container');
        if (!container) {return;}

        container.innerHTML = `
            <div class="review-page">
                <div class="page-header">
                    <h1>评价订单</h1>
                </div>

                <div class="review-form">
                    <div class="order-info" id="order-info">
                        <div class="loading">加载中...</div>
                    </div>

                    <div class="rating-section">
                        <label>评分</label>
                        <div class="star-rating" id="star-rating">
                            <span class="star" data-rating="1">★</span>
                            <span class="star" data-rating="2">★</span>
                            <span class="star" data-rating="3">★</span>
                            <span class="star" data-rating="4">★</span>
                            <span class="star" data-rating="5">★</span>
                        </div>
                        <input type="hidden" id="rating-input" value="5">
                    </div>

                    <div class="content-section">
                        <label>评价内容</label>
                        <textarea id="review-content" placeholder="请输入您的评价（选填）" maxlength="500"></textarea>
                        <span class="char-count"><span id="char-count">0</span>/500</span>
                    </div>

                    <div class="submit-section">
                        <button class="btn-secondary" onclick="window.history.back()">取消</button>
                        <button class="btn-primary" id="submit-btn">提交评价</button>
                    </div>
                </div>
            </div>
        `;

        this.bindEvents();
    }

    bindEvents(): void {
        const stars = document.querySelectorAll('.star-rating .star');
        const ratingInput = document.getElementById('rating-input') as HTMLInputElement;
        const contentInput = document.getElementById('review-content') as HTMLTextAreaElement;
        const charCount = document.getElementById('char-count');
        const submitBtn = document.getElementById('submit-btn');
        const starRating = document.querySelector('.star-rating');

        stars.forEach(star => {
            star.addEventListener('click', () => {
                const rating = (star as HTMLElement).dataset.rating;
                if (rating && ratingInput) {
                    ratingInput.value = rating;
                    this.updateStars(parseInt(rating));
                }
            });

            star.addEventListener('mouseenter', () => {
                const rating = (star as HTMLElement).dataset.rating;
                if (rating) {
                    this.updateStars(parseInt(rating));
                }
            });
        });

        starRating?.addEventListener('mouseleave', () => {
            if (ratingInput) {
                this.updateStars(parseInt(ratingInput.value));
            }
        });

        contentInput?.addEventListener('input', () => {
            if (charCount) {
                charCount.textContent = contentInput.value.length.toString();
            }
        });

        submitBtn?.addEventListener('click', () => this.submitReview());
    }

    updateStars(rating: number): void {
        const stars = document.querySelectorAll('.star-rating .star');
        stars.forEach((star, index) => {
            if (index < rating) {
                star.classList.add('active');
            } else {
                star.classList.remove('active');
            }
        });
    }

    async loadOrder(): Promise<void> {
        const orderInfoEl = document.getElementById('order-info');

        try {
            const response: Result<OrderDetail> = await api.order.getDetail(Number(this.orderId));

            if (isSuccessCode(response.code) && response.data) {
                this.order = response.data;
                this.renderOrderInfo();
            } else {
                if (orderInfoEl) {
                    orderInfoEl.innerHTML = `<div class="error">订单不存在</div>`;
                }
            }
        } catch (error) {
            const err = error as Error;
            if (orderInfoEl) {
                orderInfoEl.innerHTML = `<div class="error">加载失败: ${err.message}</div>`;
            }
        }
    }

    renderOrderInfo(): void {
        const orderInfoEl = document.getElementById('order-info');
        const order = this.order;

        if (!orderInfoEl || !order) {return;}

        orderInfoEl.innerHTML = `
            <div class="product-card">
                <img src="${order.productImage || '/images/default-product.png'}" alt="${order.productTitle || ''}">
                <div class="product-detail">
                    <h3>${order.productTitle || ''}</h3>
                    <p class="price">¥${order.price || 0}</p>
                </div>
            </div>
            <div class="trade-info">
                <p>交易对方: ${(order as OrderDetail & { sellerName?: string; buyerName?: string }).sellerName || (order as OrderDetail & { sellerName?: string; buyerName?: string }).buyerName || '未知'}</p>
                <p>成交时间: ${formatDate(order.completeTime || order.createTime)}</p>
            </div>
        `;
    }

    async submitReview(): Promise<void> {
        const ratingInput = document.getElementById('rating-input') as HTMLInputElement;
        const contentInput = document.getElementById('review-content') as HTMLTextAreaElement;

        const rating = parseInt(ratingInput?.value || '5');
        const content = contentInput?.value.trim() || '';

        if (!this.order) {
            alert('订单信息加载失败');
            return;
        }

        const orderWithUsers = this.order as OrderDetail & {
            sellerId?: number;
            buyerId?: number;
            productId?: number;
            sellerName?: string;
            buyerName?: string;
        };
        const targetUserId = orderWithUsers.sellerId || orderWithUsers.buyerId;

        if (!targetUserId) {
            alert('无法获取交易对方信息');
            return;
        }

        try {
            const reviewData = {
                orderId: parseInt(this.orderId || '0'),
                targetUserId: targetUserId,
                productId: orderWithUsers.productId || this.order.id,
                rating: rating,
                content: content
            };

            const response: ApiResponse<unknown> = await api.review.create(orderWithUsers.productId || this.order.id, reviewData as Record<string, unknown>);

            if (isSuccessCode(response.code)) {
                alert('评价成功');
                navigation.go('orders');
            } else {
                alert(response.message || '评价失败');
            }
        } catch (error) {
            const err = error as Error;
            alert(`评价失败: ${err.message}`);
        }
    }
}