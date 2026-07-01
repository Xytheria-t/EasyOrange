import { AlertTriangle, Award, Shield } from 'lucide-react';
import type { CreditScoreResult } from '@/api/creditApi';
import './ai-components.css';

interface CreditScoreCardProps {
    credit: CreditScoreResult;
}

const LEVEL_CONFIG: Record<string, { label: string; color: string; icon: typeof Shield }> = {
    EXCELLENT: { label: '优秀', color: '#22c55e', icon: Award },
    GOOD: { label: '良好', color: '#3b82f6', icon: Shield },
    NORMAL: { label: '正常', color: '#f59e0b', icon: Shield },
    LOW: { label: '较低', color: '#f97316', icon: AlertTriangle },
    BLACKLIST: { label: '黑名单', color: '#ef4444', icon: AlertTriangle },
};

export function CreditScoreCard({ credit }: CreditScoreCardProps) {
    const config = LEVEL_CONFIG[credit.level] || LEVEL_CONFIG.NORMAL;
    const LevelIcon = config.icon;
    const scorePercent = (credit.creditScore / 200) * 100;

    return (
        <div className="credit-score-card">
            <div className="credit-header">
                <Shield size={24} className="credit-icon" />
                <span className="credit-title">信用评分</span>
            </div>

            <div className="credit-score-display">
                <div
                    className="score-ring"
                    style={{ background: `conic-gradient(${config.color} ${scorePercent}%, #e5e7eb ${scorePercent}%)` }}
                >
                    <div className="score-inner">
                        <span className="score-value" style={{ color: config.color }}>
                            {credit.creditScore}
                        </span>
                        <span className="score-max">/200</span>
                    </div>
                </div>
                <div className="credit-level" style={{ color: config.color }}>
                    <LevelIcon size={20} />
                    <span>{config.label}</span>
                </div>
            </div>

            <div className="credit-stats">
                <div className="stat-item">
                    <span className="stat-label">交易完成率</span>
                    <span className="stat-value">{credit.tradeCompletionRate}%</span>
                </div>
                <div className="stat-item">
                    <span className="stat-label">AI 成交</span>
                    <span className="stat-value">{credit.completedTrades}</span>
                </div>
                <div className="stat-item">
                    <span className="stat-label">平均评分</span>
                    <span className="stat-value">{credit.reviewAvgRating.toFixed(1)}</span>
                </div>
            </div>
        </div>
    );
}
