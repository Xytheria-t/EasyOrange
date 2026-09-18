import type { ChatAnswer, ChatFeedbackRequest, ChatRequest, ChatStreamEvent, KnowledgeHit } from '@/types/ai';
import { request } from './core/request';
import { streamChat } from './core/stream';

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

/**
 * AI 调用专用超时：LLM 单次生成远慢于普通接口（实测视觉识别 ~12s、文案生成 6~28s，
 * 视供应商档位而定），沿用 10s 默认值会让请求被前端中断、后端白算一次。
 * 取值必须高于后端供应商超时（easyorange.ai.deepseek.timeout 30s / qwen-vl.timeout 60s），
 * 否则后端自己的降级结果来不及返回，前端先断在超时上。
 */
const AI_TIMEOUT = 90000;

export const aiApi = {
    autoListing(imageUrls: string[]) {
        return request<AutoListingResult>('/ai/auto-listing', {
            method: 'POST',
            body: imageUrls,
            timeout: AI_TIMEOUT,
        });
    },

    answerQuestion(data: QaRequest) {
        return request<QaResponse>('/ai/qa', {
            method: 'POST',
            body: data,
            timeout: AI_TIMEOUT,
        });
    },

    /** AI 对话（多轮 Agent + 知识库引用溯源，非流式） */
    chat(data: ChatRequest) {
        return request<ChatAnswer>('/ai/chat', {
            method: 'POST',
            body: data,
            timeout: AI_TIMEOUT,
        });
    },

    /** 知识库检索（RAG 检索侧演示） */
    knowledgeSearch(keyword: string, topK = 5) {
        return request<KnowledgeHit[]>('/ai/knowledge/search', {
            method: 'GET',
            params: { keyword, topK },
            timeout: AI_TIMEOUT,
        });
    },

    /** AI 输出反馈（👍/👎 反馈飞轮） */
    feedback(data: ChatFeedbackRequest) {
        return request<void>('/ai/feedback', {
            method: 'POST',
            body: data,
            timeout: AI_TIMEOUT,
        });
    },

    /** SSE 流式对话（fetch + ReadableStream，可带 Authorization 头） */
    chatStream(data: ChatRequest, onEvent: (event: ChatStreamEvent) => void, signal?: AbortSignal) {
        return streamChat('/ai/chat/stream', data, onEvent, signal);
    },
};
