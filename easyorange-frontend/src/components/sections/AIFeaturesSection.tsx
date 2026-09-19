import { useEffect, useRef, useState } from 'react';
import './ai-features.css';

interface PipelineStep {
    id: string;
    index: number;
    side: 'seller' | 'buyer';
    icon: string;
    title: string;
    subtitle: string;
    detail: string;
}

const PIPELINE_STEPS: PipelineStep[] = [
    {
        id: 'vision',
        index: 1,
        side: 'seller',
        icon: '📸',
        title: '拍照识别 · 单入口',
        subtitle: '一次上传 · 1 次模型调用',
        detail: '同步产出属性、建议价与标题描述——发布路径只有这一个 AI 入口',
    },
    {
        id: 'adopt',
        index: 2,
        side: 'seller',
        icon: '💰',
        title: '核对即发布',
        subtitle: '建议价与成交价同行可比',
        detail: 'AI 建议价随商品落库,采纳率与偏离分布可量化,不靠 LLM 判分',
    },
    {
        id: 'enhance',
        index: 3,
        side: 'buyer',
        icon: '🎯',
        title: '搜索增强',
        subtitle: '4 路并行 Tool Calling',
        detail: '标签 / 市场分析 / 建议问题走本地规则,仅意图识别打模型,超时降级不阻塞',
    },
    {
        id: 'rag',
        index: 4,
        side: 'buyer',
        icon: '🔍',
        title: '对话式检索',
        subtitle: 'kNN + BM25 双路召回',
        detail: 'RRF 融合排名,只检索在售资产,答案带 [来源:标题] 溯源',
    },
    {
        id: 'chat',
        index: 5,
        side: 'buyer',
        icon: '💬',
        title: '流式商品问答',
        subtitle: 'SSE 逐字输出 · 多轮记忆',
        detail: '会话窗口 24h + 偏好画像,答不上来不硬编,超预算直接拦截',
    },
];

function PipelineStepRow({ step, isActive, isDone }: { step: PipelineStep; isActive: boolean; isDone: boolean }) {
    return (
        <div
            className={`pipeline-step ${isActive ? 'is-active' : ''} ${isDone ? 'is-done' : ''} step-${step.side}`}
            data-testid={`pipeline-step-${step.id}`}
        >
            <div className="step-index">
                <span className="step-index-num">0{step.index}</span>
                <span className="step-index-pulse" />
            </div>
            <div className="step-icon-box">
                <span className="step-icon">{step.icon}</span>
            </div>
            <div className="step-body">
                <div className="step-title-row">
                    <h3 className="step-title">{step.title}</h3>
                    {isActive && <span className="step-running-badge">AI 工作中</span>}
                    {isDone && <span className="step-done-badge">✓ 完成</span>}
                </div>
                <p className="step-subtitle">{step.subtitle}</p>
                <p className="step-detail">{step.detail}</p>
                {isActive && (
                    <div className="step-progress">
                        <div className="step-progress-bar" />
                    </div>
                )}
            </div>
        </div>
    );
}

