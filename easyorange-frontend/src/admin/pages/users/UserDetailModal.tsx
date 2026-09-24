import { useState } from 'react';
import { AdminDetailModal } from '@/admin/components/AdminDetailModal';
import { Button } from '@/components/ui/button';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { pickAvatarGradient } from '../../components/avatarGradient';
import { ConfirmModal } from '../../components/ConfirmModal';
import { StatusBadge } from '../../components/StatusBadge';
import type { AdminUser } from '../../types/admin';

export interface UserDetailModalProps {
    open: boolean;
    user: AdminUser | null;
    onClose: () => void;
    onSave: (status: string) => Promise<void>;
    loading?: boolean;
}

const statusOptions = [
    { value: 'NORMAL', label: '正常', dot: 'var(--status-success-dot)' },
    { value: 'DISABLED', label: '禁用', dot: 'var(--status-error-dot)' },
    { value: 'LOCKED', label: '锁定', dot: 'var(--status-warning-dot)' },
];

/** 改成非正常态会影响该用户的登录能力，保存前要明确确认。 */
const DESTRUCTIVE_STATUS: Record<string, string> = {
    DISABLED: '禁用后该用户将无法登录、发布或下单',
    LOCKED: '锁定后该用户将无法登录，直到管理员解除锁定',
};

function formatDate(dateString: string | null | undefined) {
    if (!dateString) {
        return '—';
    }
    const date = new Date(dateString);
    if (Number.isNaN(date.getTime())) {
        return '—';
    }
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    });
}

function maskPhone(phone: string | null) {
    if (!phone) {
        return '未绑定';
    }
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
}

function maskEmail(email: string | null) {
    if (!email) {
        return '未绑定';
    }
    const [name, domain] = email.split('@');
    if (!domain) {
        return email;
    }
    const maskedName = name.length > 2 ? `${name[0]}***${name[name.length - 1]}` : name;
    return `${maskedName}@${domain}`;
}

