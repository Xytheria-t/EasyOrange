import { describe, it, expect, beforeEach } from 'vitest';
import { getCacheKey, getFromCache, setToCache, clearCache } from './cache';

describe('getCacheKey', () => {
  it('generates key from endpoint', () => {
    expect(getCacheKey('/api/products')).toBe('/api/products::');
  });

  it('includes body in key', () => {
    const key = getCacheKey('/api/products', { body: { name: 'test' } });
    expect(key).toContain('/api/products');
    expect(key).toContain('test');
  });

  it('includes params in key', () => {
    const key = getCacheKey('/api/products', { params: { page: 1 } });
    expect(key).toContain('page');
  });
});

describe('cache lifecycle', () => {
  beforeEach(() => {
    clearCache();
  });

  it('stores and retrieves cached data', () => {
    setToCache('test-key', { data: 'hello' });
    const cached = getFromCache<{ data: string }>('test-key');
    expect(cached).toEqual({ data: 'hello' });
  });

  it('returns null for missing key', () => {
    expect(getFromCache('non-existent')).toBeNull();
  });

  it('clears all cache when no pattern given', () => {
    setToCache('a', 1);
    setToCache('b', 2);
    clearCache();
    expect(getFromCache('a')).toBeNull();
    expect(getFromCache('b')).toBeNull();
  });

  it('clears cache by pattern', () => {
    setToCache('/api/products/list', 1);
    setToCache('/api/products/detail', 2);
    setToCache('/api/users', 3);
    clearCache('/api/products');
    expect(getFromCache('/api/products/list')).toBeNull();
    expect(getFromCache('/api/products/detail')).toBeNull();
    expect(getFromCache('/api/users')).toBe(3);
  });
});
