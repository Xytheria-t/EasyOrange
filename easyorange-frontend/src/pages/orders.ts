/**
 * @fileoverview 订单管理页面
 */

import { orderApi } from '../api/index.js';
import { formatDate } from '../utils/index.js';
import { navigation } from '../app/navigation.js';
import { isSuccessCode, type ApiCode, type Result } from '../types';

/**
 * 订单状态枚举值（数字形式，与后端保持一致）
 */
export enum OrderStatusCode {
    PENDING_PAYMENT = 0,
    PAID = 1,
    SHIPPED = 2,
    COMPLETED = 3,
    CANCELLED = 4,
    REFUNDED = 5,
}

/**
 * 订单状态文本映射
 */
export const OrderStatusText: Record<number, string> = {
    [OrderStatusCode.PENDING_PAYMENT]: '待支付',
    [OrderStatusCode.PAID]: '已支付',
    [OrderStatusCode.SHIPPED]: '已发货',
    [OrderStatusCode.COMPLETED]: '已完成',
    [OrderStatusCode.CANCELLED]: '已取消',
    [OrderStatusCode.REFUNDED]: '已退款',
};

/**
 * 订单列表项数据结构
 */
export interface OrderListItem {
    id: number;
    orderNo: string;
    productId: number;
    productName: string;
    productImage: string | null;
    totalAmount: number;
    status: OrderStatusCode;
    statusText: string;
    buyerId: number;
    buyerName: string;
    sellerId: number;
    sellerName: string;
    createTime: string;
}

/**
 * 订单操作类型
 */
export type OrderAction = 'pay' | 'ship' | 'receive' | 'cancel' | 'review' | 'detail';

/**
 * 订单页面元素接口
 */
export interface OrdersPageElements {
    container: HTMLElement | null;
    ordersList: HTMLElement | null;
}

/**
 * API 响应结构
 */
export interface ApiResponse<T> {
    code: ApiCode;
    message: string;
    data: T | null;
}

/**
 * 订单管理页面类
 */
export class OrdersPage {
    private currentTab: 'buyer' | 'seller';
    private orders: OrderListItem[];

    constructor() {
        this.currentTab = 'buyer';
        this.orders = [];
        this.init();
    }

    /**
     * 初始化页面
     */
    private async init(): Promise<void> {
        this.render();
        this.bindEvents();
        await this.loadOrders();
    }

    /**
     * 渲染页面结构
     */
    private render(): void {
        const container = document.getElementById('orders-container');
        if (!container) {return;}

        container.innerHTML = `
            <div class="orders-page">
                <div class="page-header">
                    <h1>我的订单</h1>
                </div>
                
                <div class="tabs">
                    <button class="tab-btn active" data-tab="buyer">我买的</button>
                    <button class="tab-btn" data-tab="seller">我卖的</button>
                </div>
                
                <div class="orders-list" id="orders-list">
                    <div class="loading">加载中...</div>
                </div>
            </div>
        `;
    }

    /**
     * 绑定事件监听器
     */
    private bindEvents(): void {
        document.querySelectorAll<HTMLButtonElement>('.tab-btn').forEach(btn => {
            btn.addEventListener('click', async (e: Event) => {
                const target = e.target as HTMLButtonElement;
                document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
                target.classList.add('active');
                this.currentTab = target.dataset.tab as 'buyer' | 'seller';
                await this.loadOrders();
            });
        });
    }

    /**
     * 加载订单列表
     */
    private async loadOrders(): Promise<void> {
        const listEl = document.getElementById('orders-list');
        
        try {
            const response = this.currentTab === 'buyer'
                ? await orderApi.getMyOrders({})
                : await orderApi.getSoldOrders({});
            
            if (isSuccessCode(response.code)) {
                this.orders = (response.data?.records || []).map((o) => ({
                    id: o.id,
                    orderNo: o.orderNo || '',
                    productId: o.productId || 0,
                    productName: o.productTitle || '',
                    productImage: o.productImage || null,
                    totalAmount: o.totalAmount || 0,
                    status: o.status as unknown as OrderStatusCode,
                    statusText: OrderStatusText[o.status as unknown as OrderStatusCode] || '未知',
                    buyerId: o.buyerId || 0,
                    buyerName: o.buyerName || '',
                    sellerId: o.sellerId || 0,
                    sellerName: o.sellerName || '',
                    createTime: o.createTime || ''
                }));
                this.renderOrders();
            } else {
                if (listEl) { listEl.innerHTML = `<div class="error">${response.message}</div>`; }
            }
        } catch (error) {
            const err = error as Error;
            if (listEl) { listEl.innerHTML = `<div class="error">加载失败: ${err.message}</div>`; }
        }
    }

