import type { ProductStatus } from '@/types/product';

export const CONDITION_LABEL_MAP: Record<number, string> = {
    1: '全新',
    2: '几乎全新',
    3: '轻微使用',
    4: '明显使用',
    5: '有缺陷',
};

export const STATUS_LABEL_MAP: Record<ProductStatus, string> = {
    DRAFT: '草稿',
    ONLINE: '在售',
    SOLD: '已售出',
    OFFLINE: '已下架',
    PENDING_REVIEW: '⏳ 审核中',
    REJECTED: '🔴 已驳回',
};
