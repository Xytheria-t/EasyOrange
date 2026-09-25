import {
    Bookmark,
    BookOpen,
    Brain,
    CheckCircle2,
    ChevronDown,
    FileSearch,
    GitCompare,
    Search,
    Sparkles,
    TrendingUp,
} from 'lucide-react';
import { useEffect, useId, useState } from 'react';
import type { AgentStep } from '@/types/ai';

/**
 * Agent 工具循环各步骤的展示文案（与后端 AgentLoopRunner 工具面对齐）。
 * 后端加工具必须同步这里，否则该步在前端渲染成裸工具名——`PlaygroundPage.test.tsx` 有断言兜底。
 */
const STEP_LABELS: Record<string, string> = {
    knowledge_search: '查规则',
    product_search: '找资产',
    product_detail: '看详情',
    market_price_stats: '看行情',
    compare_assets: '比候选',
    remember_preference: '记偏好',
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
        case 'market_price_stats':
            return <TrendingUp size={11} aria-hidden="true" />;
        case 'compare_assets':
            return <GitCompare size={11} aria-hidden="true" />;
        case 'remember_preference':
            return <Bookmark size={11} aria-hidden="true" />;
        case 'finish':
            return <CheckCircle2 size={11} aria-hidden="true" />;
        default:
            return <Sparkles size={11} aria-hidden="true" />;
    }
}

/**
 * 思考过程面板 — Agent ReAct 循环的可视化（步骤事件实时流入：thought + 观察摘要）。
 *
 * 展开策略模仿推理模型的「思考中」面板：正文 token 未开始时自动展开实时跟进
 * （2s~7s 的决策等待段因此有进度叙事，而不是干等），正文开始后自动收起成一行
 * 摘要不挡回答；点击头部可随时展开回看，手动选择优先于自动行为（重试清空步骤时复位）。
 */
export function ThinkingProcess({ steps, thinking }: { steps: AgentStep[]; thinking: boolean }) {
    // null = 跟随自动策略；点击头部后固定为用户的选择，不再被自动行为翻转
    const [manualExpanded, setManualExpanded] = useState<boolean | null>(null);
    const expanded = manualExpanded ?? thinking;
    // 每条助手消息一个面板实例：aria-controls 指向的列表 id 不能撞（useId 保证唯一）
    const panelId = useId();

    // 重试会原位清空步骤重新流入：复位手动选择，让新一轮重新跟随自动策略
    useEffect(() => {
        if (steps.length === 0) {
            setManualExpanded(null);
        }
    }, [steps.length]);

    return (
        <section className="playground-think" aria-label="Agent 思考过程">
            <button
                type="button"
                className="playground-think__head"
                aria-expanded={expanded}
                aria-controls={panelId}
                onClick={() => setManualExpanded(!expanded)}
            >
                <Brain size={13} aria-hidden="true" />
                <span className="playground-think__head-label">{thinking ? '思考中' : '已思考'}</span>
                <span className="playground-think__head-meta">{steps.length} 步</span>
                <ChevronDown size={13} className="playground-think__chevron" aria-hidden="true" />
            </button>
            {expanded && (
                <ol id={panelId} className="playground-think__list">
                    {steps.map((step, index) => (
                        <li
                            key={`${step.step}-${step.tool}`}
                            className="playground-think__item"
                            style={{ animationDelay: `${index * 70}ms` }}
                        >
                            <span className="playground-think__icon" aria-hidden="true">
                                <StepIcon tool={step.tool} />
                            </span>
                            <div className="playground-think__text">
                                <p className="playground-think__thought">
                                    {step.thought ?? STEP_LABELS[step.tool] ?? step.tool}
                                </p>
                                {step.observation && (
                                    // title 给全量观察（悬浮可读），界面里截断防长观察淹没面板
                                    <p className="playground-think__obs" title={step.observation}>
                                        {step.observation}
                                    </p>
                                )}
                            </div>
                        </li>
                    ))}
                    {thinking && (
                        <li className="playground-think__item playground-think__item--pending">
                            <span className="playground-think__icon" aria-hidden="true">
                                <Brain size={11} />
                            </span>
                            <p className="playground-think__thought">正在决定下一步…</p>
                        </li>
                    )}
                </ol>
            )}
        </section>
    );
}
