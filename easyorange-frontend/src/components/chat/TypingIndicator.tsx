interface TypingIndicatorProps {
    userName: string;
    isVisible: boolean;
}

function TypingIndicator({ userName, isVisible }: TypingIndicatorProps) {
    if (!isVisible) {
        return null;
    }

    return (
        // 与对方气泡同行同留白，占位指示才不会比真实消息左移一截
        <div className="msg-row justify-start is-group-start">
            <span className="msg-avatar-col" />
            <div className="typing-indicator">
                <div className="typing-dots" aria-hidden="true">
                    <span className="typing-dot" />
                    <span className="typing-dot" />
                    <span className="typing-dot" />
                </div>
                <span className="typing-text">{userName} 正在输入</span>
            </div>
        </div>
    );
}

export default TypingIndicator;
