/**
 * @fileoverview 错误处理工具模块
 * @description 提供统一的错误处理和友好提示功能
 */

export type ErrorTypeValue =
    | 'network'
    | 'api'
    | 'validation'
    | 'auth'
    | 'permission'
    | 'not_found'
    | 'server'
    | 'timeout'
    | 'unknown';
export type ErrorSeverityValue = 'low' | 'medium' | 'high' | 'fatal';

const httpErrorMessages: Record<number, string> = {
    0: '网络连接失败，请检查网络设置',
    400: '请求参数有误，请检查后重试',
    401: '登录已过期，请重新登录',
    403: '没有权限执行此操作',
    404: '请求的资源不存在',
    405: '请求方法不支持',
    408: '请求超时，请稍后重试',
    409: '请求冲突，请稍后重试',
    422: '请求参数校验失败，请检查输入',
    429: '操作过于频繁，请稍后再试',
    500: '服务器内部错误，请稍后重试',
    502: '服务暂时不可用，请稍后重试',
    503: '服务维护中，请稍后重试',
    504: '网关超时，请稍后重试',
};

const fallbackTypeMessages: Record<ErrorTypeValue, string> = {
    network: '网络连接失败，请检查网络设置',
    api: '服务暂时不可用，请稍后重试',
    validation: '输入信息有误，请检查后重试',
    auth: '认证失败，请重新登录',
    permission: '没有权限执行此操作',
    not_found: '请求的资源不存在',
    server: '服务器错误，请稍后重试',
    timeout: '操作超时，请重试',
    unknown: '发生未知错误，请稍后重试',
};

const errorHandler = {
    handle(error: unknown, type: ErrorTypeValue = 'unknown'): string {
        if (typeof error === 'string') {
            return error;
        }

        if (error instanceof Error) {
            const errorObj = error as unknown as Record<string, unknown>;
            const status = errorObj.status as number | string | undefined;

            if (error.message && !error.message.includes('HTTP error')) {
                return error.message;
            }

            if (typeof status === 'number' && status in httpErrorMessages) {
                return httpErrorMessages[status];
            }

            if (typeof status === 'number' && status >= 500) {
                return httpErrorMessages[status] || fallbackTypeMessages.server;
            }

            if (typeof status === 'number' && status === 0) {
                return fallbackTypeMessages.network;
            }

            return fallbackTypeMessages[type];
        }

        if (error && typeof error === 'object' && 'message' in error) {
            const msg = (error as { message: string }).message;
            if (msg && !msg.includes('HTTP error')) {
                return msg;
            }
        }

        return fallbackTypeMessages[type];
    },

    handleApiError(error: unknown, status?: number): string {
        if (!status) {
            status =
                typeof error === 'object' && error !== null && 'status' in error
                    ? (error as { status?: number }).status
                    : undefined;
        }

        let type: ErrorTypeValue = 'api';
        if (status === 401) {
            type = 'auth';
        } else if (status === 403) {
            type = 'permission';
        } else if (status === 404) {
            type = 'not_found';
        } else if (status && status >= 500) {
            type = 'server';
        } else if (status === 0) {
            type = 'network';
        }

        let message = this.handle(error, type);

        if (!message || message.includes('HTTP error')) {
            message =
                status !== undefined
                    ? (httpErrorMessages[status] ?? fallbackTypeMessages[type])
                    : fallbackTypeMessages[type];
        }

        return message;
    },
};

export { errorHandler };
export default errorHandler;
