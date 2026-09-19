/** AI 对话（Agent 编排）— 与后端 /api/ai/chat 协议对齐 */
export interface ChatRequest {
    question: string;
    sessionId?: string;
    forceFresh?: boolean;
}

export interface ChatAnswer {
    answer: string;
    sources: string[];
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
    | { type: 'sources'; data: string[] }
    | { type: 'done'; data: string }
    | { type: 'error'; data: string };
