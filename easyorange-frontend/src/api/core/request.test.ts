import { describe, expect, it } from 'vitest';
import { ApiClientError, buildQueryParams } from './request';

describe('buildQueryParams', () => {
    it('builds query string from params', () => {
        const result = buildQueryParams({ keyword: 'test', page: 1 });
        expect(result).toBe('?keyword=test&page=1');
    });

    it('filters out null, undefined, and empty values', () => {
        const result = buildQueryParams({ a: 'val', b: null, c: undefined, d: '' });
        expect(result).toBe('?a=val');
    });

    it('returns empty string for empty params', () => {
        expect(buildQueryParams({})).toBe('');
    });
});

describe('ApiClientError', () => {
    it('creates error with message', () => {
        const error = new ApiClientError('Not Found', 404);
        expect(error.message).toBe('Not Found');
        expect(error.status).toBe(404);
    });

    it('captures details', () => {
        const details = { field: 'name' };
        const error = new ApiClientError('Validation failed', 400, details);
        expect(error.details).toEqual(details);
    });

    it('has correct name', () => {
        const error = new ApiClientError('error', 0);
        expect(error.name).toBe('ApiClientError');
    });
});
