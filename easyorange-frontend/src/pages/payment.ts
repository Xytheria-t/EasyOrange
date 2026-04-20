/**
 * @fileoverview 支付页面逻辑
 * @version 1.0.0
 */

import api, { type PaymentInfo, type PaymentResponse } from '../api/index.js';
import { toast, dom, formatCurrency } from '../utils/index.js';

export type { PaymentInfo, PaymentResponse };

type PaymentMethod = 'wechat' | 'alipay' | 'campus_card';

type PaymentMethodData = {
    code: PaymentMethod;
    name: string;
    icon: string;
    desc: string;
};

/** 支付页面元素接口 */
export interface PaymentPageElements {
    paymentContainer: HTMLElement | null;
    paymentAmount: HTMLElement | null;
    paymentMethods: HTMLElement | null;
    paymentBtn: HTMLButtonElement | null;
    paymentQrCode: HTMLElement | null;
    paymentStatus: HTMLElement | null;
    countdown: HTMLElement | null;
}

/** 支付页面对象接口 */
export interface PaymentPageInterface {
    elements: PaymentPageElements;
    currentPayment: PaymentInfo | null;
    selectedMethod: PaymentMethod | null;
    init(): void;
    initElements(): void;
    bindEvents(): void;
    loadPaymentInfo(): Promise<void>;
    renderPaymentInfo(): void;
    renderPaymentMethods(): void;
    selectPaymentMethod(method: PaymentMethod): void;
    processPayment(): Promise<void>;
    handlePaymentResponse(response: PaymentResponse): void;
    showQrCode(qrCodeUrl: string): void;
    pollPaymentStatus(): void;
    onPaymentSuccess(): void;
    onPaymentFailed(): void;
    startCountdown(): void;
}

// ============================================
// 支付页面逻辑
// ============================================

