interface AiTagProps {
    tag: string;
}

const TAG_COLORS: Record<string, string> = {
    '💰超值': '#10b981',
    '✅AI审核': '#3b82f6',
    '⭐信用优': '#f59e0b',
    '📸实拍': '#8b5cf6',
};

export function AiTag({ tag }: AiTagProps) {
    const color = TAG_COLORS[tag] || '#6b7280';
    return (
        <span
            className="ai-tag"
            style={{
                backgroundColor: `${color}15`,
                color,
                border: `1px solid ${color}30`,
                borderRadius: '4px',
                padding: '2px 6px',
                fontSize: '11px',
                fontWeight: 500,
                marginLeft: '4px',
                whiteSpace: 'nowrap',
            }}
        >
            {tag}
        </span>
    );
}