function AIFeaturesSection() {
    const [activeIndex, setActiveIndex] = useState(0);
    const [completedCount, setCompletedCount] = useState(0);
    const cycleRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const prefersReducedMotion = useRef(false);

    useEffect(() => {
        prefersReducedMotion.current = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }, []);

    useEffect(() => {
        if (prefersReducedMotion.current) {
            setCompletedCount(PIPELINE_STEPS.length);
            setActiveIndex(PIPELINE_STEPS.length - 1);
            return;
        }

        const tick = () => {
            setActiveIndex(prev => {
                const next = (prev + 1) % (PIPELINE_STEPS.length + 2);
                if (next === PIPELINE_STEPS.length) {
                    setCompletedCount(PIPELINE_STEPS.length);
                    setTimeout(() => {
                        setCompletedCount(0);
                        setActiveIndex(0);
                    }, 2500);
                    return prev;
                }
                if (next < prev || prev === PIPELINE_STEPS.length - 1) {
                    setCompletedCount(0);
                } else {
                    setCompletedCount(next + 1);
                }
                return next;
            });
        };

        cycleRef.current = setInterval(tick, 2200);
        return () => {
            if (cycleRef.current) {
                clearInterval(cycleRef.current);
            }
        };
    }, []);

    const sellerSteps = PIPELINE_STEPS.filter(s => s.side === 'seller');
    const buyerSteps = PIPELINE_STEPS.filter(s => s.side === 'buyer');

    return (
        <section className="ai-features-section">
            <div className="ai-features-bg">
                <div className="ai-gradient-orb orb-1" />
                <div className="ai-gradient-orb orb-2" />
                <div className="ai-flow-lines" />
            </div>

            <div className="container">
                <div className="ai-features-header">
                    <div className="ai-badge">
                        <span className="ai-badge-dot" />
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
                        </svg>
                        <span>AI 工程化</span>
                    </div>
                    <div className="ai-features-title-group">
                        <span className="ai-features-label">
                            <span className="label-rule" aria-hidden="true" />
                            两条主线
                            <span className="label-rule" aria-hidden="true" />
                        </span>
                        <h2 className="ai-features-title">
                            <span className="title-side title-side-seller">资产方省心</span>
                            <span className="title-dot">·</span>
                            <span className="title-side title-side-buyer">认领方放心</span>
                        </h2>
                    </div>
                    <div className="ai-features-subtitle">
                        <span className="subtitle-side">
                            <span className="subtitle-dot subtitle-dot-seller" aria-hidden="true" />
                            <span>
                                <strong>资产方侧</strong>,拍照识别一次产出建议价与标题描述
                            </span>
                        </span>
                        <span className="subtitle-divider" aria-hidden="true" />
                        <span className="subtitle-side">
                            <span className="subtitle-dot subtitle-dot-buyer" aria-hidden="true" />
                            <span>
                                <strong>认领方侧</strong>,对话式找货、溯源答疑
                            </span>
                        </span>
                    </div>
                </div>

                <div className="pipeline-board">
                    <div className="pipeline-column">
                        <div className="pipeline-column-header">
                            <span className="pipeline-column-tag pipeline-column-tag-seller">资产方</span>
                            <h3>资产方侧 · 发布助手单入口</h3>
                            <p>拍一张照,建议价与描述一次生成</p>
                        </div>
                        <div className="pipeline-column-list">
                            {sellerSteps.map(step => {
                                const isActive = PIPELINE_STEPS[activeIndex]?.id === step.id;
                                const isDone = completedCount > PIPELINE_STEPS.findIndex(s => s.id === step.id);
                                return (
                                    <PipelineStepRow key={step.id} step={step} isActive={isActive} isDone={isDone} />
                                );
                            })}
                        </div>
                    </div>

                    <div className="pipeline-divider" aria-hidden="true">
                        <div className="pipeline-divider-line" />
                        <span className="pipeline-divider-label">两条主线 · 同一套 AI 底座</span>
                        <div className="pipeline-divider-line" />
                    </div>

                    <div className="pipeline-column">
                        <div className="pipeline-column-header">
                            <span className="pipeline-column-tag pipeline-column-tag-buyer">认领方</span>
                            <h3>认领方侧 · 对话式找货</h3>
                            <p>找得到 · 有出处 · 答得诚实</p>
                        </div>
                        <div className="pipeline-column-list">
                            {buyerSteps.map(step => {
                                const isActive = PIPELINE_STEPS[activeIndex]?.id === step.id;
                                const isDone = completedCount > PIPELINE_STEPS.findIndex(s => s.id === step.id);
                                return (
                                    <PipelineStepRow key={step.id} step={step} isActive={isActive} isDone={isDone} />
                                );
                            })}
                        </div>
                    </div>
                </div>

                <div className="ai-features-stats">
                    <div className="ai-stat-item">
                        <div className="ai-stat-value">
                            <span className="gradient-text">2 条</span>
                        </div>
                        <div className="ai-stat-label">AI 主线链路</div>
                    </div>
                    <div className="ai-stat-divider" />
                    <div className="ai-stat-item">
                        <div className="ai-stat-value">
                            <span className="gradient-text">1 次</span>
                        </div>
                        <div className="ai-stat-label">发布路径模型调用</div>
                    </div>
                    <div className="ai-stat-divider" />
                    <div className="ai-stat-item">
                        <div className="ai-stat-value">
                            <span className="gradient-text">4 路</span>
                        </div>
                        <div className="ai-stat-label">并行 Tool Calling</div>
                    </div>
                    <div className="ai-stat-divider" />
                    <div className="ai-stat-item">
                        <div className="ai-stat-value">
                            <span className="gradient-text">35 条</span>
                        </div>
                        <div className="ai-stat-label">金标准用例进 CI</div>
                    </div>
                </div>
            </div>
        </section>
    );
}

export default AIFeaturesSection;
