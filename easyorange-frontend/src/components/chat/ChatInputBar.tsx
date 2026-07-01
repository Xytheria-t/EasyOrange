import { Paperclip, Send, Smile } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';

interface ChatInputBarProps {
    onSend: (content: string) => void;
    onTyping: () => void;
    isDisabled?: boolean;
}

function ChatInputBar({ onSend, onTyping, isDisabled = false }: ChatInputBarProps) {
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
        <div className="chat-input-bar">
            <div className="chat-input-bar-inner">
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="chat-input-action-btn"
                    aria-label="附件"
                    disabled={isDisabled}
                >
                    <Paperclip size={20} />
                </Button>

                <div className="chat-input-wrapper">
                    <Textarea
                        ref={textareaRef}
                        value={value}
                        onChange={handleChange}
                        onKeyDown={handleKeyDown}
                        disabled={isDisabled}
                        rows={1}
                        placeholder={isDisabled ? '' : '输入消息...'}
                        className="chat-textarea"
                        style={{ maxHeight: 120 }}
                    />
                </div>

                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="chat-input-action-btn"
                    aria-label="表情"
                    disabled={isDisabled}
                >
                    <Smile size={20} />
                </Button>

                <Button
                    type="button"
                    size="icon"
                    onClick={handleSubmit}
                    disabled={!value.trim() || isDisabled}
                    className="chat-send-btn"
                    aria-label="发送"
                >
                    <Send size={18} className="-rotate-[15deg] translate-x-[1px]" />
                </Button>
            </div>
        </div>
    );
}

export default ChatInputBar;
