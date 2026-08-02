import { describe, expect, it } from 'vitest';
import type { RawProduct } from '@/types';
import { calculateDiscount, getConditionNameFromString, normalizeProduct } from './product';

describe('normalizeProduct', () => {
    it('normalizes a raw product record', () => {
        const raw: RawProduct = {
            id: '1',
            title: '测试',
            description: '描述',
            price: 100,
            categoryId: '1',
            status: 'ONLINE',
            condition: 1,
            images: [],
            sellerId: 'u1',
            createTime: '2026-01-01',
            updateTime: '2026-01-01',
        };

        const product = normalizeProduct(raw);
        expect(product.id).toBe('1');
        expect(product.title).toBe('测试');
        expect(product.price).toBe(100);
        expect(product.status).toBe('ONLINE');
    });

    it('uses defaults for missing fields', () => {
        const product = normalizeProduct({
            id: '1',
            price: 50,
            categoryId: '1',
            sellerId: 'u1',
        } as import('@/types').RawProduct);
        expect(product.title).toBe('');
        expect(product.description).toBe('');
        expect(product.status).toBe('ONLINE');
        expect(product.sellerName).toBe('匿名用户');
    });

    it('defaults unknown status codes to ONLINE', () => {
        const product = normalizeProduct({
            id: '1',
            price: 50,
            categoryId: '1',
            sellerId: 'u1',
            status: 'OFFLINE',
        } as import('@/types').RawProduct);
        expect(product.status).toBe('OFFLINE');

        const unknown = normalizeProduct({
            id: '2',
            price: 50,
            categoryId: '1',
            sellerId: 'u1',
            status: 'ARCHIVED',
        } as unknown as RawProduct);
        expect(unknown.status).toBe('ONLINE');
    });
});

describe('calculateDiscount', () => {
    it('calculates discount ratio', () => {
        expect(calculateDiscount(80, 100)).toBe(0.8);
    });

    it('returns null when originalPrice is missing', () => {
        expect(calculateDiscount(80, null)).toBeNull();
    });

    it('returns null when current >= original', () => {
        expect(calculateDiscount(100, 100)).toBeNull();
        expect(calculateDiscount(120, 100)).toBeNull();
    });
});

describe('getConditionNameFromString', () => {
    it('maps known condition strings', () => {
        expect(getConditionNameFromString('NEW')).toBe('全新');
        expect(getConditionNameFromString('LIKE_NEW')).toBe('几乎全新');
        expect(getConditionNameFromString('GOOD')).toBe('良好');
    });

    it('returns original string for unknown condition', () => {
        expect(getConditionNameFromString('UNKNOWN')).toBe('UNKNOWN');
    });
});
