import { useState, useRef, useEffect, type FormEvent } from 'react';
import { Bot, Send } from 'lucide-react';
import type { QaRequest } from '@/api/aiApi';
import type { QaHistoryItem } from '@/hooks/useAiQa';
import './ai-components.css';

interface AiQaPanelProps {
    product: {
        id: number | string;
        title: string;
        description: string;
        categoryName: string;
        price: number | string;
        conditionLevel: number | string;
        sellerName: string;
        sellerCreditLevel?: string;
    };
    onAsk: (request: QaRequest) => void;
    qaHistory: QaHistoryItem[];
    isLoading: boolean;
}

function AiQaPanel({ product, onAsk, qaHistory, isLoading }: AiQaPanelProps) {
    const [inputValue, setInputValue] = useState('');
    const historyEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        historyEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [qaHistory]);

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        const question = inputValue.trim();
        if (!question || isLoading) return;

        const request: QaRequest = {
            productId: Number(product.id),
            question,
            productName: product.title,
            productDescription: product.description,
            categoryName: product.categoryName,
            price: String(product.price),
            conditionLevel: String(product.conditionLevel),
            sellerName: product.sellerName,
            sellerCreditLevel: product.sellerCreditLevel ?? '',
        };

        onAsk(request);
        setInputValue('');
    }

    return (
        <div className="ai-qa-panel">
            <div className="qa-header">
                <Bot size={20} />
                <span>AI 智能问答</span>
            </div>

            <div className="qa-history">
                {qaHistory.length === 0 ? (
                    <div className="qa-empty">
                        <p>向 AI 询问关于商品的任何问题</p>
                    </div>
                ) : (
                    qaHistory.map((item, index) => (
                        <div key={index} className="qa-item">
                            <div className="qa-question">
                                <span>{item.question}</span>
                            </div>
                            <div className="qa-answer">
                                <span>{item.answer.answer}</span>
                            </div>
                        </div>
                    ))
                )}
                <div ref={historyEndRef} />
            </div>

            <form className="qa-input-form" onSubmit={handleSubmit}>
                <input
                    className="qa-input"
                    type="text"
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    placeholder="输入您的问题..."
                    disabled={isLoading}
                />
                <button
                    className="qa-send-btn"
                    type="submit"
                    disabled={isLoading || !inputValue.trim()}
                >
                    <Send size={18} />
                </button>
            </form>
        </div>
    );
}

export default AiQaPanel;