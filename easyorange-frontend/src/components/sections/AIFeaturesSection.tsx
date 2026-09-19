import { type ReactElement, useEffect, useRef, useState } from 'react';
import './ai-features.css';

const CameraIcon = (): ReactElement => (
    <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
    >
        <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z" />
        <circle cx="12" cy="13" r="3" />
    </svg>
);

const ClipboardCheckIcon = (): ReactElement => (
    <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
    >
        <rect x="8" y="2" width="8" height="4" rx="1" />
        <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
        <path d="m9 14 2 2 4-4" />
    </svg>
);

const SparklesIcon = (): ReactElement => (
    <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
    >
        <path d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z" />
        <path d="M20 3v4" />
        <path d="M22 5h-4" />
    </svg>
);

const SearchIcon = (): ReactElement => (
    <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
    >
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.3-4.3" />
    </svg>
);

const MessageIcon = (): ReactElement => (
    <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
    >
        <path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z" />
    </svg>
);

interface PipelineStep {
    id: string;
    index: number;
    side: 'seller' | 'buyer';
    icon: ReactElement;
    title: string;
    subtitle: string;
    detail: string;
}

const PIPELINE_STEPS: PipelineStep[] = [
    {
        id: 'vision',
        index: 1,
        side: 'seller',
        icon: <CameraIcon />,
        title: '拍照识别 · 单入口',
        subtitle: '一次上传 · 1 次模型调用',
        detail: '同步产出属性、建议价与标题描述——发布路径只有这一个 AI 入口',
    },
    {
        id: 'adopt',
        index: 2,
        side: 'seller',
        icon: <ClipboardCheckIcon />,
        title: '核对即发布',
        subtitle: '建议价与成交价同行可比',
        detail: 'AI 建议价随商品落库,采纳率与偏离分布可量化,不靠 LLM 判分',
    },
    {
        id: 'enhance',
        index: 3,
        side: 'buyer',
        icon: <SparklesIcon />,
        title: '搜索增强',
        subtitle: '4 路并行 Tool Calling',
        detail: '标签 / 市场分析 / 建议问题走本地规则,仅意图识别打模型,超时降级不阻塞',
    },
    {
        id: 'rag',
        index: 4,
        side: 'buyer',
        icon: <SearchIcon />,
        title: '对话式检索',
        subtitle: 'kNN + BM25 双路召回',
        detail: 'RRF 融合排名,只检索在售资产,答案带 [来源:标题] 溯源',
    },
    {
        id: 'chat',
        index: 5,
        side: 'buyer',
        icon: <MessageIcon />,
        title: '流式商品问答',
        subtitle: 'SSE 逐字输出 · 多轮记忆',
        detail: '会话窗口 24h + 偏好画像,答不上来不硬编,超预算直接拦截',
    },
];

function PipelineStepRow({ step, isActive, isDone }: { step: PipelineStep; isActive: boolean; isDone: boolean }) {
    return (
        <div
            className={`pipeline-step ${isActive ? 'is-active' : ''} ${isDone ? 'is-done' : ''}`}
            data-testid={`pipeline-step-${step.id}`}
        >
            <div className="step-top">
                <span className="step-icon-box">
                    <span className="step-icon">{step.icon}</span>
                </span>
                <span className="step-index-num" aria-hidden="true">
                    0{step.index}
                </span>
            </div>
            <div className="step-title-row">
                <h3 className="step-title">{step.title}</h3>
                {isActive && <span className="step-running-badge">AI 工作中</span>}
                {isDone && !isActive && <span className="step-done-badge">✓ 完成</span>}
            </div>
            <p className="step-subtitle">{step.subtitle}</p>
            <p className="step-detail">{step.detail}</p>
            {isActive && (
                <div className="step-progress">
                    <div className="step-progress-bar" />
                </div>
            )}
        </div>
    );
}

const LANES = [
    {
        side: 'seller',
        tag: '资产方',
        title: '发布助手单入口',
        desc: '拍一张照,建议价与描述一次生成',
    },
    {
        side: 'buyer',
        tag: '认领方',
        title: '对话式找货',
        desc: '找得到 · 有出处 · 答得诚实',
    },
] as const;

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

                <div className="pipeline-lanes">
                    {LANES.map(lane => {
                        const laneSteps = PIPELINE_STEPS.filter(step => step.side === lane.side);
                        return (
                            <div key={lane.side} className={`pipeline-lane lane-${lane.side}`}>
                                <div className="lane-info">
                                    <span className={`lane-tag lane-tag-${lane.side}`}>{lane.tag}</span>
                                    <h3 className="lane-title">{lane.title}</h3>
                                    <p className="lane-desc">{lane.desc}</p>
                                </div>
                                <div className="lane-cards">
                                    {laneSteps.map(step => {
                                        const isActive = PIPELINE_STEPS[activeIndex]?.id === step.id;
                                        const isDone = completedCount > PIPELINE_STEPS.findIndex(s => s.id === step.id);
                                        return (
                                            <PipelineStepRow
                                                key={step.id}
                                                step={step}
                                                isActive={isActive}
                                                isDone={isDone}
                                            />
                                        );
                                    })}
                                </div>
                            </div>
                        );
                    })}
                </div>

                <p className="pipeline-footnote">
                    2 条主线链路 · 发布路径 1 次模型调用 · 4 路并行编排 · 金标准 35 条进 CI
                </p>
            </div>
        </section>
    );
}

export default AIFeaturesSection;
