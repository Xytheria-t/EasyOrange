import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

/**
 * 助手气泡 Markdown 渲染 — GFM（表格 / 任务列表）+ 链接新窗口打开。
 * 默认不渲染内嵌 HTML（无 rehype-raw）：模型输出不可信，源码级转义优于事后 sanitize。
 */
export function MarkdownContent({ content }: { content: string }) {
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
                {content}
            </ReactMarkdown>
        </div>
    );
}
