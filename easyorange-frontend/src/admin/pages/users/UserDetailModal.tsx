import { useState } from 'react';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui';
import { Button } from '@/components/ui/button';
import { pickAvatarGradient } from '../../components/avatarGradient';
import type { AdminUser } from '../../types/admin';

export interface UserDetailModalProps {
    open: boolean;
    user: AdminUser | null;
    onClose: () => void;
    onSave: (status: string) => Promise<void>;
    loading?: boolean;
}

const statusOptions: { value: string; label: string; emoji: string }[] = [
    { value: '0', label: '正常', emoji: '✅' },
    { value: '1', label: '禁用', emoji: '🚫' },
    { value: '2', label: '锁定', emoji: '🔒' },
];

const STATUS_STYLES: Record<string, { bg: string; color: string; dot: string; label: string }> = {
    '0': {
        bg: 'linear-gradient(135deg, rgba(16,185,129,0.10), rgba(52,211,153,0.06))',
        color: '#059669',
        dot: '#10B981',
        label: '正常',
    },
    '1': {
        bg: 'linear-gradient(135deg, rgba(244,63,94,0.10), rgba(251,113,133,0.06))',
        color: '#E11D48',
        dot: '#F43F5E',
        label: '禁用',
    },
    '2': {
        bg: 'linear-gradient(135deg, rgba(245,158,11,0.10), rgba(251,191,36,0.06))',
        color: '#D97706',
        dot: '#F59E0B',
        label: '锁定',
    },
};

