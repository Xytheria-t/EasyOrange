import { useState, useEffect, useRef } from 'react'
import { Button } from '@/components/ui/button'
import './ai-features.css'

type StepStatus = 'pending' | 'running' | 'done'

interface PipelineStep {
  id: string
  index: number
  side: 'seller' | 'buyer'
  icon: string
  title: string
  subtitle: string
  detail: string
  durationMs: number
  status: StepStatus
}

const PIPELINE_STEPS: PipelineStep[] = [
  {
    id: 'pricing',
    index: 1,
    side: 'seller',
    icon: '💎',
    title: 'AI 资产估值',
    subtitle: '上传图片 · 3 秒定价',
    detail: '基于同款成交均价 + 视觉评估 + 信用加权,生成建议售价',
    durationMs: 800,
    status: 'pending'
  },
  {
    id: 'copy',
    index: 2,
    side: 'seller',
    icon: '✍️',
    title: 'AI 智能写描述',
    subtitle: '30 秒生成标题 + 卖点',
    detail: '通义千问 VL 提炼图片卖点,生成 3 套不同调性的标题与描述',
    durationMs: 1200,
    status: 'pending'
  },
  {
    id: 'listing',
    index: 3,
    side: 'seller',
    icon: '🚀',
    title: 'AI 一键发布',
    subtitle: '描述/类目/价格自动填充',
    detail: '填好图和价,AI 补全标题、卖点、类目与适配关键词',
    durationMs: 1000,
    status: 'pending'
  },
  {
    id: 'search',
    index: 4,
    side: 'buyer',
    icon: '🔍',
    title: 'AI 智能找货',
    subtitle: '说人话就能找到',
    detail: '"想要 500 以内的桌面摆件" — 自然语言 → 精准匹配',
    durationMs: 900,
    status: 'pending'
  },
  {
    id: 'evaluate',
    index: 5,
    side: 'buyer',
    icon: '🛡️',
    title: 'AI 资产核验',
    subtitle: '实物拍照验货 / 虚拟凭证核查',
    detail: '识别实物瑕疵与描述差异,核验虚拟资产凭证有效性,给你一份"交割清单"',
    durationMs: 1100,
    status: 'pending'
  },
  {
    id: 'credit',
    index: 6,
    side: 'buyer',
    icon: '📊',
    title: 'AI 信用画像',
    subtitle: '5 维雷达图 · 可解释',
    detail: '描述准确度 / 沟通及时度 / 发货速度 / 售后口碑 / 历史评价',
    durationMs: 700,
    status: 'pending'
  }
]

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
  )
}

function StewardDailyReport() {
  return (
    <div className="steward-daily-card glass-card" data-testid="steward-daily-card">
      <div className="daily-card-header">
        <div className="daily-card-avatar">
          <span className="daily-avatar-emoji">🤖</span>
          <div className="daily-avatar-pulse" />
        </div>
        <div className="daily-card-title">
          <h3>AI 资产管理 · 今日工作日报</h3>
          <p>2026-06-25 · 你不在的时候,AI 已经帮你做了这些事</p>
        </div>
      </div>

      <div className="daily-card-grid">
        <div className="daily-item">
          <span className="daily-item-num">3</span>
          <span className="daily-item-label">件资产已智能定价</span>
        </div>
        <div className="daily-item-sep" />
        <div className="daily-item">
          <span className="daily-item-num">2</span>
          <span className="daily-item-label">份新文案待你确认</span>
        </div>
        <div className="daily-item-sep" />
        <div className="daily-item">
          <span className="daily-item-num">1</span>
          <span className="daily-item-label">件资产已发布上线</span>
        </div>
        <div className="daily-item-sep" />
        <div className="daily-item daily-item-highlight">
          <span className="daily-item-num">¥2,840</span>
          <span className="daily-item-label">本月预计多回款</span>
        </div>
      </div>

      <div className="daily-card-footer">
        <span className="daily-footer-tip">📌 你只负责决策,过程 AI 全包</span>
        <Button variant="outline" className="daily-footer-btn">查看完整工作日志 →</Button>
      </div>
    </div>
  )
}