export function UserDetailModal({ open, user, onClose, onSave, loading = false }: UserDetailModalProps) {
    // 草稿初值取当前用户状态；调用方用 key={userId} 重建本组件，切人即重置，
    // 不需要 effect 把 props 镜像进 state（那样会先渲染一次上一位用户的旧选择）
    const [selectedStatus, setSelectedStatus] = useState<string>(user?.status ?? 'NORMAL');
    const [pendingConfirm, setPendingConfirm] = useState(false);

    if (!open || !user) {
        return null;
    }

    const isDirty = selectedStatus !== user.status;
    const needsConfirm = Boolean(DESTRUCTIVE_STATUS[selectedStatus]) && isDirty;

    const commitSave = async () => {
        await onSave(selectedStatus);
        setPendingConfirm(false);
    };

    const handleSaveClick = () => {
        if (needsConfirm) {
            setPendingConfirm(true);
            return;
        }
        void commitSave();
    };

    const avatarGradient = pickAvatarGradient(user.userId ?? '');

    return (
        <>
            <AdminDetailModal
                open={open}
                onClose={onClose}
                title="用户详情"
                maxWidth={440}
                loading={loading}
                closeDisabled={loading}
                icon={
                    <svg
                        aria-hidden="true"
                        width="13"
                        height="13"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2.5"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                    </svg>
                }
                footer={
                    <div className="admin-footer-actions justify-end border-t border-[rgba(229,224,219,0.4)] px-6 py-4">
                        <Button variant="outline" onClick={onClose} disabled={loading}>
                            取消
                        </Button>
                        <Button
                            onClick={handleSaveClick}
                            disabled={loading || !isDirty}
                            isLoading={loading}
                            loadingText="保存中"
                        >
                            保存修改
                        </Button>
                    </div>
                }
            >
                <div>
                    {/* 头像 + 当前状态 */}
                    <div
                        className="admin-inset"
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '1rem',
                            padding: '1rem',
                            marginBottom: '1.25rem',
                        }}
                    >
                        <div
                            aria-hidden="true"
                            style={{
                                height: 52,
                                width: 52,
                                flexShrink: 0,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                borderRadius: 16,
                                fontSize: '1.25rem',
                                fontWeight: 700,
                                color: '#fff',
                                fontFamily: 'var(--admin-font-title)',
                                background: avatarGradient,
                            }}
                        >
                            {(user.nickname || user.username || '?').charAt(0).toUpperCase()}
                        </div>
                        <div style={{ minWidth: 0, flex: 1 }}>
                            <p style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--admin-ink)' }}>
                                {user.nickname || user.username}
                            </p>
                            <p className="admin-label" style={{ marginTop: '0.2rem' }}>
                                @{user.username}
                            </p>
                        </div>
                        <StatusBadge status={user.status ?? ''} type="user" />
                    </div>

                    {/* 信息格：窄屏单列 */}
                    <div className="admin-field-grid" style={{ marginBottom: '1.25rem' }}>
                        {[
                            { label: '用户名', value: user.username },
                            { label: '昵称', value: user.nickname || '未设置' },
                            { label: '邮箱', value: maskEmail(user.email) },
                            { label: '手机', value: maskPhone(user.phone) },
                            {
                                label: '用户类型',
                                value: user.userTypeDesc || (user.userType === '01' ? '学生' : '教师'),
                            },
                            { label: '注册时间', value: formatDate(user.createTime) },
                        ].map(item => (
                            <div key={item.label} className="admin-inset">
                                <div style={{ padding: '0.65rem 0.85rem' }}>
                                    <p className="admin-label" style={{ marginBottom: '0.2rem' }}>
                                        {item.label}
                                    </p>
                                    <p className="admin-value">{item.value}</p>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* 状态选择：与分类编辑共用 Radix RadioGroup，键盘与读屏语义一致 */}
                    <fieldset>
                        <legend className="admin-label" style={{ marginBottom: '0.5rem' }}>
                            调整状态
                        </legend>
                        <RadioGroup
                            value={selectedStatus}
                            onValueChange={setSelectedStatus}
                            disabled={loading}
                            className="admin-field-grid"
                        >
                            {statusOptions.map(opt => {
                                const isActive = selectedStatus === opt.value;
                                return (
                                    <label
                                        key={opt.value}
                                        htmlFor={`user-status-${opt.value}`}
                                        style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            gap: '0.4rem',
                                            padding: '0.6rem',
                                            borderRadius: 'var(--admin-radius-control)',
                                            border: `1.5px solid ${isActive ? opt.dot : 'var(--admin-control-border)'}`,
                                            background: isActive ? 'var(--admin-accent-soft)' : '#fff',
                                            color: isActive ? 'var(--admin-accent)' : 'var(--admin-muted)',
                                            fontSize: '0.84rem',
                                            fontWeight: 600,
                                            cursor: 'pointer',
                                        }}
                                    >
                                        <RadioGroupItem
                                            value={opt.value}
                                            id={`user-status-${opt.value}`}
                                            className="sr-only"
                                        />
                                        <span
                                            aria-hidden="true"
                                            style={{
                                                width: 7,
                                                height: 7,
                                                borderRadius: 'var(--admin-radius-pill)',
                                                background: opt.dot,
                                            }}
                                        />
                                        {opt.label}
                                    </label>
                                );
                            })}
                        </RadioGroup>
                    </fieldset>
                </div>
            </AdminDetailModal>

            <ConfirmModal
                isOpen={pendingConfirm}
                title={`确认${statusOptions.find(o => o.value === selectedStatus)?.label ?? '变更'}用户`}
                content={`将「${user.username}」调整为「${statusOptions.find(o => o.value === selectedStatus)?.label}」。${DESTRUCTIVE_STATUS[selectedStatus]}。`}
                confirmText="确认变更"
                variant="warning"
                isLoading={loading}
                onConfirm={commitSave}
                onCancel={() => setPendingConfirm(false)}
            />
        </>
    );
}
