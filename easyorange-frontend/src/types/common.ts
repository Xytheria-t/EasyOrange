/** 后端统一响应码 — A0000、0 和 200 均表示成功 */
export const SUCCESS_CODES = ['A0000', 0, 200] as const;
export type ApiCode = string | number;
export type SuccessCode = typeof SUCCESS_CODES[number];

export const isSuccessCode = (code: ApiCode | null | undefined): code is SuccessCode => {
    return SUCCESS_CODES.includes(code as SuccessCode);
};

/** 可重试的 HTTP 状态码 */
export const RETRYABLE_STATUS = [0, 408, 429] as const;

/** 统一 API 响应类型 — 后端使用 Result<T> 格式 */
export interface Result<T = unknown> {
    code: ApiCode;
    message: string;
    data: T;
}

/** 分页查询参数 */
export interface PageParams {
    current?: number;
    size?: number;
}

/** 分页响应数据 */
export interface PageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
}

/** 分页 API 响应便捷类型 */
export type PageResponse<T> = Result<PageResult<T>>;

/**
 * @deprecated 使用 Result<T> 替代。保留此类型仅为向后兼容。
 */
export interface ApiResponse<T = unknown> {
    success: boolean;
    code: ApiCode;
    message: string;
    data: T;
    timestamp?: number;
}
