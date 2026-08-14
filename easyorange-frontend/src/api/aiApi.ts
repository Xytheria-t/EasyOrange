import type { RawProduct } from '@/types';
import type { ChatAnswer, ChatFeedbackRequest, ChatRequest, ChatStreamEvent, KnowledgeHit } from '@/types/ai';
import { request } from './core/request';
import { streamChat } from './core/stream';

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
    categoryId: string;
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
    records: RawProduct[];
    total: number;
    current: number;
    size: number;
    pages: number;
}

export interface QaRequest {
    productId: string;
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
            body: params,
        });
    },

    autoListing(imageUrls: string[]) {
        return request<AutoListingResult>('/ai/auto-listing', {
            method: 'POST',
            body: imageUrls,
        });
    },

    semanticSearch(params: { keyword: string; pageNum?: number; pageSize?: number }) {
        return request<SemanticSearchResult>('/ai/semantic-search', {
            method: 'GET',
            params: params as Record<string, unknown>,
        });
    },

    answerQuestion(data: QaRequest) {
        return request<QaResponse>('/ai/qa', {
            method: 'POST',
            body: data,
        });
    },

    generateCopy(params: CopyGenerationParams) {
        return request<CopyGenerationResult>('/ai/generate-copy', {
            method: 'POST',
            body: params,
        });
    },

    /** AI 对话（多轮 Agent + 知识库引用溯源，非流式） */
    chat(data: ChatRequest) {
        return request<ChatAnswer>('/ai/chat', {
            method: 'POST',
            body: data,
        });
    },

    /** 知识库检索（RAG 检索侧演示） */
    knowledgeSearch(keyword: string, topK = 5) {
        return request<KnowledgeHit[]>('/ai/knowledge/search', {
            method: 'GET',
            params: { keyword, topK },
        });
    },

    /** AI 输出反馈（👍/👎 反馈飞轮） */
    feedback(data: ChatFeedbackRequest) {
        return request<void>('/ai/feedback', {
            method: 'POST',
            body: data,
        });
    },

    /** SSE 流式对话（fetch + ReadableStream，可带 Authorization 头） */
    chatStream(data: ChatRequest, onEvent: (event: ChatStreamEvent) => void, signal?: AbortSignal) {
        return streamChat('/ai/chat/stream', data, onEvent, signal);
    },
};