function AIFeaturesSection() {
  const [activeIndex, setActiveIndex] = useState(0)
  const [completedCount, setCompletedCount] = useState(0)
  const cycleRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const prefersReducedMotion = useRef(false)

  useEffect(() => {
    prefersReducedMotion.current = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  }, [])

  useEffect(() => {
    if (prefersReducedMotion.current) {
      setCompletedCount(PIPELINE_STEPS.length)
      setActiveIndex(PIPELINE_STEPS.length - 1)
      return
    }

    const tick = () => {
      setActiveIndex(prev => {
        const next = (prev + 1) % (PIPELINE_STEPS.length + 2)
        if (next === PIPELINE_STEPS.length) {
          setCompletedCount(PIPELINE_STEPS.length)
          setTimeout(() => {
            setCompletedCount(0)
            setActiveIndex(0)
          }, 2500)
          return prev
        }
        if (next < prev || prev === PIPELINE_STEPS.length - 1) {
          setCompletedCount(0)
        } else {
          setCompletedCount(next + 1)
        }
        return next
      })
    }

    cycleRef.current = setInterval(tick, 2200)
    return () => {
      if (cycleRef.current) { clearInterval(cycleRef.current) }
    }
  }, [])

  const sellerSteps = PIPELINE_STEPS.filter(s => s.side === 'seller')
  const buyerSteps = PIPELINE_STEPS.filter(s => s.side === 'buyer')

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
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
            </svg>
            <span>AI 资产管理</span>
          </div>
          <div className="ai-features-title-group">
            <span className="ai-features-label">4 步闭环 · 双端对称</span>
            <h2 className="ai-features-title">资产方省心 · 认领方放心</h2>
          </div>
          <p className="ai-features-subtitle">
            资产方侧,AI 替你估值、写描述、智能发布 · 认领方侧,AI 帮你找货、评估、看清信用
          </p>
        </div>

        <StewardDailyReport />

        <div className="pipeline-board">
          <div className="pipeline-column">
            <div className="pipeline-column-header">
              <span className="pipeline-column-tag pipeline-column-tag-seller">资产方</span>
              <h3>资产方侧 · 3 步发布</h3>
              <p>传图写价,AI 补全其余信息</p>
            </div>
            <div className="pipeline-column-list">
              {sellerSteps.map(step => {
                const isActive = PIPELINE_STEPS[activeIndex]?.id === step.id
                const isDone = completedCount > (PIPELINE_STEPS.findIndex(s => s.id === step.id))
                return (
                  <PipelineStepRow
                    key={step.id}
                    step={step}
                    isActive={isActive}
                    isDone={isDone}
                  />
                )
              })}
            </div>
          </div>

          <div className="pipeline-divider" aria-hidden="true">
            <div className="pipeline-divider-line" />
            <span className="pipeline-divider-label">AI 能力 · 一肩挑双端</span>
            <div className="pipeline-divider-line" />
          </div>

          <div className="pipeline-column">
            <div className="pipeline-column-header">
              <span className="pipeline-column-tag pipeline-column-tag-buyer">认领方</span>
              <h3>认领方侧 · 3 步安心</h3>
              <p>找得到 · 看得清 · 买得放心</p>
            </div>
            <div className="pipeline-column-list">
              {buyerSteps.map(step => {
                const isActive = PIPELINE_STEPS[activeIndex]?.id === step.id
                const isDone = completedCount > (PIPELINE_STEPS.findIndex(s => s.id === step.id))
                return (
                  <PipelineStepRow
                    key={step.id}
                    step={step}
                    isActive={isActive}
                    isDone={isDone}
                  />
                )
              })}
            </div>
          </div>
        </div>

        <div className="ai-features-stats">
          <div className="ai-stat-item">
            <div className="ai-stat-value">
              <span className="gradient-text">3 秒</span>
            </div>
            <div className="ai-stat-label">AI 定价</div>
          </div>
          <div className="ai-stat-divider" />
          <div className="ai-stat-item">
            <div className="ai-stat-value">
              <span className="gradient-text">24h</span>
            </div>
            <div className="ai-stat-label">AI 在线</div>
          </div>
          <div className="ai-stat-divider" />
          <div className="ai-stat-item">
            <div className="ai-stat-value">
              <span className="gradient-text">98%</span>
            </div>
            <div className="ai-stat-label">AI 估值准确</div>
          </div>
          <div className="ai-stat-divider" />
          <div className="ai-stat-item">
            <div className="ai-stat-value">
              <span className="gradient-text">0</span>
            </div>
            <div className="ai-stat-label">需要你盯的</div>
          </div>
        </div>
      </div>
    </section>
  )
}

export default AIFeaturesSection
