/**
 * @fileoverview 错误处理工具模块
 * @description 提供统一的错误处理和友好提示功能
 */

import { toast } from './toast.js';
import { network } from './network.js';

export type ErrorTypeValue = 'network' | 'api' | 'validation' | 'auth' | 'permission' | 'not_found' | 'server' | 'timeout' | 'unknown';
export type ErrorSeverityValue = 'low' | 'medium' | 'high' | 'fatal';

const errorMessages: Record<ErrorTypeValue, Record<string, string>> = {
    network: { default: '网络连接失败，请检查网络设置', timeout: '请求超时，请稍后重试' },
    api: { default: '服务暂时不可用，请稍后重试', 401: '登录已过期，请重新登录', 403: '没有权限执行此操作', 404: '请求的资源不存在' },
    validation: { default: '输入信息有误，请检查后重试' },
    auth: { default: '认证失败，请重新登录' },
    permission: { default: '没有权限执行此操作' },
    not_found: { default: '请求的资源不存在' },
    server: { default: '服务器错误，请稍后重试' },
    timeout: { default: '操作超时，请重试' },
    unknown: { default: '发生未知错误，请稍后重试' }
};

const errorHandler = {
    handle(error: unknown, type: ErrorTypeValue = 'unknown'): string {
        let message = '发生错误';
        if (typeof error === 'string') {
            message = error;
        } else if (error instanceof Error) {
            message = error.message;
        } else if (error && typeof error === 'object' && 'message' in error) {
            message = (error as { message: string }).message;
        }

        return errorMessages[type]?.default || message;
    },

    handleApiError(error: unknown, status?: number): string {
        let type: ErrorTypeValue = 'api';
        if (status === 401) {type = 'auth';}
        else if (status === 403) {type = 'permission';}
        else if (status === 404) {type = 'not_found';}
        else if (status && status >= 500) {type = 'server';}
        else if (status === 0 && !network.isOnline()) {type = 'network';}

        const message = this.handle(error, type);
        toast.error(message);
        return message;
    }
};

export { errorHandler };
export default errorHandler;
