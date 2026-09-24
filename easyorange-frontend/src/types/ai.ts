/** AI 对话（Agent 编排）— 与后端 /api/ai/chat 协议对齐 */
export interface ChatRequest {
    question: string;
    sessionId?: string;
    forceFresh?: boolean;
}

/**
 * 引用来源（与后端 ChatSource 对齐）— type 决定前端怎么渲染。
 * 前端拿 id 去查的都是真实记录：模型在正文里写错标题，点开仍会落到真实存在的那一条。
 */
export interface ChatSource {
    /** knowledge = 平台规则引文；asset = 在售资产，可点进商品详情 */
    type: 'knowledge' | 'asset';
    /** 命中文档 ID 或资产 ID */
    id: string;
    title: string;
}

export interface ChatAnswer {
    answer: string;
    sources: ChatSource[];
    sessionId: string;
    /** 本次回答不是模型实时生成（供应商故障时复用 stale 旧回答或降级文案），UI 不应把它当作正常回答展示证据链 */
    degraded: boolean;
}

/** AI 输出反馈（反馈飞轮） */
export interface ChatFeedbackRequest {
    scope: string;
    question: string;
    answer: string;
    helpful: boolean;
    comment?: string;
    callLogId?: string;
}

/** 知识库检索命中（RAG 引用溯源） */
export interface KnowledgeHit {
    docId: string;
    title: string;
    content: string;
    score: number;
}

/** 知识库文档（管理端） */
export interface KnowledgeDoc {
    id: string;
    title: string;
    content: string;
    source: string;
    status: 'PENDING' | 'INDEXED' | 'FAILED';
    chunkCount: number;
    createTime: string;
}

/** Agent 工具循环单步（与后端 AgentStepView 对齐：step 事件载荷，前端步骤可视化） */
export interface AgentStep {
    step: number;
    tool: string;
    thought?: string;
    observation?: string;
}

/** SSE 流式事件（与后端 SseEmitter 事件名对齐） */
export type ChatStreamEvent =
    | { type: 'step'; data: AgentStep }
    | { type: 'token'; data: string }
    | { type: 'sources'; data: ChatSource[] }
    | { type: 'done'; data: string }
    | { type: 'error'; data: string };
