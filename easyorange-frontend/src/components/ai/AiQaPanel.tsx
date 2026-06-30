import { useState, useRef, useEffect, useCallback, type FormEvent } from 'react';
import { Bot, Send, User, Sparkles, Loader2, Copy, Check } from 'lucide-react';
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
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

function formatTime(date: Date): string {
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
}

function AiQaPanel({ product, onAsk, qaHistory, isLoading }: AiQaPanelProps) {
  const [inputValue, setInputValue] = useState('');
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);
  const historyEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const prevHistoryLenRef = useRef(0);

  useEffect(() => {
    if (qaHistory.length > prevHistoryLenRef.current) {
      historyEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
    prevHistoryLenRef.current = qaHistory.length;
  }, [qaHistory]);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const question = inputValue.trim();
    if (!question || isLoading) {return;}

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

  const handleCopy = useCallback(async (text: string, index: number) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedIndex(index);
      setTimeout(() => setCopiedIndex(null), 2000);
    } catch {
      // silently fail
    }
  }, []);

  const suggestedQuestions = [
    '这个商品成色如何？',
    '价格还能优惠吗？',
    '支持面交吗？',
    '有原装配件吗？',
  ];

  return (
    <div className="ai-qa-panel">
      <div className="qa-header">
        <div className="qa-header-icon">
          <Bot size={16} />
          <div className="qa-header-pulse" />
        </div>
        <div className="qa-header-text">
          <span className="qa-header-title">AI 智能问答</span>
          <span className="qa-header-subtitle">基于商品信息的智能助手</span>
        </div>
        <div className="qa-header-badge">
          <Sparkles size={10} />
          AI
        </div>
      </div>

      <div className="qa-history">
        {qaHistory.length === 0 ? (
          <div className="qa-empty">
            <div className="qa-empty-icon">
              <Bot size={28} />
              <div className="qa-empty-glow" />
              <div className="qa-empty-ring" />
            </div>
            <p className="qa-empty-title">向 AI 询问关于商品的任何问题</p>
            <p className="qa-empty-desc">智能助手将基于商品信息为您提供专业解答</p>
            <div className="qa-suggested-questions">
              {suggestedQuestions.map((q, idx) => (
                <Button
                  key={idx}
                  variant="outline"
                  size="sm"
                  className="qa-suggested-chip"
                  onClick={() => {
                    setInputValue(q);
                    inputRef.current?.focus();
                  }}
                >
                  <Sparkles size={10} />
                  {q}
                </Button>
              ))}
            </div>
          </div>
        ) : (
          <div className="qa-messages">
            {qaHistory.map((item, index) => (
              <div key={index} className="qa-message-group">
                <div className="qa-message user-message">
                  <div className="qa-message-avatar user-avatar">
                    <User size={12} />
                  </div>
                  <div className="qa-message-content">
                    <div className="qa-message-bubble user-bubble">
                      {item.question}
                    </div>
                    <span className="qa-message-time">{formatTime(new Date())}</span>
                  </div>
                </div>
                <div className="qa-message ai-message">
                  <div className="qa-message-avatar ai-avatar">
                    <Bot size={12} />
                    <div className="ai-avatar-ring" />
                  </div>
                  <div className="qa-message-content">
                    <div className="qa-message-bubble ai-bubble">
                      <div className="ai-bubble-content">
                        {item.answer.answer}
                      </div>
                      {item.answer.hasConfidence && (
                        <div className="ai-bubble-badge">
                          <Sparkles size={10} />
                          已确认
                        </div>
                      )}
                    </div>
                    <div className="qa-message-actions">
                      <span className="qa-message-time">{formatTime(new Date())}</span>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="qa-copy-btn"
                        onClick={() => handleCopy(item.answer.answer, index)}
                        title="复制回答"
                      >
                        {copiedIndex === index ? <Check size={12} /> : <Copy size={12} />}
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
            {isLoading && (
              <div className="qa-message ai-message">
                <div className="qa-message-avatar ai-avatar">
                  <Bot size={12} />
                  <div className="ai-avatar-ring" />
                </div>
                <div className="qa-message-content">
                  <div className="qa-message-bubble ai-bubble">
                    <div className="ai-typing">
                      <span className="ai-typing-dot" />
                      <span className="ai-typing-dot" />
                      <span className="ai-typing-dot" />
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
        <div ref={historyEndRef} />
      </div>

      <form className="qa-input-form" onSubmit={handleSubmit}>
        <div className="qa-input-wrapper">
          <Input
            ref={inputRef}
            className="qa-input"
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="输入您的问题..."
            disabled={isLoading}
            maxLength={500}
          />
          {inputValue.length > 400 && (
            <span className="qa-input-count">{inputValue.length}/500</span>
          )}
          <Button
            className={`qa-send-btn ${inputValue.trim() ? 'active' : ''}`}
            type="submit"
            disabled={isLoading || !inputValue.trim()}
            size="icon"
          >
            {isLoading ? <Loader2 size={16} className="qa-send-spinner" /> : <Send size={16} />}
          </Button>
        </div>
      </form>
    </div>
  );
}

export default AiQaPanel;