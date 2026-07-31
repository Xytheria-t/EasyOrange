/**
 * @fileoverview 后端 API 返回的原始数据类型定义
 * @description 这些类型反映了后端实际返回的数据结构，需要通过 normalize 函数转换为前端类型
 */

import type { ProductStatus } from './product';

/**
 * 后端返回的原始商品数据
 * @description status 是后端下发的状态码（如 'ONLINE'），condition 是数字
 */
export interface RawProduct {
    id: string;
    title?: string;
    description?: string;
    price: number;
    originalPrice?: number | null;
    categoryId: number;
    categoryName?: string;
    condition?: number;
    status?: ProductStatus;
    images?: string[];
    location?: string;
    views?: number;
    favorites?: number;
    sellerId: string;
    sellerName?: string;
    username?: string; // 后端可能返回 username 而非 sellerName
    sellerAvatar?: string | null;
    userAvatar?: string | null; // 后端可能返回 userAvatar 而非 sellerAvatar
    sellerRating?: number;
    createTime?: string;
    updateTime?: string;
    stock?: number;
    contactMethod?: string;
}

/**
 * 后端返回的原始聊天消息数据
 * @description type 和 status 可能缺失，需要设置默认值
 */
export interface RawChatMessage {
    id: string;
    senderId: string;
    receiverId: string;
    content: string;
    type?: string;
    status?: string;
    createTime: string;
    readTime?: string | null;
    recalledAt?: string | null;
}