const PaymentPage: PaymentPageInterface = {
    elements: {
        paymentContainer: null,
        paymentAmount: null,
        paymentMethods: null,
        paymentBtn: null,
        paymentQrCode: null,
        paymentStatus: null,
        countdown: null
    },
    currentPayment: null,
    selectedMethod: null,

    init(): void {
        this.initElements();
        this.bindEvents();
        this.loadPaymentInfo();
    },

    initElements(): void {
        this.elements = {
            paymentContainer: dom.get('#paymentContainer') as HTMLElement | null,
            paymentAmount: dom.get('#paymentAmount') as HTMLElement | null,
            paymentMethods: dom.get('#paymentMethods') as HTMLElement | null,
            paymentBtn: dom.get('#paymentBtn') as HTMLButtonElement | null,
            paymentQrCode: dom.get('#paymentQrCode') as HTMLElement | null,
            paymentStatus: dom.get('#paymentStatus') as HTMLElement | null,
            countdown: dom.get('#countdown') as HTMLElement | null
        };
    },

    bindEvents(): void {
        if (this.elements.paymentMethods) {
            dom.on(this.elements.paymentMethods, 'click', (e: Event) => {
                const target = e.target as HTMLElement;
                const method = target.closest('.payment-method') as HTMLElement;
                if (method && method.dataset.method) {
                    this.selectPaymentMethod(method.dataset.method as PaymentMethod);
                }
            });
        }

        if (this.elements.paymentBtn) {
            dom.on(this.elements.paymentBtn, 'click', () => this.processPayment());
        }
    },

    async loadPaymentInfo(): Promise<void> {
        const urlParams = new URLSearchParams(window.location.search);
        const orderId = urlParams.get('orderId');
        
        if (!orderId) {
            toast.error('订单信息缺失');
            return;
        }

        try {
            const response = await api.payment.getPaymentByOrder(parseInt(orderId, 10));
            const data = (response as { data?: PaymentInfo }).data ?? (response as unknown as PaymentInfo);
            this.currentPayment = data;
            this.renderPaymentInfo();
        } catch (error) {
            toast.error('加载支付信息失败');
        }
    },

    renderPaymentInfo(): void {
        if (!this.currentPayment || !this.elements.paymentContainer) {return;}

        const { amount } = this.currentPayment;
        
        if (this.elements.paymentAmount) {
            this.elements.paymentAmount.textContent = formatCurrency(amount);
        }

        this.renderPaymentMethods();
        this.startCountdown();
    },

    renderPaymentMethods(): void {
        if (!this.elements.paymentMethods) {return;}

        const methods: PaymentMethodData[] = [
            { code: 'wechat', name: '微信支付', icon: '💳', desc: '使用微信扫码支付' },
            { code: 'alipay', name: '支付宝', icon: '📱', desc: '使用支付宝扫码支付' },
            { code: 'campus_card', name: '校园卡', icon: '🎓', desc: '使用校园卡余额支付' }
        ];

        this.elements.paymentMethods.innerHTML = methods.map(method => `
            <div class="payment-method" data-method="${method.code}">
                <div class="payment-method-icon">${method.icon}</div>
                <div class="payment-method-info">
                    <div class="payment-method-name">${method.name}</div>
                    <div class="payment-method-desc">${method.desc}</div>
                </div>
                <div class="payment-method-check"></div>
            </div>
        `).join('');
    },

    selectPaymentMethod(method: PaymentMethod): void {
        const methods = dom.getAll('.payment-method', this.elements.paymentMethods ?? undefined);
        methods.forEach(m => {
            m.classList.toggle('selected', m.dataset.method === method);
        });
        this.selectedMethod = method;
    },

    async processPayment(): Promise<void> {
        if (!this.selectedMethod) {
            toast.error('请选择支付方式');
            return;
        }

        if (!this.currentPayment) {
            toast.error('支付信息缺失');
            return;
        }

        dom.addClass(this.elements.paymentBtn, 'loading');
        if (this.elements.paymentBtn) {
            this.elements.paymentBtn.disabled = true;
        }

        try {
            const response = await api.payment.createPayment(
                this.currentPayment.orderId,
                this.selectedMethod
            );

            const data = (response as { data?: PaymentResponse }).data ?? (response as unknown as PaymentResponse);
            this.handlePaymentResponse(data);
        } catch (error) {
            const err = error as Error;
            toast.error(`创建支付失败：${  err.message || '请稍后重试'}`);
        } finally {
            dom.removeClass(this.elements.paymentBtn, 'loading');
            if (this.elements.paymentBtn) {
                this.elements.paymentBtn.disabled = false;
            }
        }
    },

    handlePaymentResponse(response: PaymentResponse): void {
        const { payUrl, qrCodeUrl } = response;

        if (qrCodeUrl) {
            this.showQrCode(qrCodeUrl);
        } else if (payUrl) {
            // 第三方支付跳转 - 外部支付平台必须使用直接 URL
            window.location.href = payUrl;
        } else {
            toast.success('支付创建成功');
            this.pollPaymentStatus();
        }
    },

    showQrCode(qrCodeUrl: string): void {
        if (this.elements.paymentQrCode) {
            this.elements.paymentQrCode.innerHTML = `
                <div class="qrcode-container">
                    <img src="${qrCodeUrl}" alt="支付二维码" class="qrcode-image">
                    <p class="qrcode-tip">请使用扫码支付</p>
                </div>
            `;
            dom.show(this.elements.paymentQrCode);
        }
        this.pollPaymentStatus();
    },

    pollPaymentStatus(): void {
        if (!this.currentPayment) {return;}

        const payment = this.currentPayment;
        const pollInterval = setInterval(async () => {
            try {
                const response = await api.payment.queryPaymentStatus(payment.id);
                const status = (response.data || response).status;

                if (status === 'SUCCESS') {
                    clearInterval(pollInterval);
                    this.onPaymentSuccess();
                } else if (status === 'FAILED' || status === 'CANCELLED') {
                    clearInterval(pollInterval);
                    this.onPaymentFailed();
                }
            } catch (error) {
                // 轮询错误静默处理，不影响用户体验
            }
        }, 3000);

        setTimeout(() => clearInterval(pollInterval), 30 * 60 * 1000);
    },

    onPaymentSuccess(): void {
        toast.success('支付成功！');
        if (this.elements.paymentStatus) {
            this.elements.paymentStatus.innerHTML = `
                <div class="payment-success">
                    <div class="success-icon">✓</div>
                    <h3>支付成功</h3>
                    <p>订单已确认，感谢您的购买！</p>
                    <a href="/orders.html" class="btn btn-primary">查看订单</a>
                </div>
            `;
        }
    },

    onPaymentFailed(): void {
        toast.error('支付失败');
        if (this.elements.paymentStatus) {
            this.elements.paymentStatus.innerHTML = `
                <div class="payment-failed">
                    <div class="failed-icon">✗</div>
                    <h3>支付失败</h3>
                    <p>支付遇到问题，请重试或联系客服</p>
                    <button class="btn btn-primary" onclick="location.reload()">重新支付</button>
                </div>
            `;
        }
    },

    startCountdown(): void {
        if (!this.elements.countdown || !this.currentPayment) {return;}

        const expireTime = new Date(this.currentPayment.expireTime).getTime();
        
        const updateCountdown = () => {
            const now = Date.now();
            const remaining = expireTime - now;

            if (remaining <= 0) {
                if (this.elements.countdown) { this.elements.countdown.textContent = '已过期'; }
                toast.error('支付已超时');
                return;
            }

            const minutes = Math.floor(remaining / 60000);
            const seconds = Math.floor((remaining % 60000) / 1000);
            if (this.elements.countdown) { this.elements.countdown.textContent = `${minutes}:${seconds.toString().padStart(2, '0')}`; }
        };

        updateCountdown();
        setInterval(updateCountdown, 1000);
    }
};

export default PaymentPage;
