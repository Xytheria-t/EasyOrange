/**
 * @fileoverview 应用常量配置
 * @description 统一管理所有常量
 */

/** 存储键名常量 */
export const STORAGE_KEYS = {
    TOKEN: 'token',
    USER: 'user',
    FAVORITES: 'favorites',
    CART: 'cart',
    DRAFT: 'draft',
    THEME: 'theme',
    LANGUAGE: 'language'
} as const;

/** 商品相关常量 */
export const PRODUCT_CONSTANTS = {
    CONDITION: {
        NEW: 'NEW',
        LIKE_NEW: 'LIKE_NEW',
        GOOD: 'GOOD',
        FAIR: 'FAIR',
        POOR: 'POOR'
    },
    STATUS: {
        ON_SALE: 1,
        SOLD_OUT: 2,
        OFF_SHELF: 3
    },
    CATEGORIES: {
        BOOKS: 1,
        ELECTRONICS: 2,
        CLOTHING: 3,
        DAILY_NECESSITIES: 4
    }
} as const;