    /**
     * 渲染订单列表
     */
    private renderOrders(): void {
        const listEl = document.getElementById('orders-list');
        
        if (this.orders.length === 0) {
            if (listEl) { listEl.innerHTML = '<div class="empty">暂无订单</div>'; }
            return;
        }

        if (listEl) {
            listEl.innerHTML = this.orders.map((order: OrderListItem) => `
            <div class="order-card" data-id="${order.id}">
                <div class="order-header">
                    <span class="order-no">订单号: ${order.orderNo}</span>
                    <span class="order-status status-${order.status}">${order.statusText}</span>
                </div>
                <div class="order-body">
                    <div class="product-info">
                        <img src="${order.productImage || '/images/default-product.png'}" alt="${order.productName}">
                        <div class="product-detail">
                            <h3>${order.productName}</h3>
                            <p class="price">¥${order.totalAmount.toFixed(2)}</p>
                        </div>
                    </div>
                    <div class="user-info">
                        <p>${this.currentTab === 'buyer' ? '卖家' : '买家'}: ${this.currentTab === 'buyer' ? order.sellerName : order.buyerName}</p>
                    </div>
                </div>
                <div class="order-footer">
                    <span class="order-time">${formatDate(order.createTime)}</span>
                    <div class="order-actions">
                        ${this.renderActions(order)}
                    </div>
                </div>
            </div>
        `).join('');
        }

        this.bindOrderEvents();
    }

    /**
     * 渲染订单操作按钮
     */
    private renderActions(order: OrderListItem): string {
        const actions: string[] = [];
        const isBuyer = this.currentTab === 'buyer';

        switch (order.status) {
            case OrderStatusCode.PENDING_PAYMENT:
                if (isBuyer) {
                    actions.push(`<button class="btn-primary" data-action="pay" data-id="${order.id}">立即支付</button>`);
                }
                actions.push(`<button class="btn-secondary" data-action="cancel" data-id="${order.id}">取消订单</button>`);
                break;
            case OrderStatusCode.PAID:
                if (!isBuyer) {
                    actions.push(`<button class="btn-primary" data-action="ship" data-id="${order.id}">确认发货</button>`);
                }
                break;
            case OrderStatusCode.SHIPPED:
                if (isBuyer) {
                    actions.push(`<button class="btn-primary" data-action="receive" data-id="${order.id}">确认收货</button>`);
                }
                break;
            case OrderStatusCode.COMPLETED:
                if (isBuyer) {
                    actions.push(`<button class="btn-secondary" data-action="review" data-id="${order.id}">评价</button>`);
                }
                break;
        }

        actions.push(`<button class="btn-link" data-action="detail" data-id="${order.id}">查看详情</button>`);

        return actions.join('');
    }

    /**
     * 绑定订单操作事件
     */
    private bindOrderEvents(): void {
        document.querySelectorAll<HTMLButtonElement>('.order-actions button').forEach(btn => {
            btn.addEventListener('click', async (e: Event) => {
                const target = e.target as HTMLButtonElement;
                const action = target.dataset.action as OrderAction;
                const orderId = target.dataset.id;
                if (orderId) { await this.handleAction(action, orderId); }
            });
        });
    }

    /**
     * 处理订单操作
     */
    private async handleAction(action: OrderAction, orderId: string): Promise<void> {
        try {
            let response: Result<unknown> | undefined;
            const orderIdNum = parseInt(orderId, 10);

            switch (action) {
                case 'pay':
                    response = await orderApi.pay(orderIdNum);
                    break;
                case 'ship':
                    response = await orderApi.ship(orderIdNum);
                    break;
                case 'receive':
                    response = await orderApi.receive(orderIdNum);
                    break;
                case 'cancel':
                    if (confirm('确定要取消订单吗？')) {
                        response = await orderApi.cancel(orderIdNum);
                    }
                    break;
                case 'review':
                    navigation.go('review', { query: { orderId: orderId } });
                    return;
                case 'detail':
                    navigation.go('orderDetail', { query: { id: orderId } });
                    return;
            }

            if (response && isSuccessCode(response.code)) {
                alert('操作成功');
                await this.loadOrders();
            } else if (response) {
                alert(response.message || '操作失败');
            }
        } catch (error) {
            const err = error as Error;
            alert(`操作失败: ${  err.message}`);
        }
    }
}

export default OrdersPage;
