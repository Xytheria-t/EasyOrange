const SUCCESS_CODES = ['A0000', '0', 0, 200] as const;

export type ApiCode = string | number;

type SuccessCode = typeof SUCCESS_CODES[number];

export const isSuccessCode = (code: ApiCode | null | undefined): code is SuccessCode => {
    return SUCCESS_CODES.includes(code as SuccessCode);
};

export interface Result<T = unknown> {
    code: ApiCode;
    message: string;
    data: T;
    timestamp: number;
}

export interface PageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
}
