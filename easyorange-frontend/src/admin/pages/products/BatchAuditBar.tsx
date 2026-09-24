import { Check, X } from 'lucide-react';
import { createPortal } from 'react-dom';
import { Button } from '@/components/ui/button';

interface BatchAuditBarProps {
    count: number;
    submitting: boolean;
    onApprove: () => void;
    /** 打开驳回原因弹窗，不直接提交 */
    onReject: () => void;
    onClear: () => void;
}

/**
 * 批量审核操作条。Portal 到 body：`.admin-sidebar` / `.admin-header` 的 backdrop-filter
 * 会创建新包含块，留在文档流里手搓 fixed 会定位错乱（见前端 AGENTS.md）。
 * 底部 6rem：给 Toast（bottom 2.5rem，同样居中）留出位置，避免互相盖住。
 */
export function BatchAuditBar({ count, submitting, onApprove, onReject, onClear }: BatchAuditBarProps) {
    if (count === 0) {
        return null;
    }

    return createPortal(
        <div className="admin-batch-bar" role="toolbar" aria-label="批量审核操作">
            <span className="admin-batch-bar-count">
                已选 <strong>{count}</strong> 件待审核商品
            </span>
            <div className="admin-batch-bar-actions">
                <Button
                    type="button"
                    onClick={onApprove}
                    disabled={submitting}
                    isLoading={submitting}
                    loadingText="处理中"
                    className="admin-action-btn admin-action-btn--approve"
                >
                    {!submitting ? <Check size={15} aria-hidden="true" /> : null}
                    批量通过
                </Button>
                <Button
                    type="button"
                    onClick={onReject}
                    disabled={submitting}
                    className="admin-action-btn admin-action-btn--reject"
                >
                    <X size={15} aria-hidden="true" />
                    批量驳回
                </Button>
                <Button
                    type="button"
                    variant="ghost"
                    onClick={onClear}
                    disabled={submitting}
                    className="admin-action-btn--ghost"
                >
                    取消选择
                </Button>
            </div>
        </div>,
        document.body
    );
}
