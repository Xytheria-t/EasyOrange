import { Send } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';

interface ChatInputBarProps {
    onSend: (content: string) => void;
    onTyping: () => void;
    isDisabled?: boolean;
    /** 禁用态占位文案（如系统通知会话）；缺省仍为空串 */
    disabledPlaceholder?: string;
}

function ChatInputBar({ onSend, onTyping, isDisabled = false, disabledPlaceholder }: ChatInputBarProps) {
    const textareaRef = useRef<HTMLTextAreaElement>(null);
    const [value, setValue] = useState('');
    const typingTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const adjustHeight = useCallback(() => {
        const el = textareaRef.current;
        if (!el) {
            return;
        }
        el.style.height = 'auto';
        el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
    }, []);

    const handleTypingDebounced = useCallback(() => {
        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
        }
        onTyping();
        typingTimerRef.current = setTimeout(() => {
            typingTimerRef.current = null;
        }, 2000);
    }, [onTyping]);

    useEffect(() => {
        return () => {
            if (typingTimerRef.current) {
                clearTimeout(typingTimerRef.current);
            }
        };
    }, []);

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit();
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        setValue(e.target.value);
        adjustHeight();
        if (e.target.value.trim()) {
            handleTypingDebounced();
        }
    };

    const handleSubmit = () => {
        const trimmed = value.trim();
        if (!trimmed || isDisabled) {
            return;
        }
        onSend(trimmed);
        setValue('');
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
        }
    };

    return (
        <div className="chat-input-bar-inner">
            <div className="chat-input-wrapper">
                <Textarea
                    ref={textareaRef}
                    value={value}
                    onChange={handleChange}
                    onKeyDown={handleKeyDown}
                    disabled={isDisabled}
                    rows={1}
                    aria-label="消息内容"
                    placeholder={isDisabled ? (disabledPlaceholder ?? '') : '输入消息…'}
                    className="chat-textarea max-h-[120px]"
                />
                {/* 快捷键提示常驻：塞在占位符里，用户一开始打字就再也看不到了 */}
                {!isDisabled && (
                    <div className="chat-input-hint">
                        <span>Enter 发送 · Shift + Enter 换行</span>
                    </div>
                )}
            </div>

            <Button
                type="button"
                size="icon"
                onClick={handleSubmit}
                disabled={!value.trim() || isDisabled}
                className="chat-send-btn"
                aria-label="发送"
            >
                <Send size={17} />
            </Button>
        </div>
    );
}

export default ChatInputBar;
