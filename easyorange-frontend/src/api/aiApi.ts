import type { ChatFeedbackRequest, ChatRequest, ChatStreamEvent } from '@/types/ai';
import { request } from './core/request';
import { streamChat } from './core/stream';

export interface AutoListingResult {
    title: string;
    description: string;
    price: number;
    categoryName: string;
    /** 成色等级 "1"~"4"（后端按枚举 code 传输，与发布表单同形） */
    conditionLevel: string;
    location: string;
}

/**
 * AI 调用专用超时：LLM 单次生成远慢于普通接口（实测视觉识别 ~12s、文案生成 6~28s，
 * 视供应商档位而定），沿用 10s 默认值会让请求被前端中断、后端白算一次。
 * 取值必须高于后端供应商超时（easyorange.ai.text.timeout 30s / vision.timeout 60s），
 * 否则后端自己的降级结果来不及返回，前端先断在超时上。
 */
const AI_TIMEOUT = 90000;

export const aiApi = {
    /**
     * 流式上架识别 — 事件只有 step / done / error（发布产出是结构化表单不是散文，后端无 token / sources）。
     * done 的 data 是 AutoListingResult 的序列化 JSON 文本，调用方自行 JSON.parse。
     */
    autoListingStream(imageUrls: string[], onEvent: (event: ChatStreamEvent) => void, signal?: AbortSignal) {
        return streamChat('/ai/auto-listing/stream', imageUrls, onEvent, signal);
    },

    /** AI 输出反馈（反馈飞轮） */
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
