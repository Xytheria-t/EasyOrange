/**
 * @fileoverview 保险服务页面逻辑
 * @version 1.0.0
 */

import api, { INSURANCE_TYPE_NAMES, calculatePremiumLocal, type InsuranceType, type Insurance, type InsuranceStatus, type ClaimStatus } from '../api/index.js';
import { toast, dom, formatCurrency, formatDate } from '../utils/index.js';

export type { InsuranceType, Insurance, InsuranceStatus, ClaimStatus };

type InsuranceTypeCard = {
    type: InsuranceType;
    name: string;
    rate: string;
    features: string[];
    icon: string;
};

/** 保险页面元素接口 */
export interface InsurancePageElements {
    insuranceContainer: HTMLElement | null;
    orderInfo: HTMLElement | null;
    insuranceTypes: HTMLElement | null;
    insuranceCalculator: HTMLElement | null;
    insuranceAmount: HTMLInputElement | null;
    premiumDisplay: HTMLElement | null;
    purchaseBtn: HTMLButtonElement | null;
    myInsurances: HTMLElement | null;
}

/** 保险页面对象接口 */
export interface InsurancePageInterface {
    elements: InsurancePageElements;
    currentOrder: unknown;
    currentOrderId: string | null;
    selectedType: InsuranceType;
    init(): void;
    initElements(): void;
    bindEvents(): void;
    loadOrderInfo(): Promise<void>;
    renderInsuranceTypes(): void;
    selectType(type: InsuranceType): void;
    calculatePremium(): void;
    purchaseInsurance(): Promise<void>;
    loadMyInsurances(): Promise<void>;
    renderMyInsurances(insurances: Insurance[]): void;
    renderInsuranceItem(ins: Insurance): string;
    showClaimModal(insuranceId: number): void;
    submitClaim(insuranceId: number, claimAmount: number, claimReason: string): Promise<void>;
}

// ============================================
// 保险页面逻辑
// ============================================

