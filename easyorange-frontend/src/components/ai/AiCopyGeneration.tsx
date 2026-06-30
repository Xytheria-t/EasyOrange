import { useState } from 'react'
import { Sparkles, Loader2, FileText, Check, ChevronDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { CopyGenerationResult } from '@/api/aiApi'
import './ai-components.css'

type CopyStyle = 'standard' | 'detailed' | 'concise' | 'emotional'

interface AiCopyGenerationProps {
  productName: string
  onGenerate: (style: CopyStyle) => void
  onApply: (result: CopyGenerationResult) => void
  result: CopyGenerationResult | null
  isLoading: boolean
}

const STYLE_OPTIONS: { value: CopyStyle; label: string; desc: string }[] = [
  { value: 'standard', label: '标准推荐', desc: '平衡描述，适合大多数商品' },
  { value: 'detailed', label: '详细详尽', desc: '全面描述所有细节信息' },
  { value: 'concise', label: '简洁明了', desc: '突出核心卖点，简短有力' },
  { value: 'emotional', label: '情感共鸣', desc: '温暖感性，讲述商品故事' },
]

export function AiCopyGeneration({
  productName,
  onGenerate,
  onApply,
  result,
  isLoading,
}: AiCopyGenerationProps) {
  const [selectedStyle, setSelectedStyle] = useState<CopyStyle>('standard')
  const [showStylePicker, setShowStylePicker] = useState(false)

  const canGenerate = productName.trim().length > 0 && !isLoading

  return (
    <div className="ai-copy-generation">
      <div className="ai-copy-header">
        <div className="ai-copy-header-left">
          <Sparkles size={16} className="ai-copy-sparkle" />
          <span className="ai-copy-title">AI 智能文案</span>
        </div>
        <div className="ai-copy-style-selector">
          <Button
            variant="outline"
            size="sm"
            className="ai-copy-style-btn"
            onClick={() => setShowStylePicker(!showStylePicker)}
            type="button"
          >
            <FileText size={14} />
            <span>{STYLE_OPTIONS.find(s => s.value === selectedStyle)?.label}</span>
            <ChevronDown size={12} className={`ai-copy-chevron ${showStylePicker ? 'open' : ''}`} />
          </Button>
          {showStylePicker && (
            <div className="ai-copy-style-dropdown">
              {STYLE_OPTIONS.map(style => (
                <Button
                  key={style.value}
                  variant="ghost"
                  className={`ai-copy-style-option ${selectedStyle === style.value ? 'active' : ''}`}
                  onClick={() => {
                    setSelectedStyle(style.value)
                    setShowStylePicker(false)
                  }}
                  type="button"
                >
                  <span className="style-label">{style.label}</span>
                  <span className="style-desc">{style.desc}</span>
                </Button>
              ))}
            </div>
          )}
        </div>
      </div>

      <Button
        className={`ai-copy-generate-btn ${!canGenerate ? 'disabled' : ''}`}
        onClick={() => onGenerate(selectedStyle)}
        disabled={!canGenerate}
        type="button"
      >
        {isLoading ? (
          <>
            <Loader2 size={16} className="animate-spin" />
            AI 生成中...
          </>
        ) : (
          <>
            <Sparkles size={16} />
            生成商品描述
          </>
        )}
      </Button>

      {!productName.trim() && !isLoading && (
        <p className="ai-copy-hint">请先填写商品名称，AI 将为您生成专业的商品描述</p>
      )}

      {result && (
        <div className="ai-copy-result">
          <div className="ai-copy-result-header">
            <Sparkles size={14} />
            <span>生成结果（{STYLE_OPTIONS.find(s => s.value === result.style)?.label || result.style}）</span>
          </div>
          <div className="ai-copy-result-body">
            <div className="ai-copy-field">
              <span className="ai-copy-field-label">标题</span>
              <p className="ai-copy-field-value">{result.title}</p>
            </div>
            <div className="ai-copy-field">
              <span className="ai-copy-field-label">描述</span>
              <p className="ai-copy-field-value">{result.description}</p>
            </div>
          </div>
          <div className="ai-copy-result-actions">
            <Button
              className="ai-copy-apply-btn"
              onClick={() => onApply(result)}
              type="button"
            >
              <Check size={14} />
              采纳并填充
            </Button>
            <Button
              variant="outline"
              className="ai-copy-regenerate-btn"
              onClick={() => onGenerate(selectedStyle)}
              disabled={isLoading}
              type="button"
            >
              <Sparkles size={14} />
              重新生成
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}