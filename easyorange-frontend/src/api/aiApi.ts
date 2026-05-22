import { request } from './core/request';

export interface PricingSuggestion {
    suggestedPrice: number;
    minPrice: number;
    maxPrice: number;
    reasoning: string;
    marketContext: string;
}

export interface AutoListingResult {
    title: string;
    description: string;
    price: number;
    categoryName: string;
    categoryId: number;
    conditionLevel: number;
    location: string;
    tags: string[];
    imageDescriptions: string[];
}

export interface AiReviewResult {
    isApproved: boolean;
    suggestedActionDesc: string;
    confidenceScore: number;
    riskFlags: string[];
    reasoning: string;
}

export interface SemanticSearchResult {
    records: unknown[];
    total: number;
    pageNum: number;
    pageSize: number;
}

export interface QaRequest {
    productId: number;
    question: string;
    productName: string;
    productDescription: string;
    categoryName: string;
    price: string;
    conditionLevel: string;
    sellerName: string;
    sellerCreditLevel: string;
}

export interface QaResponse {
    answer: string;
    hasConfidence: boolean;
}

export interface PriceSuggestionParams {
    productName: string;
    description?: string;
    categoryName?: string;
    conditionLevel?: number;
    originalPrice?: number;
}

export interface CopyGenerationParams {
    productName: string;
    categoryName?: string;
    conditionLevel?: number;
    originalPrice?: string;
    style?: 'standard' | 'detailed' | 'concise' | 'emotional';
}

export interface CopyGenerationResult {
    title: string;
    description: string;
    style: string;
}

export const aiApi = {
    suggestPrice(params: PriceSuggestionParams) {
        return request<PricingSuggestion>('/ai/pricing', {
            method: 'POST',
            params: params as unknown as Record<string, unknown>
        });
    },

    autoListing(imageUrls: string[]) {
        return request<AutoListingResult>('/ai/auto-listing', {
            method: 'POST',
            body: imageUrls
        });
    },

    semanticSearch(params: { keyword: string; pageNum?: number; pageSize?: number }) {
        return request<SemanticSearchResult>('/ai/semantic-search', {
            method: 'GET',
            params: params as Record<string, unknown>
        });
    },

    answerQuestion(data: QaRequest) {
        return request<QaResponse>('/ai/qa', {
            method: 'POST',
            body: data
        });
    },

    generateCopy(params: CopyGenerationParams) {
        return request<CopyGenerationResult>('/ai/generate-copy', {
            method: 'POST',
            params: params as unknown as Record<string, unknown>
        });
    }
};