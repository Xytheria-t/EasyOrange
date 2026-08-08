import { ApiClientError } from '@/api/core/request';

/**
 * 订单操作错误码 → 用户可读文案。错误码来自后端 {@code OrderResultCode}，
 * 用结构化 status 匹配而非错误消息字符串嗅探（后端改文案不失效）。
 */
export function orderErrorMessage(err: unknown, fallback = '操作失败，请重试'): string {
    if (err instanceof ApiClientError) {
        switch (err.status) {
            case 'B3007':
                return '该订单当前无法取消，可能已支付或已发货';
            case 'B3001':
                return '订单信息已变更，请刷新页面重试';
            case 'B3003':
                return '您没有权限操作此订单';
            default:
                return err.message;
        }
    }
    return fallback;
}
