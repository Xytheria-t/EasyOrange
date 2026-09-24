import {
    AlertCircle,
    ArrowUpRight,
    Bookmark,
    BookOpen,
    CheckCircle2,
    FileSearch,
    GitCompare,
    Loader2,
    RefreshCw,
    Search,
    Send,
    Sparkles,
    Square,
    ThumbsDown,
    ThumbsUp,
    TrendingUp,
    User,
} from 'lucide-react';
import { type FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { aiApi } from '@/api/aiApi';
import { StreamAuthError, StreamIdleTimeoutError, StreamRequestError } from '@/api/core/stream';
import { ProductCard } from '@/components/product/ProductCard';
import { useProductsByIds } from '@/hooks/product/useProducts';
import type { AgentStep, ChatSource, ChatStreamEvent } from '@/types/ai';
import { MarkdownContent } from './MarkdownContent';
import './playground.css';

/** 消息状态 — stopped 与 error 分开：前者是用户主动中止，不是故障 */
type MessageStatus = 'streaming' | 'done' | 'error' | 'stopped';

interface ChatMessage {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    sources: ChatSource[];
    steps: AgentStep[];
    status: MessageStatus;
    /** 失败/中止原因 — 与 content 分开存：流已吐出的部分答案要留着，不能被错误文案覆盖 */
    note?: string;
    feedback: 'helpful' | 'unhelpful' | null;
}

/**
 * Agent 工具循环各步骤的展示文案（与后端 AgentLoopRunner 工具面对齐）。
 * 后端加工具必须同步这里，否则该步在前端渲染成裸工具名——`PlaygroundPage.test.tsx` 有断言兜底。
 */
const STEP_LABELS: Record<string, string> = {
    knowledge_search: '查规则',
    product_search: '找资产',
    product_detail: '看详情',
    market_price_stats: '看行情',
    compare_assets: '比候选',
    remember_preference: '记偏好',
    finish: '生成回答',
};

function StepIcon({ tool }: { tool: string }) {
    switch (tool) {
        case 'knowledge_search':
            return <BookOpen size={11} aria-hidden="true" />;
        case 'product_search':
            return <Search size={11} aria-hidden="true" />;
        case 'product_detail':
            return <FileSearch size={11} aria-hidden="true" />;
        case 'market_price_stats':
            return <TrendingUp size={11} aria-hidden="true" />;
        case 'compare_assets':
            return <GitCompare size={11} aria-hidden="true" />;
        case 'remember_preference':
            return <Bookmark size={11} aria-hidden="true" />;
        case 'finish':
            return <CheckCircle2 size={11} aria-hidden="true" />;
        default:
            return <Sparkles size={11} aria-hidden="true" />;
    }
}

/**
 * 首屏快捷问题 —— 找货 4 条 + 规则 2 条。
 *
 * /playground 是「对话式找货」的唯一入口（搜索页已无 AI 增强），首屏若全是规则问答，
 * 用户第一眼看到的却是 FAQ 助手，找货主链路要自己打字才试得出来。找货示例写具体
 * （带预算、品类、用途），比「找点手机」更能演示召回质量。
 */
const SUGGESTED_QUESTIONS = [
    '3000 以内适合拍视频的手机有哪些？',
    '预算 500 的降噪耳机，通勤用',
    '8000 左右能跑 3A 大作的游戏本',
    '校内面交的二手自行车，200 以内',
    '平台交易流程是什么？',
    '平台能卖烟酒吗？',
];

const WELCOME_MESSAGE: ChatMessage = {
    id: 'welcome',
    role: 'assistant',
    content:
        '你好，我是 EasyOrange AI 助手。找货直接说预算、品类和用途，我在在售资产里帮你挑；交易、退款、运费、禁售品类这些规则也能问，每次回答会标注来源。',
    sources: [],
    steps: [],
    status: 'done',
    feedback: null,
};

function nextId(): string {
    return `msg-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

/** 贴底判定余量：一次 token 渲染约长高一行，距底这么近都算「在看最新消息」。 */
const FOLLOW_THRESHOLD = 120;

/** 主动取消（停止按钮 / 卸载）不算故障 */
function isAbortError(e: unknown): boolean {
    return e instanceof DOMException && e.name === 'AbortError';
}

/**
 * 引用来源 — 资产渲染成真商品卡，规则渲染成胶囊。
 *
 * 此前所有来源都渲染成「来源 · 标题」的灰色胶囊：在售资产被标成「知识库来源」，
 * 标错了语义，还点不动 —— Agent 找出了货，用户却拿不到货，「对话式找货」断在最后一步。
 * 现在按 type 分流：资产走公开的 /products/batch 补全真实商品（图片、价格、卖家、
 * 地点都是真的），规则仍是不可点的引文胶囊。
 */
function SourceList({ sources }: { sources: ChatSource[] }) {
    const rules = sources.filter(source => source.type === 'knowledge');
    const assetIds = useMemo(
        () => sources.filter(source => source.type === 'asset').map(source => source.id),
        [sources]
    );
    const { data: products, isPending } = useProductsByIds(assetIds);

    return (
        <div className="playground-msg__sources-wrap">
            {rules.length > 0 && (
                <ul className="playground-msg__sources" aria-label="平台规则引用">
                    {rules.map(source => (
                        <li key={`${source.type}-${source.id}`}>
                            <span className="playground-msg__source" title={`平台规则：${source.title}`}>
                                <BookOpen size={11} aria-hidden="true" />
                                规则 · {source.title}
                            </span>
                        </li>
                    ))}
                </ul>
            )}
            {assetIds.length > 0 && (
                <section className="playground-msg__assets" aria-label="推荐商品">
                    {isPending && (
                        <p className="playground-msg__assets-hint">
                            <Loader2 size={12} aria-hidden="true" />
                            正在加载商品…
                        </p>
                    )}
                    {products?.map((product, index) => (
                        // ProductCard 整体是链接，卡内再套链接会形成嵌套 a
                        <ProductCard key={product.id} product={product} index={index} variant="compact" />
                    ))}
                </section>
            )}
        </div>
    );
}

export default function PlaygroundPage() {
    const [messages, setMessages] = useState<ChatMessage[]>([WELCOME_MESSAGE]);
    const [inputValue, setInputValue] = useState('');
    const [isStreaming, setIsStreaming] = useState(false);
    // 屏幕阅读器播报的唯一出口。消息列表不能挂 aria-live：token 逐字到达会让它
    // 每个字都重播整条消息。只在这里推「开始生成 / 已完成 / 已停止 / 失败」这类状态变化。
    const [announcement, setAnnouncement] = useState('');
    const sessionIdRef = useRef<string>(`sess-${crypto.randomUUID()}`);
    const abortRef = useRef<AbortController | null>(null);
    const chatRef = useRef<HTMLDivElement>(null);
    /** 用户是否贴在消息区底部：贴底 = 流式跟随，上翻读历史 = 停止跟随。 */
    const isPinnedRef = useRef(true);
    const prevCountRef = useRef(messages.length);
    const mountedRef = useRef(false);

    // 依赖 messages（引用随 token 到达而变）而不是 messageCount：新消息追加与流式增长要区别对待。
    // 只依赖 messageCount 的话，流式阶段 effect 根本不跑，长回答会一直长在视口外。
    useEffect(() => {
        const el = chatRef.current;
        if (!el) {
            return;
        }
        if (!mountedRef.current) {
            // 首屏只有欢迎语：滚动会连祖先容器一起对齐，把页头拖出视口，第一眼看不到标题
            mountedRef.current = true;
            return;
        }
        const appended = messages.length !== prevCountRef.current;
        prevCountRef.current = messages.length;
        if (appended) {
            // 发送 / 重试追加新消息：无条件滚到底，用户刚发出的消息必须在视口里
            el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
        } else if (isPinnedRef.current) {
            // 流式增长：只在贴底时跟随（instant，逐 token 用 smooth 会堆积动画）；
            // 用户上翻读历史时不抢滚动位置
            el.scrollTo({ top: el.scrollHeight });
        }
    }, [messages]);

    const handleChatScroll = () => {
        const el = chatRef.current;
        if (!el) {
            return;
        }
        isPinnedRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < FOLLOW_THRESHOLD;
    };

    useEffect(() => {
        return () => abortRef.current?.abort();
    }, []);

    const appendToken = useCallback((messageId: string, token: string) => {
        setMessages(prev => prev.map(msg => (msg.id === messageId ? { ...msg, content: msg.content + token } : msg)));
    }, []);

    const appendStep = useCallback((messageId: string, step: AgentStep) => {
        setMessages(prev => prev.map(msg => (msg.id === messageId ? { ...msg, steps: [...msg.steps, step] } : msg)));
    }, []);

    const setMessage = useCallback((messageId: string, patch: Partial<ChatMessage>) => {
        setMessages(prev => prev.map(msg => (msg.id === messageId ? { ...msg, ...patch } : msg)));
    }, []);

    async function handleSend(question?: string) {
        const text = (question ?? inputValue).trim();
        if (!text || isStreaming) {
            return;
        }
        const userMessage: ChatMessage = {
            id: nextId(),
            role: 'user',
            content: text,
            sources: [],
            steps: [],
            status: 'done',
            feedback: null,
        };
        const assistantMessage: ChatMessage = {
            id: nextId(),
            role: 'assistant',
            content: '',
            sources: [],
            steps: [],
            status: 'streaming',
            feedback: null,
        };
        setMessages(prev => [...prev, userMessage, assistantMessage]);
        setInputValue('');
        setIsStreaming(true);
        setAnnouncement('正在生成回答');
        await startStream(text, assistantMessage.id);
    }

    /**
     * 重试 — 原位重跑该条助手消息：问题不重复入列（重试按钮挂在回答上，问题就在上一条），
     * 已流出的半截回答与失败说明一并清掉，消息条数保持一问一答。
     */
    function handleRetry(messageId: string) {
        if (isStreaming) {
            return;
        }
        const question = findQuestion(messageId);
        if (!question) {
            return;
        }
        setMessage(messageId, {
            content: '',
            sources: [],
            steps: [],
            status: 'streaming',
            note: undefined,
            feedback: null,
        });
        setIsStreaming(true);
        setAnnouncement('正在生成回答');
        void startStream(question, messageId);
    }

    async function startStream(question: string, assistantId: string) {
        const controller = new AbortController();
        abortRef.current = controller;

        const handleEvent = (event: ChatStreamEvent) => {
            switch (event.type) {
                case 'step':
                    appendStep(assistantId, event.data);
                    break;
                case 'token':
                    appendToken(assistantId, event.data);
                    break;
                case 'sources':
                    setMessage(assistantId, { sources: event.data });
                    break;
                case 'done':
                    setMessage(assistantId, { content: event.data, status: 'done' });
                    setAnnouncement('回答已生成');
                    setIsStreaming(false);
                    break;
                case 'error':
                    setMessage(assistantId, { note: event.data, status: 'error' });
                    setAnnouncement('回答生成失败');
                    setIsStreaming(false);
                    break;
                default:
                    break;
            }
        };

        try {
            await aiApi.chatStream({ question, sessionId: sessionIdRef.current }, handleEvent, controller.signal);
            // 走到这里说明收到了终止事件（done/error），状态已在 handleEvent 里落定。
            // streamChat 对「流结束却没有终止事件」会抛错，所以这里不再补设状态 ——
            // 补设会把上面刚落的 error 覆盖成 done，重试按钮随之消失
            setIsStreaming(false);
        } catch (e) {
            if (isAbortError(e)) {
                // 用户主动停止：已流出的部分答案保留，只标状态
                setMessage(assistantId, { status: 'stopped', note: '已停止生成' });
                setAnnouncement('已停止生成');
            } else {
                setAnnouncement('回答生成失败');
                if (e instanceof StreamAuthError) {
                    setMessage(assistantId, { note: e.message, status: 'error' });
                } else if (e instanceof StreamIdleTimeoutError) {
                    setMessage(assistantId, { note: e.message, status: 'error' });
                } else if (e instanceof StreamRequestError) {
                    // 服务端明确拒绝（限流/校验/5xx）：透传服务端文案，别伪装成「连接中断」
                    setMessage(assistantId, { note: e.message, status: 'error' });
                } else {
                    setMessage(assistantId, { note: '连接中断，请重试', status: 'error' });
                }
            }
            setIsStreaming(false);
        } finally {
            abortRef.current = null;
        }
    }

    /** 生成中点发送按钮 = 停止，不新增按钮位（输入区只有一个动作位） */
    function handleStop() {
        abortRef.current?.abort();
    }

    async function handleFeedback(message: ChatMessage, helpful: boolean) {
        if (message.feedback !== null) {
            return;
        }
        setMessage(message.id, { feedback: helpful ? 'helpful' : 'unhelpful' });
        try {
            await aiApi.feedback({
                scope: 'chat',
                question: findQuestion(message.id),
                answer: message.content,
                helpful,
            });
        } catch {
            // 反馈失败不打断对话
        }
    }

    /** 找到该回答对应的用户问题（上一条 user 消息） */
    function findQuestion(messageId: string): string {
        const index = messages.findIndex(msg => msg.id === messageId);
        for (let i = index - 1; i >= 0; i--) {
            if (messages[i].role === 'user') {
                return messages[i].content;
            }
        }
        return '';
    }

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        void handleSend();
    }

    return (
        <div className="playground">
            <div className="playground__bg" aria-hidden="true">
                <span className="playground__orb playground__orb--1" />
                <span className="playground__orb playground__orb--2" />
            </div>
            {/* 状态播报区：常驻 DOM，内容变化才播报；token 流本身不进 live region */}
            <p className="sr-only" role="status">
                {announcement}
            </p>

            <section className="playground__shell">
                <header className="playground__header">
                    <div className="playground__header-icon" aria-hidden="true">
                        <Sparkles size={18} />
                    </div>
                    <div>
                        <h1 className="playground__title">AI 智能助手</h1>
                        <p className="playground__subtitle">多轮 Agent · 知识库溯源 · SSE 流式</p>
                    </div>
                    <span className="playground__status">
                        <span className="playground__status-dot" aria-hidden="true" />
                        Agent 就绪
                    </span>
                </header>

                <div className="playground__chat" ref={chatRef} onScroll={handleChatScroll}>
                    {messages.map(message => (
                        <div
                            key={message.id}
                            className={[
                                'playground-msg',
                                `playground-msg--${message.role}`,
                                message.role === 'assistant' && message.status === 'streaming' ? 'is-streaming' : '',
                            ]
                                .filter(Boolean)
                                .join(' ')}
                        >
                            <div className="playground-msg__avatar" aria-hidden="true">
                                {message.role === 'user' ? <User size={15} /> : <Sparkles size={14} />}
                            </div>
                            <div className="playground-msg__body">
                                {message.steps.length > 0 && (
                                    <ul className="playground-msg__steps" aria-label="Agent 执行步骤">
                                        {message.steps.map((step, index) => (
                                            <li
                                                key={`${step.step}-${step.tool}`}
                                                className="playground-msg__step"
                                                style={{ animationDelay: `${index * 70}ms` }}
                                                title={step.observation ?? undefined}
                                            >
                                                <span className="playground-msg__step-icon" aria-hidden="true">
                                                    <StepIcon tool={step.tool} />
                                                </span>
                                                {step.thought ?? STEP_LABELS[step.tool] ?? step.tool}
                                            </li>
                                        ))}
                                    </ul>
                                )}
                                {message.sources.length > 0 && <SourceList sources={message.sources} />}
                                <div
                                    className={[
                                        'playground-msg__bubble',
                                        message.status === 'streaming' && message.content ? 'is-streaming' : '',
                                    ]
                                        .filter(Boolean)
                                        .join(' ')}
                                >
                                    {message.content ? (
                                        // 用户消息保持纯文本；助手回答走 Markdown（不渲染内嵌 HTML）
                                        message.role === 'assistant' ? (
                                            <MarkdownContent content={message.content} />
                                        ) : (
                                            message.content
                                        )
                                    ) : (
                                        message.status === 'streaming' && (
                                            // 不再自带 live region：开始生成已由页面级
                                            // 状态播报区承担，两处播报会叠成两声
                                            <span className="playground-typing" aria-hidden="true">
                                                <span className="playground-typing__dot" />
                                                <span className="playground-typing__dot" />
                                                <span className="playground-typing__dot" />
                                            </span>
                                        )
                                    )}
                                </div>
                                {message.note && (
                                    <div className="playground-msg__note">
                                        <AlertCircle size={13} aria-hidden="true" />
                                        <span>{message.note}</span>
                                        {(message.status === 'error' || message.status === 'stopped') && (
                                            <button
                                                type="button"
                                                className="playground-msg__retry"
                                                onClick={() => handleRetry(message.id)}
                                            >
                                                <RefreshCw size={12} aria-hidden="true" />
                                                重试
                                            </button>
                                        )}
                                    </div>
                                )}
                                {message.role === 'assistant' &&
                                    (message.status === 'done' || message.status === 'stopped') &&
                                    message.id !== 'welcome' && (
                                        <fieldset className="playground-msg__feedback">
                                            <legend className="sr-only">反馈</legend>
                                            <button
                                                type="button"
                                                className={`playground-msg__feedback-btn ${message.feedback === 'helpful' ? 'is-active' : ''}`}
                                                onClick={() => void handleFeedback(message, true)}
                                                aria-label="有帮助"
                                                aria-pressed={message.feedback === 'helpful'}
                                            >
                                                <ThumbsUp size={13} />
                                            </button>
                                            <button
                                                type="button"
                                                className={`playground-msg__feedback-btn ${message.feedback === 'unhelpful' ? 'is-active' : ''}`}
                                                onClick={() => void handleFeedback(message, false)}
                                                aria-label="没帮助"
                                                aria-pressed={message.feedback === 'unhelpful'}
                                            >
                                                <ThumbsDown size={13} />
                                            </button>
                                        </fieldset>
                                    )}
                            </div>
                        </div>
                    ))}
                </div>

                {messages.length === 1 && (
                    <div className="playground__suggestions">
                        {SUGGESTED_QUESTIONS.map(question => (
                            <button
                                key={question}
                                type="button"
                                className="playground__suggestion"
                                onClick={() => void handleSend(question)}
                                disabled={isStreaming}
                            >
                                {question}
                                <ArrowUpRight size={13} aria-hidden="true" />
                            </button>
                        ))}
                    </div>
                )}

                <form className="playground__composer" onSubmit={handleSubmit}>
                    <div className="playground__composer-bar">
                        <Sparkles size={16} className="playground__composer-icon" aria-hidden="true" />
                        <input
                            type="text"
                            className="playground__composer-field"
                            value={inputValue}
                            onChange={e => setInputValue(e.target.value)}
                            placeholder="输入问题，如：3000 以内适合拍视频的手机"
                            aria-label="问题输入"
                            autoComplete="off"
                            disabled={isStreaming}
                        />
                        {isStreaming ? (
                            // 生成中占用发送按钮位：多一个并列按钮会挤窄输入框，
                            // 而「停止」正是此刻唯一该有的动作
                            <button
                                type="button"
                                className="playground__send playground__send--stop"
                                onClick={handleStop}
                                aria-label="停止生成"
                            >
                                <Square size={13} fill="currentColor" />
                            </button>
                        ) : (
                            <button
                                type="submit"
                                className="playground__send"
                                disabled={!inputValue.trim()}
                                aria-label="发送"
                            >
                                <Send size={15} />
                            </button>
                        )}
                    </div>
                    <p className="playground__disclaimer">回答由 AI 生成，请以平台规则原文为准</p>
                </form>
            </section>
        </div>
    );
}
