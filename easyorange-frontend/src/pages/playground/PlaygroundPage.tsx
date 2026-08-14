import { Bot, Send, Sparkles, ThumbsDown, ThumbsUp, User } from 'lucide-react';
import { type FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { aiApi } from '@/api/aiApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import type { ChatStreamEvent } from '@/types/ai';
import './playground.css';

interface ChatMessage {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    sources: string[];
    status: 'streaming' | 'done' | 'error';
    feedback: 'helpful' | 'unhelpful' | null;
}

const SUGGESTED_QUESTIONS = [
    '平台交易流程是什么？',
    '怎么申请退款？',
    '运费由谁承担？',
    '信用分怎么提升？',
    '平台能卖烟酒吗？',
];

const WELCOME_MESSAGE: ChatMessage = {
    id: 'welcome',
    role: 'assistant',
    content:
        '你好，我是 EasyOrange AI 助手 🤖 可以回答平台交易、退款、运费、信用分、禁售品类等规则问题，也能陪你闲聊。每次回答会标注知识库来源。',
    sources: [],
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
            status: 'done',
            feedback: null,
        };
        const assistantMessage: ChatMessage = {
            id: nextId(),
            role: 'assistant',
            content: '',
            sources: [],
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
            <div className="playground__header">
                <Sparkles className="playground__header-icon" aria-hidden="true" />
                <div>
                    <h1 className="playground__title">AI 智能助手</h1>
                    <p className="playground__subtitle">多轮 Agent · 知识库引用溯源 · SSE 流式 · 反馈飞轮</p>
                </div>
            </div>

            <div className="playground__chat" aria-live="polite">
                {messages.map(message => (
                    <div key={message.id} className={`playground-msg playground-msg--${message.role}`}>
                        <div className="playground-msg__avatar" aria-hidden="true">
                            {message.role === 'user' ? <User size={16} /> : <Bot size={16} />}
                        </div>
                        <div className="playground-msg__body">
                            {message.sources.length > 0 && (
                                <div className="playground-msg__sources">
                                    {message.sources.map(source => (
                                        <span key={source} className="playground-msg__source" title="知识库引用来源">
                                            [来源:{source}]
                                        </span>
                                    ))}
                                </div>
                            )}
                            <div
                                className={`playground-msg__bubble ${message.status === 'error' ? 'playground-msg__bubble--error' : ''}`}
                            >
                                {message.content ||
                                    (message.status === 'streaming' && (
                                        <span className="ai-typing-dot" role="status" aria-label="思考中" />
                                    ))}
                            </div>
                            {message.role === 'assistant' && message.status === 'done' && message.id !== 'welcome' && (
                                <fieldset className="playground-msg__feedback">
                                    <legend className="sr-only">反馈</legend>
                                    <button
                                        type="button"
                                        className={`playground-msg__feedback-btn ${message.feedback === 'helpful' ? 'is-active' : ''}`}
                                        onClick={() => void handleFeedback(message, true)}
                                        aria-label="有帮助"
                                        aria-pressed={message.feedback === 'helpful'}
                                    >
                                        <ThumbsUp size={14} />
                                    </button>
                                    <button
                                        type="button"
                                        className={`playground-msg__feedback-btn ${message.feedback === 'unhelpful' ? 'is-active' : ''}`}
                                        onClick={() => void handleFeedback(message, false)}
                                        aria-label="没帮助"
                                        aria-pressed={message.feedback === 'unhelpful'}
                                    >
                                        <ThumbsDown size={14} />
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
                        </button>
                    ))}
                </div>
            )}

            <form className="playground__input" onSubmit={handleSubmit}>
                <Input
                    value={inputValue}
                    onChange={e => setInputValue(e.target.value)}
                    placeholder="输入问题，如：怎么申请退款？"
                    aria-label="问题输入"
                    disabled={isStreaming}
                />
                <Button type="submit" size="icon" disabled={isStreaming || !inputValue.trim()} aria-label="发送">
                    <Send size={18} />
                </Button>
            </form>
        </div>
    );
}
