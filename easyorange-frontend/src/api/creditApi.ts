import { request } from './core/request';

export interface CreditScoreResult {
    userId: number;
    creditScore: number;
    level: string;
    totalTrades: number;
    completedTrades: number;
    cancelledTrades: number;
    totalReports: number;
    confirmedReports: number;
    reviewAvgRating: number;
    tradeCompletionRate: number;
    lastUpdated: string;
}

export const creditApi = {
    getMyCredit() {
        return request<CreditScoreResult>('/credit/me');
    },

    getUserCredit(userId: number) {
        return request<CreditScoreResult>(`/credit/${userId}`);
    },

    recalculateScore() {
        return request<void>('/credit/recalculate', {
            method: 'POST',
        });
    },
};
