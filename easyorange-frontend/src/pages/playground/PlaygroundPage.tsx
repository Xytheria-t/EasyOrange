import {
    AlertCircle,
    ArrowUpRight,
    BookOpen,
    CheckCircle2,
    FileSearch,
    Search,
    Send,
    Sparkles,
    ThumbsDown,
    ThumbsUp,
    User,
} from 'lucide-react';
import { type FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { aiApi } from '@/api/aiApi';
import type { AgentStep, ChatStreamEvent } from '@/types/ai';
import './playground.css';

interface ChatMessage {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    sources: string[];
    steps: AgentStep[];
    status: 'streaming' | 'done' | 'error';
    feedback: 'helpful' | 'unhelpful' | null;
}

/** Agent 工具循环各步骤的展示文案（与后端 AgentLoopRunner 工具面对齐） */
const STEP_LABELS: Record<string, string> = {
    knowledge_search: '查规则',
    product_search: '找资产',
    product_detail: '看详情',
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
        case 'finish':
            return <CheckCircle2 size={11} aria-hidden="true" />;
        default:
            return <Sparkles size={11} aria-hidden="true" />;
    }
}

const SUGGESTED_QUESTIONS = [
    '平台交易流程是什么？',
    '怎么申请退款？',
    '运费由谁承担？',
    '签收后还能退吗？',
    '平台能卖烟酒吗？',
];

const WELCOME_MESSAGE: ChatMessage = {
    id: 'welcome',
    role: 'assistant',
    content:
        '你好，我是 EasyOrange AI 助手，可以回答平台交易、退款、运费、禁售品类等规则问题，也能帮你在在售资产里找货。每次回答会标注知识库来源。',
    sources: [],
    steps: [],
    status: 'done',
    feedback: null,
};

function nextId(): string {
    return `msg-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export default function PlaygroundPage() {
    const [messages, setMessages] = useState<ChatMessage[]>([WELCOME_MESSAGE]);
    const [inputValue, setInputValue] = useState('');
    const [isStreaming, setIsStreaming] = useState(false);
    const sessionIdRef = useRef<string>(`sess-${crypto.randomUUID()}`);
    const abortRef = useRef<AbortController | null>(null);
    const listEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (messages.length > 0) {
            listEndRef.current?.scrollIntoView({ behavior: 'smooth' });
        }
    }, [messages]);

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

        const controller = new AbortController();
        abortRef.current = controller;

        const handleEvent = (event: ChatStreamEvent) => {
            switch (event.type) {
                case 'step':
                    appendStep(assistantMessage.id, event.data);
                    break;
                case 'token':
                    appendToken(assistantMessage.id, event.data);
                    break;
                case 'sources':
                    setMessage(assistantMessage.id, { sources: event.data });
                    break;
                case 'done':
                    setMessage(assistantMessage.id, { content: event.data, status: 'done' });
                    setIsStreaming(false);
                    break;
                case 'error':
                    setMessage(assistantMessage.id, { content: event.data, status: 'error' });
                    setIsStreaming(false);
                    break;
                default:
                    break;
            }
        };

        try {
            await aiApi.chatStream({ question: text, sessionId: sessionIdRef.current }, handleEvent, controller.signal);
        } catch {
            setMessage(assistantMessage.id, { content: '连接中断，请重试', status: 'error' });
            setIsStreaming(false);
        }
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
                <div className="playground__mesh" />
            </div>

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

                <div className="playground__chat" aria-live="polite">
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
                                {message.sources.length > 0 && (
                                    <ul className="playground-msg__sources" aria-label="知识库引用来源">
                                        {message.sources.map(source => (
                                            <li key={source}>
                                                <span
                                                    className="playground-msg__source"
                                                    title={`知识库来源：${source}`}
                                                >
                                                    <BookOpen size={11} aria-hidden="true" />
                                                    来源 · {source}
                                                </span>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                                <div
                                    className={[
                                        'playground-msg__bubble',
                                        message.status === 'error' ? 'playground-msg__bubble--error' : '',
                                        message.status === 'streaming' && message.content ? 'is-streaming' : '',
                                    ]
                                        .filter(Boolean)
                                        .join(' ')}
                                >
                                    {message.status === 'error' && (
                                        <AlertCircle
                                            size={15}
                                            className="playground-msg__error-icon"
                                            aria-hidden="true"
                                        />
                                    )}
                                    {message.content ||
                                        (message.status === 'streaming' && (
                                            <span className="playground-typing" role="status" aria-label="思考中">
                                                <span className="playground-typing__dot" />
                                                <span className="playground-typing__dot" />
                                                <span className="playground-typing__dot" />
                                            </span>
                                        ))}
                                </div>
                                {message.role === 'assistant' &&
                                    message.status === 'done' &&
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
                    <div ref={listEndRef} />
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
                            placeholder="输入问题，如：怎么申请退款？"
                            aria-label="问题输入"
                            autoComplete="off"
                            disabled={isStreaming}
                        />
                        <button
                            type="submit"
                            className="playground__send"
                            disabled={isStreaming || !inputValue.trim()}
                            aria-label="发送"
                        >
                            <Send size={15} />
                        </button>
                    </div>
                    <p className="playground__disclaimer">回答由 AI 生成，请以平台规则原文为准</p>
                </form>
            </section>
        </div>
    );
}
