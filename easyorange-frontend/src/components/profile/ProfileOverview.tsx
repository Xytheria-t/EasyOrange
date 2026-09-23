/**
 * ProfileOverview 组件
 *
 * 注意：以下数据为占位数据，待后续实现真实数据获取：
 * - 本月交易额（¥12,580）及趋势（+23.5%）
 * - 商品浏览量（1,234）及趋势（+15.2%）
 * - 平均评分（4.9）
 * - AI交易助手建议（最佳发布时间、价格建议、热门品类）
 * - 会员等级（黄金会员）
 *
 * 已实现真实数据：
 * - 消息数、收藏数
 */

import {
    Award,
    BadgeCheck,
    Calendar,
    Check,
    ChevronRight,
    Mail,
    MessageSquare,
    Pencil,
    Phone,
    Shield,
    Sparkles,
    User,
    X,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Input } from '@/components/ui';
import { Button } from '@/components/ui/button';
import type { User as UserType } from '@/types';

type EditableField = 'nickname' | 'email' | 'phone' | 'realName';

interface ProfileOverviewProps {
    user: UserType | undefined;
    unreadMessageCount: number;
    editingField: EditableField | null;
    editValue: string;
    isSaving: boolean;
    onEdit: (field: EditableField, value: string) => void;
    onSave: () => void;
    onCancel: () => void;
    onEditValueChange: (value: string) => void;
}

export function ProfileOverview({
    user,
    unreadMessageCount,
    editingField,
    editValue,
    isSaving,
    onEdit,
    onSave,
    onCancel,
    onEditValueChange,
}: ProfileOverviewProps) {
    const navigate = useNavigate();

    const editableFields: { key: EditableField; label: string; value?: string | null; icon: typeof User }[] = [
        { key: 'nickname', label: '昵称', value: user?.nickname, icon: Sparkles },
        { key: 'realName', label: '真实姓名', value: user?.realName, icon: User },
        { key: 'email', label: '邮箱', value: user?.email, icon: Mail },
        { key: 'phone', label: '手机', value: user?.phone, icon: Phone },
    ];

    const readonlyFields = [
        { key: 'username', label: '用户名', value: user?.username, icon: BadgeCheck },
        { key: 'createTime', label: '注册时间', value: user?.createTime, icon: Calendar },
    ];

    // 「我的发布」「我的订单」入口只保留侧边栏一处：概览页再放同名卡片会有多个入口指向同一页面，
    // 且此前「我的发布」卡片指向的 /products?seller=me 是公共商品列表（seller 参数无人解析）
    const quickActions = [
        { label: '消息中心', icon: MessageSquare, count: unreadMessageCount, path: '/messages', color: 'green' },
    ];

    return (
        <div className="content-section active">
            <div className="section-header">
                <div className="header-title">
                    <h2>数据概览</h2>
                    <p className="header-subtitle">实时追踪你的智能托管数据</p>
                </div>
            </div>

            <div className="quick-actions">
                <h3 className="section-subtitle">快捷入口</h3>
                <div className="action-cards">
                    {quickActions.map(action => {
                        const Icon = action.icon;
                        return (
                            <Button
                                key={action.label}
                                variant="ghost"
                                className="action-card"
                                onClick={() => navigate(action.path)}
                            >
                                <div className={`action-icon ${action.color}`}>
                                    <Icon size={22} />
                                </div>
                                <div className="action-content">
                                    <span className="action-title">{action.label}</span>
                                    <span className="action-count">{action.count} 条记录</span>
                                </div>
                                <ChevronRight size={20} className="action-arrow" />
                            </Button>
                        );
                    })}
                </div>
            </div>

            <div className="info-cards">
                <div className="info-card-large">
                    <div className="card-header">
                        <h3>个人信息</h3>
                    </div>
                    <div className="info-list">
                        {readonlyFields.map(({ key, label, value, icon: Icon }) => (
                            <div className="info-row" key={key}>
                                <div className="info-label">
                                    <Icon size={18} />
                                    {label}
                                </div>
                                <span className="info-value">{value || '未设置'}</span>
                            </div>
                        ))}
                        {editableFields.map(({ key, label, value, icon: Icon }) => (
                            <div className="info-row" key={key}>
                                <div className="info-label">
                                    <Icon size={18} />
                                    {label}
                                </div>
                                {editingField === key ? (
                                    <div
                                        style={{
                                            display: 'flex',
                                            gap: '0.5rem',
                                            alignItems: 'center',
                                            flex: 1,
                                            justifyContent: 'flex-end',
                                        }}
                                    >
                                        <Input
                                            style={{ padding: '0.375rem 0.75rem', fontSize: '0.875rem', maxWidth: 200 }}
                                            value={editValue}
                                            onChange={e => onEditValueChange(e.target.value)}
                                            autoFocus
                                            onKeyDown={e => {
                                                if (e.key === 'Enter') {
                                                    onSave();
                                                }
                                                if (e.key === 'Escape') {
                                                    onCancel();
                                                }
                                            }}
                                        />
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            className="profile-action-btn profile-action-save"
                                            onClick={onSave}
                                            disabled={isSaving}
                                        >
                                            <Check size={14} />
                                        </Button>
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            className="profile-action-btn profile-action-cancel"
                                            onClick={onCancel}
                                        >
                                            <X size={14} />
                                        </Button>
                                    </div>
                                ) : (
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <span
                                            className="info-value"
                                            style={{ color: value ? 'var(--profile-ink)' : 'var(--profile-ink-soft)' }}
                                        >
                                            {value || '未设置'}
                                        </span>
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            className="profile-edit-btn"
                                            onClick={() => onEdit(key, value || '')}
                                            style={{ opacity: 0.6 }}
                                        >
                                            <Pencil size={12} />
                                        </Button>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                </div>

                <div className="info-card-large">
                    <div className="card-header">
                        <h3>账号状态</h3>
                    </div>
                    <div className="info-list">
                        <div className="info-row">
                            <div className="info-label">
                                <Shield size={18} />
                                账号状态
                            </div>
                            <span className="status-badge active">正常</span>
                        </div>
                        <div className="info-row">
                            <div className="info-label">
                                <Award size={18} />
                                会员等级
                            </div>
                            <span className="info-value">黄金会员</span>
                        </div>
                        <div className="info-row">
                            <div className="info-label">
                                <span style={{ fontSize: 18 }}>🕐</span>
                                最后登录
                            </div>
                            <span className="info-value">刚刚</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