const InsurancePage: InsurancePageInterface = {
    elements: {
        insuranceContainer: null,
        orderInfo: null,
        insuranceTypes: null,
        insuranceCalculator: null,
        insuranceAmount: null,
        premiumDisplay: null,
        purchaseBtn: null,
        myInsurances: null
    },
    currentOrder: null,
    currentOrderId: null,
    selectedType: 'basic' as InsuranceType,

    init(): void {
        this.initElements();
        this.bindEvents();
        this.loadOrderInfo();
    },

    initElements(): void {
        this.elements = {
            insuranceContainer: dom.get('#insuranceContainer') as HTMLElement | null,
            orderInfo: dom.get('#orderInfo') as HTMLElement | null,
            insuranceTypes: dom.get('#insuranceTypes') as HTMLElement | null,
            insuranceCalculator: dom.get('#insuranceCalculator') as HTMLElement | null,
            insuranceAmount: dom.get('#insuranceAmount') as HTMLInputElement | null,
            premiumDisplay: dom.get('#premiumDisplay') as HTMLElement | null,
            purchaseBtn: dom.get('#purchaseBtn') as HTMLButtonElement | null,
            myInsurances: dom.get('#myInsurances') as HTMLElement | null
        };
    },

    bindEvents(): void {
        if (this.elements.insuranceTypes) {
            dom.on(this.elements.insuranceTypes, 'click', (e: Event) => {
                const target = e.target as HTMLElement;
                const typeCard = target.closest('.insurance-type-card') as HTMLElement;
                if (typeCard && typeCard.dataset.type) {
                    this.selectType(typeCard.dataset.type as InsuranceType);
                }
            });
        }

        if (this.elements.insuranceAmount) {
            dom.on(this.elements.insuranceAmount, 'input', () => this.calculatePremium());
        }

        if (this.elements.purchaseBtn) {
            dom.on(this.elements.purchaseBtn, 'click', () => this.purchaseInsurance());
        }
    },

    async loadOrderInfo(): Promise<void> {
        const urlParams = new URLSearchParams(window.location.search);
        const orderId = urlParams.get('orderId');

        if (orderId) {
            try {
                const canPurchase = await api.insurance.canPurchase(parseInt(orderId, 10));
                if (!canPurchase.data && !canPurchase) {
                    toast.error('该订单无法购买保险');
                    return;
                }
                this.currentOrderId = orderId;
            } catch (error) {
                // 检查失败时静默处理，用户仍可继续操作
            }
        }

        this.renderInsuranceTypes();
        this.loadMyInsurances();
    },

    renderInsuranceTypes(): void {
        if (!this.elements.insuranceTypes) {return;}

        const types: InsuranceTypeCard[] = [
            {
                type: 'basic',
                name: INSURANCE_TYPE_NAMES['basic'],
                rate: '2%',
                features: ['商品与描述不符', '商品损坏赔付', '最高全额赔付'],
                icon: '🛡️'
            },
            {
                type: 'premium',
                name: INSURANCE_TYPE_NAMES['premium'],
                rate: '3.5%',
                features: ['包含基础保障', '物流丢失赔付', '卖家违约赔付'],
                icon: '🛡️✨'
            },
            {
                type: 'vip',
                name: INSURANCE_TYPE_NAMES['vip'],
                rate: '5%',
                features: ['包含高级保障', '无理由退货', '价格保护'],
                icon: '🛡️👑'
            }
        ];

        this.elements.insuranceTypes.innerHTML = types.map(t => `
            <div class="insurance-type-card" data-type="${t.type}">
                <div class="type-icon">${t.icon}</div>
                <h4 class="type-name">${t.name}</h4>
                <div class="type-rate">保费率：${t.rate}</div>
                <ul class="type-features">
                    ${t.features.map(f => `<li>${f}</li>`).join('')}
                </ul>
            </div>
        `).join('');

        this.selectType('basic' as InsuranceType);
    },

    selectType(type: InsuranceType): void {
        this.selectedType = type;

        const cards = dom.getAll('.insurance-type-card', this.elements.insuranceTypes ?? undefined);
        cards.forEach(card => {
            card.classList.toggle('selected', card.dataset.type === type);
        });

        this.calculatePremium();
    },

    calculatePremium(): void {
        const amount = parseFloat(this.elements.insuranceAmount?.value || '0') || 0;

        if (amount <= 0) {
            if (this.elements.premiumDisplay) {
                this.elements.premiumDisplay.textContent = '请输入保险金额';
            }
            return;
        }

        const premium = calculatePremiumLocal(amount, this.selectedType);

        if (this.elements.premiumDisplay) {
            this.elements.premiumDisplay.innerHTML = `
                <div class="premium-result">
                    <div class="premium-label">预估保费</div>
                    <div class="premium-value">${formatCurrency(premium)}</div>
                    <div class="premium-info">
                        保险金额：${formatCurrency(amount)}<br>
                        保险类型：${INSURANCE_TYPE_NAMES[this.selectedType]}
                    </div>
                </div>
            `;
        }
    },

    async purchaseInsurance(): Promise<void> {
        if (!this.currentOrderId) {
            toast.error('订单信息缺失');
            return;
        }

        const amount = parseFloat(this.elements.insuranceAmount?.value || '0') || 0;

        if (amount <= 0) {
            toast.error('请输入有效的保险金额');
            return;
        }

        dom.addClass(this.elements.purchaseBtn, 'loading');
        if (this.elements.purchaseBtn) {
            this.elements.purchaseBtn.disabled = true;
        }

        try {
            await api.insurance.purchase(
                parseInt(this.currentOrderId, 10),
                this.selectedType
            );

            toast.success('购买保险成功！');
            this.loadMyInsurances();
        } catch (error) {
            const err = error as Error;
            toast.error(`购买失败：${  err.message || '请稍后重试'}`);
        } finally {
            dom.removeClass(this.elements.purchaseBtn, 'loading');
            if (this.elements.purchaseBtn) {
                this.elements.purchaseBtn.disabled = false;
            }
        }
    },

    async loadMyInsurances(): Promise<void> {
        if (!this.elements.myInsurances) {return;}

        try {
            const response = await api.insurance.getMyInsurances();
            const insurances = (response.data?.records || response.data || []) as Insurance[];
            this.renderMyInsurances(insurances);
        } catch (error) {
            // 加载失败时显示空状态
            this.renderMyInsurances([]);
        }
    },

    renderMyInsurances(insurances: Insurance[]): void {
        if (!this.elements.myInsurances) {return;}

        if (!insurances || insurances.length === 0) {
            this.elements.myInsurances.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">🛡️</div>
                    <p>暂无保险记录</p>
                </div>
            `;
            return;
        }

        this.elements.myInsurances.innerHTML = `
            <h3>我的保险</h3>
            <div class="insurance-list">
                ${insurances.map(ins => this.renderInsuranceItem(ins)).join('')}
            </div>
        `;
    },

    renderInsuranceItem(ins: Insurance): string {
        const statusClass = ins.status === 1 ? 'active' : ins.status === 2 ? 'expired' : 'claimed';

        return `
            <div class="insurance-item ${statusClass}">
                <div class="insurance-header">
                    <span class="insurance-no">${ins.insuranceNo}</span>
                    <span class="insurance-status">${ins.statusText}</span>
                </div>
                <div class="insurance-body">
                    <div class="insurance-detail">
                        <span class="label">保险类型</span>
                        <span class="value">${ins.insuranceTypeName || ins.type}</span>
                    </div>
                    <div class="insurance-detail">
                        <span class="label">保险金额</span>
                        <span class="value">${formatCurrency(ins.insuranceAmount ?? ins.coverage)}</span>
                    </div>
                    <div class="insurance-detail">
                        <span class="label">保费</span>
                        <span class="value">${formatCurrency(ins.premium)}</span>
                    </div>
                    <div class="insurance-detail">
                        <span class="label">有效期至</span>
                        <span class="value">${formatDate(ins.expireTime ?? ins.endTime, 'date')}</span>
                    </div>
                </div>
                ${ins.status === 1 && ins.claimStatus === 'none' ? `
                    <div class="insurance-actions">
                        <button class="btn btn-outline" onclick="InsurancePage.showClaimModal(${ins.id})">
                            申请理赔
                        </button>
                    </div>
                ` : ''}
            </div>
        `;
    },

    showClaimModal(insuranceId: number): void {
        const amount = prompt('请输入理赔金额：');
        if (!amount) {return;}

        const reason = prompt('请输入理赔原因：');
        if (!reason) {return;}

        this.submitClaim(insuranceId, parseFloat(amount), reason);
    },

    async submitClaim(insuranceId: number, claimAmount: number, claimReason: string): Promise<void> {
        try {
            await api.insurance.claim(insuranceId, claimAmount, claimReason);
            toast.success('理赔申请已提交');
            this.loadMyInsurances();
        } catch (error) {
            const err = error as Error;
            toast.error(`申请失败：${  err.message || '请稍后重试'}`);
        }
    }
};

// 挂载到全局对象以供内联事件使用
(window as unknown as { InsurancePage: InsurancePageInterface }).InsurancePage = InsurancePage;

export default InsurancePage;
