import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { StorageUtils, storage } from './storage';

describe('storage', () => {
    beforeEach(() => {
        localStorage.clear();
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-07-28T10:00:00.000Z'));
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('get', () => {
        it('returns default value when key does not exist', () => {
            expect(storage.get('missing')).toBeNull();
            expect(storage.get('missing', 'fallback')).toBe('fallback');
        });

        it('returns parsed value when key exists', () => {
            localStorage.setItem('key', JSON.stringify({ value: 'hello' }));
            expect(storage.get('key')).toBe('hello');
        });

        it('removes and returns default when item is expired', () => {
            localStorage.setItem('key', JSON.stringify({ value: 'expired', expireAt: Date.now() - 1 }));
            expect(storage.get('key', 'default')).toBe('default');
            expect(localStorage.getItem('key')).toBeNull();
        });

        it('returns default when stored value is not valid JSON', () => {
            localStorage.setItem('key', 'not-json');
            expect(storage.get('key', 'default')).toBe('default');
        });
    });

    describe('has', () => {
        it('returns true when key exists', () => {
            localStorage.setItem('key', JSON.stringify({ value: 1 }));
            expect(storage.has('key')).toBe(true);
        });

        it('returns false when key does not exist', () => {
            expect(storage.has('missing')).toBe(false);
        });
    });

    describe('set', () => {
        it('stores value without ttl', () => {
            storage.set('key', { name: 'test' });
            const stored = JSON.parse(localStorage.getItem('key') ?? '{}');
            expect(stored.value).toEqual({ name: 'test' });
            expect(stored.expireAt).toBeUndefined();
        });

        it('stores value with ttl', () => {
            storage.set('key', 'value', 60_000);
            const stored = JSON.parse(localStorage.getItem('key') ?? '{}');
            expect(stored.value).toBe('value');
            expect(stored.expireAt).toBe(Date.now() + 60_000);
        });
    });

    describe('remove', () => {
        it('removes the stored key', () => {
            localStorage.setItem('key', JSON.stringify({ value: 1 }));
            storage.remove('key');
            expect(localStorage.getItem('key')).toBeNull();
        });
    });

    describe('clear', () => {
        it('clears all stored items', () => {
            localStorage.setItem('a', '1');
            localStorage.setItem('b', '2');
            storage.clear();
            expect(localStorage.getItem('a')).toBeNull();
            expect(localStorage.getItem('b')).toBeNull();
        });
    });

    describe('StorageUtils', () => {
        it('exposes the same methods as storage', () => {
            StorageUtils.set('utils', 'works');
            expect(StorageUtils.get('utils')).toBe('works');
            expect(StorageUtils.has('utils')).toBe(true);
            StorageUtils.remove('utils');
            expect(StorageUtils.has('utils')).toBe(false);
        });
    });
});
