import {
    AlertTriangle,
    Bookmark,
    BookOpen,
    Brain,
    CheckCircle2,
    ChevronDown,
    FileSearch,
    GitCompare,
    List,
    Search,
    Sparkles,
    TrendingUp,
} from 'lucide-react';
import { useEffect, useId, useState } from 'react';
import type { AgentLoopOutcome, AgentStep } from '@/types/ai';
import './ThinkingProcess.css';

/**
 * Agent 工具循环各步骤的展示文案与所属层（与后端 ChatTools / ListingTools 工具面并集对齐）。
 * 后端加工具必须同步这里，否则该步在前端渲染成裸工具名——`PlaygroundPage.test.tsx` 有断言兜底。
 *
 * 层标出来是因为它是「不是 workflow」在界面上唯一看得见的实证：检索类工具谁都能调，
 * 真正让下一步依赖上一步的是计算类（行情统计 / 多件比对，零模型调用、纯确定性计算）。
 */
const STEP_META: Record<string, { label: string; layer: string }> = {
    knowledge_search: { label: '查规则', layer: '检索' },
    product_search: { label: '找资产', layer: '检索' },
    product_detail: { label: '看详情', layer: '检索' },
    list_categories: { label: '查类目', layer: '检索' },
    market_price_stats: { label: '看行情', layer: '计算' },
    compare_assets: { label: '比候选', layer: '计算' },
    remember_preference: { label: '记偏好', layer: '写入' },
    finish: { label: '生成回答', layer: '收敛' },
};

/**
 * 降级结局的文案 —— 与后端 `ToolLoopOutcome` 的降级出口一一对应（收敛的 finished 不出现在这里）。
 * 三条降级都产出一份正常回答，界面上这是唯一能看出「它是降级产物」的地方。
 */
const DEGRADED_LABELS: Record<string, string> = {
    step_limit: '轮数超限',
    budget: '预算耗尽',
    decision_failed: '决策失败',
};

/** 降级文案；收敛（finished）与未知标签返回 null，即不标注。 */
function degradedText(outcome?: AgentLoopOutcome): string | null {
    const label = outcome ? DEGRADED_LABELS[outcome.outcome] : undefined;
    if (!label || !outcome) {
        return null;
    }
    // 轮数只对「轮数超限」有意义：这条降级要说的就是跑到第几轮被截断
    return outcome.outcome === 'step_limit' ? `${label}（${outcome.rounds} 轮）` : label;
}

function StepIcon({ tool }: { tool: string }) {
    switch (tool) {
        case 'knowledge_search':
            return <BookOpen size={11} aria-hidden="true" />;
        case 'product_search':
            return <Search size={11} aria-hidden="true" />;
        case 'product_detail':
            return <FileSearch size={11} aria-hidden="true" />;
        case 'list_categories':
            return <List size={11} aria-hidden="true" />;
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
export function ThinkingProcess({
    steps,
    thinking,
    outcome,
}: {
    steps: AgentStep[];
    thinking: boolean;
    /** 循环结局（outcome 事件）；收敛为 finished，降级的三条在头部标出来 */
    outcome?: AgentLoopOutcome;
}) {
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

    const degradedLabel = degradedText(outcome);

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
                {degradedLabel && (
                    // 降级不是错误：回答照常产出，所以标记走头部而不是错误气泡
                    <span className="playground-think__degraded">
                        <AlertTriangle size={11} aria-hidden="true" />
                        {`已降级 · ${degradedLabel}`}
                    </span>
                )}
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
                                    {/* 步序取 step 事件而非 DOM 次序：丢事件时仍与后端轮次对齐。
                                        纯视觉标记，顺序对读屏由 DOM 次序传达 */}
                                    <span className="playground-think__ord" aria-hidden="true">
                                        {step.step}
                                    </span>
                                    <span>{step.thought ?? STEP_META[step.tool]?.label ?? step.tool}</span>
                                    {STEP_META[step.tool] && (
                                        <span className="playground-think__layer">{STEP_META[step.tool].layer}</span>
                                    )}
                                </p>
                                {step.observation && (
                                    // 全文直接铺开：截断只留 title 悬浮，触屏与键盘用户读不到后半段
                                    <p className="playground-think__obs">{step.observation}</p>
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