export function UserDetailModal({ open, user, onClose, onSave, loading = false }: UserDetailModalProps) {
    const [selectedStatus, setSelectedStatus] = useState<string>(user?.status ?? '0');

    if (!open || !user) {
        return null;
    }

    const handleSave = async () => {
        await onSave(selectedStatus);
    };

    const formatDate = (dateString: string) =>
        new Date(dateString).toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
        });

    const maskPhone = (phone: string | null) => {
        if (!phone) {
            return '未绑定';
        }
        return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
    };

    const maskEmail = (email: string) => {
        const [name, domain] = email.split('@');
        const maskedName = name.length > 2 ? `${name[0]}***${name[name.length - 1]}` : name;
        return `${maskedName}@${domain}`;
    };

    const avatarGradient = pickAvatarGradient(user.userId ?? '');
    const currentStatusStyle = STATUS_STYLES[selectedStatus] ?? STATUS_STYLES['0'];

    return (
        <Dialog
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen) {
                    onClose();
                }
            }}
        >
            <DialogContent
                className="[&>button]:hidden max-w-[440px] w-[calc(100%-2rem)] p-0 gap-0 overflow-hidden rounded-3xl border border-white/70 bg-white/92 shadow-[0_24px_64px_rgba(42,37,32,0.18),0_8px_24px_rgba(249,115,22,0.06)]"
                style={{ backdropFilter: 'blur(24px)', WebkitBackdropFilter: 'blur(24px)' }}
            >
                {/* Header */}
                <DialogHeader className="relative flex-row items-center justify-between border-b border-[rgba(229,224,219,0.5)] px-6 py-5 text-left">
                    <div
                        className="absolute bottom-0 left-6 right-6 h-px"
                        style={{
                            background:
                                'linear-gradient(90deg, rgba(249,115,22,0.12), rgba(195,155,211,0.08), transparent)',
                        }}
                    />
                    <DialogTitle
                        className="flex items-center gap-2 text-[1.1rem] font-bold text-[#2A2520]"
                        style={{ fontFamily: "'Playfair Display', 'Noto Serif SC', serif" }}
                    >
                        <span className="inline-flex h-[26px] w-[26px] shrink-0 items-center justify-center rounded-lg bg-[linear-gradient(135deg,#F97316,#FB923C)] text-white">
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
                        </span>
                        用户详情
                    </DialogTitle>
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={onClose}
                        disabled={loading}
                        className="inline-flex h-8 w-8 items-center justify-center rounded-[10px] border-[1.5px] border-[#E5E0DB] bg-white text-[#8B857E] transition-all duration-150 hover:border-[rgba(244,63,94,0.2)] hover:bg-[rgba(244,63,94,0.06)] hover:text-[#E11D48] disabled:cursor-not-allowed disabled:opacity-50"
                        aria-label="关闭"
                    >
                        <svg
                            aria-hidden="true"
                            width="14"
                            height="14"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.5"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        >
                            <path d="M18 6L6 18M6 6l12 12" />
                        </svg>
                    </Button>
                </DialogHeader>

                {/* Body */}
                <div className="max-h-[calc(100vh-2rem)] flex-1 overflow-y-auto p-6">
                    {/* User profile card */}
                    <div className="mb-5 flex items-center gap-4 rounded-2xl border border-[rgba(249,115,22,0.06)] bg-[linear-gradient(135deg,rgba(249,115,22,0.04),rgba(195,155,211,0.03))] p-4">
                        <div
                            className="flex h-[52px] w-[52px] shrink-0 items-center justify-center rounded-2xl text-[1.25rem] font-bold text-white"
                            style={{
                                fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                                background: avatarGradient,
                                boxShadow: '0 4px 12px rgba(249,115,22,0.18)',
                            }}
                        >
                            {(user.nickname || user.username || '?').charAt(0).toUpperCase()}
                        </div>
                        <div className="min-w-0 flex-1">
                            <p className="text-[1rem] font-bold leading-tight text-[#2A2520]">
                                {user.nickname || user.username}
                            </p>
                            <p className="mt-0.5 text-[0.82rem] text-[#9B9590]">@{user.username}</p>
                        </div>
                        <div
                            className="inline-flex items-center gap-[0.35rem] rounded-[10px] px-[0.7rem] py-[0.3rem] text-[0.78rem] font-semibold"
                            style={{ background: currentStatusStyle.bg, color: currentStatusStyle.color }}
                        >
                            <span
                                className="h-1.5 w-1.5 shrink-0 rounded-full"
                                style={{ background: currentStatusStyle.dot }}
                            />
                            {currentStatusStyle.label}
                        </div>
                    </div>

                    {/* Info grid */}
                    <div className="mb-5 grid grid-cols-2 gap-[0.85rem]">
                        {[
                            { label: '用户名', value: user.username },
                            { label: '昵称', value: user.nickname || '未设置' },
                            { label: '邮箱', value: maskEmail(user.email ?? '') },
                            { label: '手机', value: maskPhone(user.phone) },
                            { label: '用户类型', value: user.userType === '01' ? '🎓 学生' : '👨‍🏫 教师' },
                            { label: '注册时间', value: formatDate(user.createTime ?? '') },
                        ].map(item => (
                            <div
                                key={item.label}
                                className="rounded-xl border border-[rgba(229,224,219,0.4)] bg-white/60 px-[0.85rem] py-[0.65rem]"
                            >
                                <p className="mb-0.5 text-[0.72rem] font-medium text-[#9B9590]">{item.label}</p>
                                <p className="text-[0.87rem] font-semibold text-[#2A2520]">{item.value}</p>
                            </div>
                        ))}
                    </div>

                    {/* Stats row */}
                    <div className="mb-5 flex items-center gap-0 border-y border-[rgba(229,224,219,0.4)] py-[0.85rem]">
                        <div className="flex-1 text-center">
                            <p
                                className="text-[1.5rem] font-bold leading-tight text-[#F97316]"
                                style={{ fontFamily: "'DM Sans', sans-serif" }}
                            >
                                —
                            </p>
                            <p className="mt-0.5 text-[0.78rem] text-[#9B9590]">商品数</p>
                        </div>
                        <div className="h-9 w-px bg-[rgba(229,224,219,0.5)]" />
                        <div className="flex-1 text-center">
                            <p
                                className="text-[1.5rem] font-bold leading-tight text-[#C39BD3]"
                                style={{ fontFamily: "'DM Sans', sans-serif" }}
                            >
                                —
                            </p>
                            <p className="mt-0.5 text-[0.78rem] text-[#9B9590]">订单数</p>
                        </div>
                    </div>

                    {/* Status selector */}
                    <div>
                        <label
                            htmlFor="status-select"
                            className="mb-2 block text-[0.82rem] font-semibold text-[#6B6460]"
                        >
                            调整状态
                        </label>
                        <div className="flex gap-2" id="status-select" role="radiogroup">
                            {statusOptions.map(opt => {
                                const isActive = selectedStatus === opt.value;
                                const sStyle = STATUS_STYLES[opt.value];
                                return (
                                    <Button
                                        key={opt.value}
                                        type="button"
                                        variant="outline"
                                        onClick={() => setSelectedStatus(opt.value)}
                                        disabled={loading}
                                        className="flex flex-1 items-center justify-center gap-[0.35rem] rounded-xl border-[1.5px] px-2 py-[0.6rem] text-[0.84rem] font-semibold transition-all duration-200 disabled:cursor-not-allowed disabled:opacity-60"
                                        style={{
                                            borderColor: isActive ? sStyle.dot : '#E5E0DB',
                                            background: isActive ? sStyle.bg : '#fff',
                                            color: isActive ? sStyle.color : '#8B857E',
                                        }}
                                    >
                                        <span className="text-[0.9rem]">{opt.emoji}</span>
                                        {opt.label}
                                    </Button>
                                );
                            })}
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <DialogFooter className="flex-row justify-end gap-[0.65rem] border-t border-[rgba(229,224,219,0.4)] bg-[linear-gradient(180deg,rgba(250,248,245,0.5),rgba(250,248,245,0.9))] px-6 py-4">
                    <Button
                        variant="outline"
                        onClick={onClose}
                        disabled={loading}
                        className="h-10 rounded-xl border-[1.5px] border-[#E5E0DB] bg-white px-5 text-[0.87rem] font-semibold text-[#6B6460] hover:bg-[rgba(229,224,219,0.3)] hover:text-[#6B6460]"
                    >
                        取消
                    </Button>
                    <Button
                        onClick={handleSave}
                        disabled={loading || selectedStatus === user.status}
                        isLoading={loading}
                        loadingText="保存中..."
                        className="h-10 rounded-xl border-none bg-[linear-gradient(135deg,#F97316,#EA580C)] px-6 text-[0.87rem] font-semibold text-white shadow-[0_3px_12px_rgba(249,115,22,0.3)] transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_5px_18px_rgba(249,115,22,0.4)] disabled:translate-y-0 disabled:bg-[#D6CEC5] disabled:shadow-none"
                    >
                        保存修改
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
