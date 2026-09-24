import { useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

/**
 * 模型按提示词约定在引用处输出 [来源:标题]；裸文本混在正文里读感很差，
 * 转成行内代码令牌后走 .playground-md code 的胶囊样式，与上方来源胶囊视觉同源。
 * 只认成对的中括号；流式渲染中未收完的半截引用按普通文本短暂显示，收完即成胶囊。
 */
const CITATION_PATTERN = /\[来源[::]\s*([^[\]]+)\]/g;

/**
 * 助手气泡 Markdown 渲染 — GFM（表格 / 任务列表）+ 链接新窗口打开。
 * 默认不渲染内嵌 HTML（无 rehype-raw）：模型输出不可信，源码级转义优于事后 sanitize。
 */
export function MarkdownContent({ content }: { content: string }) {
    const normalized = useMemo(() => content.replace(CITATION_PATTERN, '`来源:$1`'), [content]);
    return (
        <div className="playground-md">
            <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                    a: props => <a {...props} target="_blank" rel="noopener noreferrer" />,
                    // 表格套滚动容器：窄气泡里列一多就会把气泡撑破
                    table: props => (
                        <div className="playground-md__table-wrap">
                            <table {...props} />
                        </div>
                    ),
                }}
            >
                {normalized}
            </ReactMarkdown>
        </div>
    );
}
