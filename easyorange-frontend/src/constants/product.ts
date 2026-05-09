import type { ProductStatus } from '@/types/product';

export const CONDITION_LABEL_MAP: Record<number, string> = {
    1: '全新',
    2: '几乎全新',
    3: '轻微使用',
    4: '明显使用',
};

export const STATUS_LABEL_MAP: Record<ProductStatus, string> = {
    DRAFT: '草稿',
    ONLINE: '在售',
    SOLD: '已售出',
    OFFLINE: '已下架',
};

export const PRODUCT_STATUS_CODE: Record<number, ProductStatus> = {
    0: 'DRAFT',
    1: 'ONLINE',
    2: 'SOLD',
    3: 'OFFLINE',
};
