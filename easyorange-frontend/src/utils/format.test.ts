import { describe, expect, it } from 'vitest';
import { buildQueryString, escapeHtml, formatDate, formatPrice, formatRelativeTime } from './format';

describe('formatPrice', () => {
    it('formats integer to two decimal places', () => {
        expect(formatPrice(100)).toBe('100.00');
    });

    it('formats decimal to two decimal places', () => {
        expect(formatPrice(99.5)).toBe('99.50');
    });

    it('formats zero', () => {
        expect(formatPrice(0)).toBe('0.00');
    });
});

describe('formatDate', () => {
    it('returns "--" for null/undefined', () => {
        expect(formatDate(null as unknown as string)).toBe('--');
        expect(formatDate(undefined as unknown as string)).toBe('--');
    });

    it('returns "--" for invalid date', () => {
        expect(formatDate('not-a-date')).toBe('--');
    });

    it('formats Date object as datetime by default', () => {
        const date = new Date('2026-05-16T10:30:00');
        const result = formatDate(date);
        expect(result).toContain('2026');
        expect(result).toContain('05');
        expect(result).toContain('16');
    });

    it('formats with date format', () => {
        const date = new Date('2026-05-16T10:30:00');
        const result = formatDate(date, 'date');
        expect(result).toContain('2026');
        // time part should NOT be in the output for 'date' format
        expect(result).not.toContain('10:');
    });
});

describe('formatRelativeTime', () => {
    it('returns "--" for null', () => {
        expect(formatRelativeTime(null as unknown as string)).toBe('--');
    });

    it('returns "刚刚" for less than 1 minute', () => {
        const now = new Date();
        expect(formatRelativeTime(now)).toBe('刚刚');
    });

    it('returns minutes ago', () => {
        const fiveMinAgo = new Date(Date.now() - 5 * 60 * 1000);
        expect(formatRelativeTime(fiveMinAgo)).toBe('5分钟前');
    });

    it('returns hours ago', () => {
        const threeHoursAgo = new Date(Date.now() - 3 * 60 * 60 * 1000);
        expect(formatRelativeTime(threeHoursAgo)).toBe('3小时前');
    });

    it('returns days ago', () => {
        const twoDaysAgo = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000);
        expect(formatRelativeTime(twoDaysAgo)).toBe('2天前');
    });
});

describe('escapeHtml', () => {
    it('escapes HTML special characters', () => {
        expect(escapeHtml('<script>alert("xss")</script>')).toBe(
            '&lt;script&gt;alert(&quot;xss&quot;)&lt;&#x2F;script&gt;'
        );
    });

    it('returns empty string for null/undefined', () => {
        expect(escapeHtml(null)).toBe('');
        expect(escapeHtml(undefined)).toBe('');
    });

    it('passes through plain text unchanged', () => {
        expect(escapeHtml('hello world')).toBe('hello world');
    });
});

describe('buildQueryString', () => {
    it('builds query string from params', () => {
        const result = buildQueryString({ keyword: 'test', page: 1 });
        expect(result).toContain('keyword=test');
        expect(result).toContain('page=1');
    });

    it('filters out null, undefined, and empty values', () => {
        const result = buildQueryString({ a: 'val', b: null, c: undefined, d: '' });
        expect(result).toContain('a=val');
        expect(result).not.toContain('b=');
        expect(result).not.toContain('c=');
        expect(result).not.toContain('d=');
    });

    it('returns empty string for empty params', () => {
        expect(buildQueryString({})).toBe('');
    });
});
