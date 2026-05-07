/**
 * @fileoverview 保险 API 模块
 */

import type { PageResult } from '@/types';
import { request } from './core/request';

export const INSURANCE_TYPE_NAMES: Record<string, string> = {};

export type InsuranceType = string;
export type InsuranceStatus = string;
export type ClaimStatus = string;

export interface Insurance {
    id: number;
    orderId: number;
    type: InsuranceType;
    status: InsuranceStatus | number;
    premium: number;
    coverage: number;
    startTime: string;
    endTime: string;
    expireTime?: string;
    claimStatus: ClaimStatus | null;
    insuranceNo?: string;
    statusText?: string;
    insuranceTypeName?: string;
    insuranceAmount?: number;
}

export const insuranceApi = {
    canPurchase(orderId: number) {
        return request<{ canPurchase: boolean }>(`/insurance/orders/${orderId}/can-purchase`, {
            method: 'GET'
        });
    },

    purchase(orderId: number, insuranceType: string) {
        return request('/insurance/purchase', {
            method: 'POST',
            body: { orderId, insuranceType }
        });
    },

    getMyInsurances() {
        return request<PageResult<Insurance>>('/insurance/my', {
            method: 'GET'
        });
    },

    claim(insuranceId: number, claimAmount: number, claimReason: string) {
        return request(`/insurance/${insuranceId}/claim`, {
            method: 'POST',
            body: { claimAmount, claimReason }
        });
    }
};

export function calculatePremiumLocal(coverage: number, _insuranceType: string): number {
    return Math.round(coverage * 0.01 * 100) / 100;
}
